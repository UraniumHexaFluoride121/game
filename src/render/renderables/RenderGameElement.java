package render.renderables;

import foundation.Deletable;
import foundation.ObjPos;
import render.RenderOrder;
import render.Renderable;

import java.util.function.Supplier;

public abstract class RenderGameElement implements Renderable, Deletable {
    private final RenderOrder renderOrder;
    protected Supplier<ObjPos> gamePos;

    public RenderGameElement(RenderOrder renderOrder, Supplier<ObjPos> gamePos) {
        this.renderOrder = renderOrder;
        this.gamePos = gamePos;
    }

    @Override
    public RenderOrder getRenderOrder() {
        return renderOrder;
    }

    //remove reference to parent object to avoid memory leaks
    @Override
    public void delete() {
        gamePos = null;
    }
}
