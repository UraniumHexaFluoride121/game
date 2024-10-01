package render.texture.ct;

import level.Level;
import level.objects.BlockLike;
import render.Renderable;

import java.util.function.BiPredicate;

public class CTElement {
    public final BiPredicate<BlockLike, Level> condition;
    public final Renderable renderable;

    public CTElement(BiPredicate<BlockLike, Level> condition, Renderable renderable) {
        this.condition = condition;
        this.renderable = renderable;
    }
}
