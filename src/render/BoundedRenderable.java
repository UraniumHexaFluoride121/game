package render;

public interface BoundedRenderable extends Renderable {
    RenderOrder getRenderOrder();
    float getTopRenderBound();
    float getBottomRenderBound();
    int getZOrder();
}
