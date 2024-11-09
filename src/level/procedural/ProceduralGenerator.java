package level.procedural;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.RandomType;
import level.objects.BlockLike;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public boolean validate(LayoutMarker marker, GeneratorType type) {
        AtomicBoolean validated = new AtomicBoolean(true);
        MainPanel.level.layout.forEachMarker(marker.pos.y, 1, lm -> {
            if (!validation.validate(this, marker, type, lm))
                validated.set(false);
        });
        return validated.get();
    }

    public void generateMarkers(LayoutMarker marker, GeneratorType type) {
        for (int i = 0; i < 16; i++) {
            markerFunction.generateMarkers(this, marker, type);
            generatedLayoutMarkers.forEach(LayoutMarker::generate);
            boolean validated = true;
            for (LayoutMarker lm : generatedLayoutMarkers) {
                if (lm.gen.validate(lm, lm.genType))
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
