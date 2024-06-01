package render;

import java.awt.*;

public interface Renderable {
    void render(Graphics2D g);
    default RenderOrder getRenderOrder() {
        return RenderOrder.NONE;
    }
}
