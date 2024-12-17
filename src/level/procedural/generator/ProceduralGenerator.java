package level.procedural.generator;

import foundation.Deletable;
import foundation.Main;
import foundation.math.MathUtil;
import foundation.math.ObjPos;
import foundation.math.RandomType;
import level.Level;
import level.objects.BlockLike;
import level.procedural.jump.JumpSimGroup;
import level.procedural.jump.JumpSimulation;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.movement.LMDPlayerMovement;
import level.procedural.marker.resolved.LMDResolvedElement;
import level.procedural.marker.resolved.LMTResolvedElement;
import loader.AssetManager;
import render.event.RenderEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProceduralGenerator implements Deletable {
    //All blocks added by the generation function must be added to this set
    private final HashSet<BlockLike> generatedBlocks = new HashSet<>();
    //All blocks overwritten by the generation function are added to this set
    private final HashSet<BlockLike> overwrittenBlocks = new HashSet<>();
    //All layout markers added by the marker generation function must be added to this set
    //The markerFunction is the only function allowed to add LayoutMarkers
    private final ArrayList<LayoutMarker> generatedLayoutMarkers = new ArrayList<>();
    private final ArrayList<LayoutMarker> playerMovementMarkers = new ArrayList<>();

    //Data that the generator stores for use in validation
    private final HashMap<String, Object> generationData = new HashMap<>();

    //The function should generate everything necessary for bounds validation, the blockGenerator runs afterward
    private final GeneratorFunction function, blockGenerator;
    private final GeneratorLMFunction markerFunction, validatorMarkerFunction;
    private final GeneratorValidation validation;
    public Level level;

    public ProceduralGenerator(Level level, GeneratorFunction boundsGenerator, GeneratorValidation validation, GeneratorFunction blockGenerator, GeneratorLMFunction validatorMarkerFunction, GeneratorLMFunction markerFunction) {
        this.function = boundsGenerator;
        this.validation = validation;
        this.blockGenerator = blockGenerator;
        this.validatorMarkerFunction = validatorMarkerFunction;
        this.markerFunction = markerFunction;
        this.level = level;
    }

    public void generate(LayoutMarker marker, GeneratorType type) {
        function.generate(this, marker, type);
    }

    public static void generateBlocks(LayoutMarker marker) {
        if (marker.data instanceof LMDResolvedElement data)
            data.gen.blockGenerator.generate(data.gen, marker, data.genType);
    }

    public static void generateValidationMarkers(LayoutMarker marker) {
        if (marker.data instanceof LMDResolvedElement data) {
            data.gen.validatorMarkerFunction.generateMarkers(data.gen, marker, data.genType);
        }
    }

    public static int generationAttempts = 0, generatedMarkers = 0;

    public void generateMarkers(LayoutMarker marker, GeneratorType type) {
        generatedMarkers++;
        //Validation markers will have been generated on this marker's behalf by the previous loop iteration,
        //and we don't want to revert their generation
        generatedLayoutMarkers.clear();

        for (int i = 0; i < 500; i++) {
            if (level.interruptGeneration.get()) {
                level.interruptGeneration.set(false);
                return;
            }
            generationAttempts++;
            markerFunction.generateMarkers(this, marker, type); //Generate new markers
            generatedLayoutMarkers.forEach(LayoutMarker::generate); //Generate bounds for those markers
            AtomicBoolean validated = new AtomicBoolean(true);
            //Test bounds for new markers
            for (LayoutMarker lm : generatedLayoutMarkers) {
                if (lm.data instanceof LMDResolvedElement data && !GeneratorValidation.validate(lm, data.gen.validation, lm.level)) {
                    validated.set(false);
                    break;
                }
            }
            HashSet<JumpSimulation> revalidatedJumps = new HashSet<>();
            HashSet<JumpSimulation> addedJumps = new HashSet<>();
            if (validated.get()) {
                //Generate blocks and validation markers in preparation for player movement sim
                generatedLayoutMarkers.forEach(ProceduralGenerator::generateBlocks);
                generatedLayoutMarkers.forEach(ProceduralGenerator::generateValidationMarkers);
                for (LayoutMarker lm : generatedLayoutMarkers) {
                    if (lm.data instanceof LMDResolvedElement data) {
                        data.forEachJumpSimGroup(group -> {
                            level.layout.forEachJumpSimGroup(lm.pos.y, 1, otherGroup -> {
                                if (!otherGroup.equals(group)) {
                                    JumpSimulation jumpSimulationFrom = new JumpSimulation(otherGroup, group, otherGroup.movementMarkers, group.movementMarkers);
                                    jumpSimulationFrom.validateJump(level);
                                    jumpSimulationFrom.addFromGroup();
                                    addedJumps.add(jumpSimulationFrom);

                                    JumpSimulation jumpSimulationTo = new JumpSimulation(group, otherGroup, group.movementMarkers, otherGroup.movementMarkers);
                                    jumpSimulationTo.validateJump(level);
                                    jumpSimulationTo.addFromGroup();
                                    addedJumps.add(jumpSimulationTo);
                                }
                            });
                        });
                    }
                }

                for (LayoutMarker lm : generatedLayoutMarkers) {
                    if (!(lm.type instanceof LMTResolvedElement))
                        continue;
                    level.layout.forEachJumpSimGroup(lm.pos.y, 2, otherGroup -> {
                        if (!((LMDResolvedElement) lm.data).jumps.contains(otherGroup)) {
                            for (JumpSimulation jump : otherGroup.jumps.keySet()) {
                                if (jump.bound != null && lm.isBoxColliding(jump.bound, BoundType.COLLISION)) {
                                    jump.validateJump(level);
                                    revalidatedJumps.add(jump);
                                }
                            }
                        }
                    });
                }
                for (LayoutMarker lm : generatedLayoutMarkers) {
                    if (!(lm.type instanceof LMTResolvedElement))
                        continue;
                    HashSet<JumpSimGroup> jumpSimGroups = level.layout.nonReachable(lm);
                    if (!jumpSimGroups.isEmpty())
                        validated.set(false);
                }
            }
            if (validated.get()) {
                level.updateBlocks(RenderEvent.ON_GAME_INIT, marker);
                level.collisionHandler.qRemove.addAll(overwrittenBlocks);
                level.collisionHandler.qAdd.addAll(generatedBlocks);
                generatedLayoutMarkers.forEach(level.layout::addProceduralLM); //Repeat the cycle for the newly validated markers
                break;
            } else {
                addedJumps.forEach(j -> j.from.jumps.remove(j));
                generatedLayoutMarkers.forEach(lm -> {
                    if (lm.data instanceof LMDResolvedElement data) {
                        data.gen.revertGeneration();
                        data.delete();
                    }
                    lm.delete();
                });
                generatedLayoutMarkers.clear();
                revalidatedJumps.forEach(j -> {
                    if (j.validatedJumpHadCollision)
                        j.validateJump(level);
                });
            }
        }
    }

    public void revertGeneration() {
        level.removeBlocks(false, generatedBlocks.toArray(new BlockLike[0]));
        overwrittenBlocks.forEach(b -> level.addBlocks(false, true, AssetManager.createBlock(b.name, b.pos, level)));

        generatedLayoutMarkers.forEach(LayoutMarker::delete);
        overwrittenBlocks.clear();
        generatedBlocks.clear();
        generationData.forEach((k, v) -> {
            if (v instanceof Deletable d)
                d.delete();
        });
        generationData.clear();
    }

    public void addBlock(String blockName, ObjPos pos) {
        if (level.outOfBounds(pos))
            return;
        BlockLike block = AssetManager.createBlock(blockName, pos, level);
        BlockLike removed = level.addProceduralBlock(false, true, block);
        if (removed != null) {
            if (!generatedBlocks.remove(removed))
                overwrittenBlocks.add(removed);
        }
        generatedBlocks.add(block);
    }

    public void addMarker(String name, ObjPos pos) {
        if (level.outOfBounds(pos))
            return;
        LayoutMarker marker = new LayoutMarker(name, pos, level);
        generatedLayoutMarkers.add(marker);
        level.layout.addMarker(marker);
    }

    public JumpSimGroup newJumpSimGroup(LayoutMarker lm) {
        JumpSimGroup group = new JumpSimGroup(lm.pos);
        ((LMDResolvedElement) lm.data).jumps.add(group);
        lm.level.layout.addJumpGroup(group);
        return group;
    }

    public LMDPlayerMovement addJumpMarker(String name, JumpSimGroup group, ObjPos pos) {
        if (level.outOfBounds(pos))
            return null;
        LayoutMarker marker = new LayoutMarker(name, pos, level);
        group.movementMarkers.add(marker);
        return (LMDPlayerMovement) marker.data;
    }

    public LMDPlayerMovement addJumpMarker(String name, JumpSimGroup group, ObjPos pos, Consumer<LMDPlayerMovement> action) {
        if (level.outOfBounds(pos))
            return null;
        LayoutMarker marker = new LayoutMarker(name, pos, level);
        group.movementMarkers.add(marker);
        LMDPlayerMovement data = (LMDPlayerMovement) marker.data;
        action.accept(data);
        return data;
    }

    public void addData(String name, Object data) {
        generationData.put(name, data);
    }

    public ObjPos randomPosAbove(LayoutMarker lm, float minAngle, float maxAngle, float minLength, float maxLength, float xLengthMultiplier, int borderProximityLimit) {
        return randomPosAbove(lm.pos, minAngle, maxAngle, minLength, maxLength, xLengthMultiplier, borderProximityLimit);
    }

    public ObjPos randomPosAbove(ObjPos origin, float minAngle, float maxAngle, float minLength, float maxLength, float xLengthMultiplier, int borderProximityLimit) {
        ObjPos pos;
        do {
            float angle = randomFloat(minAngle, maxAngle);
            float length = randomFloat(minLength, maxLength);
            boolean isRight = randomBoolean(0.5f);
            if (origin.x > Main.BLOCKS_X - 1 - borderProximityLimit)
                isRight = false;
            else if (origin.x < borderProximityLimit)
                isRight = true;
            pos = new ObjPos(origin.x + Math.cos(angle) * length * xLengthMultiplier * (isRight ? 1 : -1), origin.y + Math.sin(angle) * length).toInt();
        } while (level.outOfBounds(pos));
        return pos;
    }

    public ObjPos randomPosAbove(Supplier<ObjPos> originSupplier, float minAngle, float maxAngle, float minLength, float maxLength, float xLengthMultiplier, int borderProximityLimit) {
        ObjPos pos;
        do {
            ObjPos origin = originSupplier.get();
            float angle = randomFloat(minAngle, maxAngle);
            float length = randomFloat(minLength, maxLength);
            boolean isRight = randomBoolean(0.5f);
            if (origin.x > Main.BLOCKS_X - 1 - borderProximityLimit)
                isRight = false;
            else if (origin.x < borderProximityLimit)
                isRight = true;
            pos = new ObjPos(origin.x + Math.cos(angle) * length * xLengthMultiplier * (isRight ? 1 : -1), origin.y + Math.sin(angle) * length).toInt();
        } while (level.outOfBounds(pos));
        return pos;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String name, Class<T> clazz) {
        Object data = generationData.get(name);
        if (!clazz.isInstance(data))
            return null;
        return (T) data;
    }

    public void lineOfBlocks(int fromX, int toX, int y, Function<ObjPos, String> blockSupplier) {
        for (int x = fromX; x <= toX; x++) {
            ObjPos pos = new ObjPos(x, y);
            addBlock(blockSupplier.apply(pos), pos);
        }
    }

    public void lineOfBlocks(float fromX, float toX, float y, Function<ObjPos, String> blockSupplier) {
        lineOfBlocks((int) fromX, (int) toX, (int) y, blockSupplier);
    }

    public int randomInt(int min, int max) {
        return MathUtil.randIntBetween(min, max, random());
    }

    public float randomFloat(float min, float max) {
        return MathUtil.randFloatBetween(min, max, random());
    }

    public boolean randomBoolean(float probability) {
        return MathUtil.randBoolean(probability, random());
    }

    public Supplier<Double> random() {
        return level.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL);
    }

    public Supplier<Boolean> probability(float probability) {
        return () -> MathUtil.randBoolean(probability, random());
    }

    @Override
    public void delete() {
        revertGeneration();
    }
}
