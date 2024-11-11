package level.procedural.generator;

import foundation.Deletable;
import foundation.Main;
import foundation.MainPanel;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.RandomType;
import level.objects.BlockLike;
import level.procedural.Layout;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

public class ProceduralGenerator implements Deletable {
    //All blocks added by the generation function must be added to this set
    private final HashSet<BlockLike> generatedBlocks = new HashSet<>();
    //All blocks overwritten by the generation function are added to this set
    private final HashSet<BlockLike> overwrittenBlocks = new HashSet<>();
    //All layout markers added by the marker generation function must be added to this set
    //The markerFunction is the only function allowed to add LayoutMarkers, not the generation function
    private final HashSet<LayoutMarker> generatedLayoutMarkers = new HashSet<>();

    //Data that the generator stores for use in validation
    private final HashMap<String, Object> generationData = new HashMap<>();

    private final GeneratorFunction function;
    private final GeneratorLMFunction markerFunction;
    private final GeneratorValidation validation;


    public ProceduralGenerator(GeneratorFunction function, GeneratorLMFunction markerFunction, GeneratorValidation validation) {
        this.function = function;
        this.markerFunction = markerFunction;
        this.validation = validation;
    }

    public void generate(LayoutMarker marker, GeneratorType type) {
        function.generate(this, marker, type);
    }

    public void generateMarkers(LayoutMarker marker, GeneratorType type) {
        for (int i = 0; i < 50; i++) {
            markerFunction.generateMarkers(this, marker, type);
            generatedLayoutMarkers.forEach(LayoutMarker::generate);
            boolean validated = true;
            for (LayoutMarker lm : generatedLayoutMarkers) {
                if (GeneratorValidation.validate(lm, lm.genType, lm.gen.validation))
                    continue;
                validated = false;
                break;
            }
            if (validated) {
                generatedLayoutMarkers.forEach(LayoutMarker::generateMarkers);
                break;
            } else {
                generatedLayoutMarkers.forEach(lm -> {
                    lm.gen.revertGeneration();
                    lm.delete();
                });
                generatedLayoutMarkers.clear();
            }
        }
    }

    public void revertGeneration() {
        MainPanel.level.removeBlocks(generatedBlocks.toArray(new BlockLike[0]));
        overwrittenBlocks.forEach(b -> MainPanel.level.addBlocks(AssetManager.createBlock(b.name, b.pos)));

        overwrittenBlocks.clear();
        generatedBlocks.clear();
        if (Layout.DEBUG_LAYOUT_RENDER) //Delete debug elements that were added to the renderer
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

    public ObjPos randomPosAbove(LayoutMarker lm, float minAngle, float maxAngle, float minLength, float maxLength, float xLengthMultiplier, int borderProximityLimit)  {
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
        generatedBlocks.forEach(BlockLike::unregister);
        generationData.clear();
    }
}
