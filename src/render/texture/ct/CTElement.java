package render.texture.ct;

import level.Level;
import level.objects.BlockLike;
import render.TickedRenderable;

import java.util.function.BiPredicate;

public class CTElement {
    public final BiPredicate<BlockLike, Level> condition;
    public final TickedRenderable renderable;

    public CTElement(BiPredicate<BlockLike, Level> condition, TickedRenderable renderable) {
        this.condition = condition;
        this.renderable = renderable;
    }

    @Override
    public String toString() {
        return renderable.toString();
    }
}
