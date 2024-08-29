package render.renderables;

import foundation.Deletable;
import foundation.ObjPos;
import foundation.tick.TickOrder;
import foundation.tick.RegisteredTickable;
import foundation.tick.Tickable;
import render.RenderOrder;
import render.OrderedRenderable;

import java.util.function.Supplier;

public abstract class RenderGameElement implements OrderedRenderable, Tickable, Deletable {
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
}
