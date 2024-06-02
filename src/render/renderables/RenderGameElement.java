package render.renderables;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import foundation.tick.Tickable;
import render.RenderOrder;
import render.Renderable;

import java.util.function.Supplier;

public abstract class RenderGameElement implements Renderable, Tickable {
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

    //Remove reference to parent object to avoid memory leaks
    @Override
    public void delete() {
        gamePos = null;
    }

    //Use for animated textures only, no game logic. This method should only be invoked
    //by the parent object when being rendered, NEVER directly registered to the tick thread.
    @Override
    public void tick(float deltaTime) {

    }

    //Since we never register this object to a tick thread, we need not worry about tick order.
    //It is expected that tick order instead be handled by the rendering object, if necessary.
    @Override
    public TickOrder getTickOrder() {
        return null;
    }
}
