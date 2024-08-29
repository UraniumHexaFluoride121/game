package render;

public interface OrderedRenderable extends Renderable {
    default RenderOrder getRenderOrder() {
        return RenderOrder.NONE;
    }
}
