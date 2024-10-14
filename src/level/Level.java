package level;

import foundation.Deletable;
import foundation.Main;
import foundation.input.InputHandler;
import level.objects.BlockLike;
import physics.CollisionHandler;
import render.event.RenderEvent;

import java.util.HashMap;
import java.util.HashSet;

public class Level implements Deletable {
    public final int maximumHeight;
    public final InputHandler inputHandler;
    public final CollisionHandler collisionHandler;

    //All BlockLikes inserted as static MUST NOT have their positions modified, otherwise
    //they risk not being able to be accessed.
    //Multiple static blocks can be placed at the same position as long as they're on different
    //layers. This does NOT affect the rendering layer or render order, it is just to allow
    //multiple static blocks to be placed on top of each other. It can, however, affect how
    //the renderer does connected textures
    public HashMap<ObjectLayer, BlockLike[][]> staticBlocks = new HashMap<>();

    //A set containing all dynamic blocks. Connected textures do not work for these blocks.
    public HashSet<BlockLike> dynamicBlocks = new HashSet<>();

    public Level(int maximumHeight) {
        for (ObjectLayer layer : ObjectLayer.values()) {
            if (!layer.addToStatic)
                continue;
            staticBlocks.put(layer, new BlockLike[Main.BLOCKS_X][]);
            BlockLike[][] blockLayer = staticBlocks.get(layer);
            for (int i = 0; i < Main.BLOCKS_X; i++) {
                blockLayer[i] = new BlockLike[maximumHeight];
            }
        }

        inputHandler = new InputHandler();
        collisionHandler = new CollisionHandler(maximumHeight, 16, 2);
        this.maximumHeight = maximumHeight;
    }

    public BlockLike getBlock(ObjectLayer layer, int x, int y) {
        if (x < 0 || x >= 30 || y < 0 || y >= maximumHeight || !layer.addToStatic)
            return null;
        return staticBlocks.get(layer)[x][y];
    }

    public void addBlocks(BlockLike... blockLikes) {
        for (BlockLike b : blockLikes) {
            if (b.getLayer().addToStatic) {
                BlockLike block = staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)];
                if (block != null)
                    removeBlocks(block);
                staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = b;
            }
            if (b.getLayer().addToDynamic)
                dynamicBlocks.add(b);
        }
        collisionHandler.register(blockLikes);

    }

    public void removeBlocks(BlockLike... blockLikes) {
        for (BlockLike b : blockLikes) {
            if (b.getLayer().addToStatic)
                staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = null;
            if (b.getLayer().addToDynamic)
                dynamicBlocks.remove(b);
            b.delete();
        }
        collisionHandler.remove(blockLikes);
    }

    public void updateBlocks(RenderEvent type) {
        for (BlockLike[][] layer : staticBlocks.values()) {
            for (BlockLike[] column : layer) {
                for (BlockLike block : column) {
                    if (block != null)
                        block.renderUpdateBlock(type);
                }
            }
        }
    }

    @Override
    public void delete() {
        for (ObjectLayer layer : ObjectLayer.values()) {
            if (!layer.addToStatic)
                continue;
            BlockLike[][] blockLayer = staticBlocks.get(layer);
            for (int i = 0; i < Main.BLOCKS_X; i++) {
                for (int j = 0; j < maximumHeight; j++) {
                    BlockLike b = blockLayer[i][j];
                    if (b != null) {
                        b.delete();
                    }
                    blockLayer[i][j] = null;
                }
            }
        }
        dynamicBlocks.clear();
        inputHandler.delete();
        collisionHandler.delete();
    }
}
