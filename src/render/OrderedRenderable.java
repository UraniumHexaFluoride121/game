package render;

public interface OrderedRenderable extends Renderable {
    RenderOrder getRenderOrder();
}
