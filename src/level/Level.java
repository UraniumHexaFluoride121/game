package level;

import foundation.Deletable;
import foundation.Main;
import foundation.input.InputHandler;
import level.objects.BlockLike;

public class Level implements Deletable {
    public final int maximumHeight;
    public final InputHandler inputHandler;

    //All BlockLikes inserted as static MUST NOT have their positions modified, otherwise
    //they risk not being able to be accessed
    public BlockLike[][] staticBlocks = new BlockLike[Main.BLOCKS_X][];

    public Level(int maximumHeight) {
        inputHandler = new InputHandler();
        this.maximumHeight = maximumHeight;
        for (int i = 0; i < Main.BLOCKS_X; i++) {
            staticBlocks[i] = new BlockLike[maximumHeight];
        }
    }

    public void addStatic(BlockLike b) {
        staticBlocks[((int) b.pos.x)][((int) b.pos.y)] = b;
    }

    @Override
    public void delete() {
        for (int i = 0; i < Main.BLOCKS_X; i++) {
            for (int j = 0; j < maximumHeight; j++) {
                BlockLike b = staticBlocks[i][j];
                if (b != null)
                    b.delete();
                staticBlocks[i][j] = null;
            }
        }
        inputHandler.delete();
    }
}
