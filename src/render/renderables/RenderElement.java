package render.renderables;

import render.RenderOrder;
import render.Renderable;

public abstract class RenderElement implements Renderable {
    private final RenderOrder renderOrder;

    public RenderElement(RenderOrder renderOrder) {
        this.renderOrder = renderOrder;
    }

    @Override
    public RenderOrder getRenderOrder() {
        return renderOrder;
    }
}
