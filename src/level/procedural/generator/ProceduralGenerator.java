package level.procedural.generator;

import foundation.Deletable;
import foundation.Main;
import foundation.MainPanel;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.RandomType;
import level.objects.BlockLike;
import level.procedural.JumpSimulation;
import level.procedural.Layout;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.movement.LMTPlayerMovement;
import level.procedural.marker.resolved.LMDResolvedElement;
import loader.AssetManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProceduralGenerator implements Deletable {
    //All blocks added by the generation function must be added to this set
    private final HashSet<BlockLike> generatedBlocks = new HashSet<>();
    //All blocks overwritten by the generation function are added to this set
    private final HashSet<BlockLike> overwrittenBlocks = new HashSet<>();
    //All layout markers added by the marker generation function must be added to this set
    //The markerFunction is the only function allowed to add LayoutMarkers
    private final HashSet<LayoutMarker> generatedLayoutMarkers = new HashSet<>();
    private final HashSet<LayoutMarker> playerMovementMarkers = new HashSet<>();

    //Data that the generator stores for use in validation
    private final HashMap<String, Object> generationData = new HashMap<>();

    //The function should generate everything necessary for bounds validation, the blockGenerator runs afterward
    private final GeneratorFunction function, blockGenerator;
    private final GeneratorLMFunction markerFunction, validatorMarkerFunction;
    private final GeneratorValidation validation;


    public ProceduralGenerator(GeneratorFunction boundsGenerator, GeneratorValidation validation, GeneratorFunction blockGenerator, GeneratorLMFunction validatorMarkerFunction, GeneratorLMFunction markerFunction) {
        this.function = boundsGenerator;
        this.validation = validation;
        this.blockGenerator = blockGenerator;
        this.validatorMarkerFunction = validatorMarkerFunction;
        this.markerFunction = markerFunction;
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
            data.gen.generatedLayoutMarkers.forEach(lm -> {
                if (lm.type instanceof LMTPlayerMovement)
                    data.gen.playerMovementMarkers.add(lm);
            });
        }
    }

    public void generateMarkers(LayoutMarker marker, GeneratorType type) {
        //Validation markers will have been generated on this marker's behalf by the previous loop iteration,
        //and we don't want to revert their generation
        generatedLayoutMarkers.clear();

        for (int i = 0; i < 150; i++) {
            markerFunction.generateMarkers(this, marker, type); //Generate new markers
            generatedLayoutMarkers.forEach(LayoutMarker::generate); //Generate bounds for those markers
            boolean validated = true;
            //Test bounds for new markers
            for (LayoutMarker lm : generatedLayoutMarkers) {
                if (lm.data instanceof LMDResolvedElement data && !GeneratorValidation.validate(lm, data.gen.validation)) {
                    validated = false;
                    break;
                }
            }
            if (validated) {
                //Generate blocks and validation markers in preparation for player movement sim
                generatedLayoutMarkers.forEach(ProceduralGenerator::generateBlocks);
                generatedLayoutMarkers.forEach(ProceduralGenerator::generateValidationMarkers);
                for (LayoutMarker lm : generatedLayoutMarkers) {
                    if (lm.data instanceof LMDResolvedElement data) {
                        if (!new JumpSimulation(marker, lm, playerMovementMarkers, data.gen.playerMovementMarkers).validateJump())
                            validated = false;
                    }
                }
            }
            if (validated) {
                generatedLayoutMarkers.forEach(LayoutMarker::generateMarkers); //Repeat the cycle for the newly validated markers
                break;
            } else {
                generatedLayoutMarkers.forEach(lm -> {
                    if (lm.data instanceof LMDResolvedElement data)
                        data.gen.revertGeneration();
                    lm.delete();
                });
                generatedLayoutMarkers.clear();
            }
        }
    }

    public void revertGeneration() {
        MainPanel.level.removeBlocks(generatedBlocks.toArray(new BlockLike[0]));
        overwrittenBlocks.forEach(b -> MainPanel.level.addBlocks(AssetManager.createBlock(b.name, b.pos)));

        generatedLayoutMarkers.forEach(LayoutMarker::delete);
        overwrittenBlocks.clear();
        generatedBlocks.clear();
        if (Layout.DEBUG_RENDER) //Delete debug elements that were added to the renderer
            generationData.forEach((k, v) -> {
                if (v instanceof Deletable d)
                    d.delete();
            });
        generationData.clear();
    }

    public void addBlock(String blockName, ObjPos pos) {
        if (MainPanel.level.outOfBounds(pos))
            return;
        BlockLike block = AssetManager.createBlock(blockName, pos);
        BlockLike removed = MainPanel.level.addProceduralBlock(block);
        if (removed != null) {
            if (!generatedBlocks.remove(removed))
                overwrittenBlocks.add(removed);
        }
        generatedBlocks.add(block);
    }

    public void addMarker(String name, ObjPos pos) {
        if (MainPanel.level.outOfBounds(pos))
            return;
        LayoutMarker marker = new LayoutMarker(name, pos);
        generatedLayoutMarkers.add(marker);
        MainPanel.level.layout.addMarker(marker);
    }

    public void addData(String name, Object data) {
        generationData.put(name, data);
    }

    public ObjPos randomPosAbove(LayoutMarker lm, float minAngle, float maxAngle, float minLength, float maxLength, float xLengthMultiplier, int borderProximityLimit) {
        ObjPos pos;
        do {
            float angle = randomFloat(minAngle, maxAngle);
            float length = randomFloat(minLength, maxLength);
            boolean isRight = randomBoolean(0.5f);
            if (lm.pos.x > Main.BLOCKS_X - 1 - borderProximityLimit)
                isRight = false;
            else if (lm.pos.x < borderProximityLimit)
                isRight = true;
            pos = new ObjPos(lm.pos.x + Math.cos(angle) * length * xLengthMultiplier * (isRight ? 1 : -1), lm.pos.y + Math.sin(angle) * length).toInt();
        } while (MainPanel.level.outOfBounds(pos));
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
        } while (MainPanel.level.outOfBounds(pos));
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
        return MathHelper.randIntBetween(min, max, MainPanel.level.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL));
    }

    public float randomFloat(float min, float max) {
        return MathHelper.randFloatBetween(min, max, MainPanel.level.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL));
    }

    public boolean randomBoolean(float probability) {
        return MathHelper.randBoolean(probability, MainPanel.level.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL));
    }

    @Override
    public void delete() {
        revertGeneration();
    }
}
