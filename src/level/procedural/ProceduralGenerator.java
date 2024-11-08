package level.procedural;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.RandomType;
import level.objects.BlockLike;
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
    //All layout markers added by the generation function must be added to this set
    private final HashSet<LayoutMarker> generatedLayoutMarkers = new HashSet<>();

    //Data that the generator stores for use in validation
    private final HashMap<String, Object> generationData = new HashMap<>();

    private final GeneratorFunction function;

    public ProceduralGenerator(GeneratorFunction function) {
        this.function = function;
    }

    public void generate(LayoutMarker marker, GeneratorType type) {
        function.generate(this, marker, type);
    }

    public void generateMarkers() {
        generatedLayoutMarkers.forEach(LayoutMarker::generate);
    }

    public void revertGeneration() {
        generatedLayoutMarkers.forEach(m -> MainPanel.level.layout.removeMarker(m));
        MainPanel.level.removeBlocks(generatedBlocks.toArray(new BlockLike[0]));
        overwrittenBlocks.forEach(b -> MainPanel.level.addBlocks(AssetManager.createBlock(b.name, b.pos)));

        overwrittenBlocks.clear();
        generatedBlocks.clear();
        generatedLayoutMarkers.clear();
        generationData.clear();
    }

    public void addBlock(String blockName, ObjPos pos) {
        BlockLike block = AssetManager.createBlock(blockName, pos);
        BlockLike removed = MainPanel.level.addProceduralBlock(block);
        if (removed != null) {
            if (!generatedBlocks.remove(removed))
                overwrittenBlocks.add(removed);
        }
        generatedBlocks.add(block);
    }

    public void addMarker(String name, ObjPos pos) {
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
