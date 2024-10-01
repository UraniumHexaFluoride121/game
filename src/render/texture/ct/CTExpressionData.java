package render.texture.ct;

import level.Level;
import level.objects.BlockLike;

public class CTExpressionData {
    public final BlockLike b;
    public final Level l;

    public CTExpressionData(BlockLike b, Level l) {
        this.b = b;
        this.l = l;
    }
}
