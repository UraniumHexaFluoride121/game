package render;

public interface BoundedRenderable extends Renderable {
    RenderOrder getRenderOrder();
    float getTopBound();
    float getBottomBound();
}
