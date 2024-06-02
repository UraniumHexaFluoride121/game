package render.renderables;

import foundation.MainPanel;
import render.RenderOrder;
import render.Renderable;

import java.awt.*;

public class RenderBackground implements Renderable {
    public Color color;
    public RenderBackground(Color color) {
        this.color = color;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(color);
        g.fillRect(0, 0, (int) MainPanel.RENDER_WINDOW_SIZE.x, (int) MainPanel.RENDER_WINDOW_SIZE.y);
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.BACKGROUND;
    }
}
