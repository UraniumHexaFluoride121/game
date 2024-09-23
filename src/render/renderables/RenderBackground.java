package render.renderables;

import foundation.MainPanel;
import render.RenderOrder;
import render.OrderedRenderable;

import java.awt.*;

public class RenderBackground implements OrderedRenderable {
    public Color color;
    public RenderBackground(Color color) {
        this.color = color;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(color);
        g.scale(1 / 1000d, 1 / 1000d);
        g.fillRect(0, 0, (int) (MainPanel.BLOCK_DIMENSIONS.x * 1000), (int) (MainPanel.BLOCK_DIMENSIONS.y * 1000));
        g.scale(1000, 1000);
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.SOLID_COLOUR_BACKGROUND;
    }
}
