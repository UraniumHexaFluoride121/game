package level.procedural;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.MathHelper;
import foundation.ObjPos;
import level.RandomType;
import level.objects.BlockLike;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;

import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ProceduralGenerator implements Deletable {
    //All blocks added by the generation function must be added to this set
    private HashSet<BlockLike> generatedBlocks = new HashSet<>();
    //All blocks overwritten by the generation function are added to this set
    private HashSet<BlockLike> overwrittenBlocks = new HashSet<>();
    //All layout markers added by the generation function must be added to this set
    private HashSet<LayoutMarker> generatedLayoutMarkers = new HashSet<>();

    private final BiConsumer<ProceduralGenerator, LayoutMarker> function;

    public ProceduralGenerator(BiConsumer<ProceduralGenerator, LayoutMarker> function) {
        this.function = function;
    }

    public void generate(LayoutMarker marker) {
        function.accept(this, marker);
    }

    public void revertGeneration() {
        generatedLayoutMarkers.forEach(m -> MainPanel.level.layout.removeMarker(m));
        MainPanel.level.removeBlocks(generatedBlocks.toArray(new BlockLike[]{}));
        overwrittenBlocks.forEach(b -> MainPanel.level.addBlocks(AssetManager.createBlock(b.name, b.pos)));

        overwrittenBlocks.clear();
        generatedBlocks.clear();
        generatedLayoutMarkers.clear();
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

    public void lineOfBlocks(int fromX, int toX, int y, Function<ObjPos, String> blockSupplier) {
        for (int x = fromX; x <= toX; x++) {
            ObjPos pos = new ObjPos(x, y);
            addBlock(blockSupplier.apply(pos), pos);
        }
    }

    public int randomInt(int min, int max) {
        return MathHelper.randIntBetween(min, max, MainPanel.level.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL));
    }

    public boolean randomBoolean(float probability) {
        return MathHelper.randBoolean(probability, MainPanel.level.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL));
    }

    @Override
    public void delete() {
        generatedBlocks.forEach(BlockLike::unregister);
    }
}
