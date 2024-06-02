package level;

import foundation.Main;
import level.objects.BlockLike;

public class Level {
    public final int maximumHeight;

    //All BlockLikes inserted as static MUST NOT have their positions modified, otherwise
    //they risk not being able to be accessed
    public BlockLike[][] staticBlocks = new BlockLike[Main.BLOCKS_X][];

    public Level(int maximumHeight) {
        this.maximumHeight = maximumHeight;
        for (int i = 0; i < Main.BLOCKS_X; i++) {
            staticBlocks[i] = new BlockLike[maximumHeight];
        }
    }

    public void addStatic(BlockLike b) {
        staticBlocks[((int) b.pos.x)][((int) b.pos.y)] = b;
    }
}
