package render.renderables;

import foundation.ObjPos;
import render.RenderOrder;

import java.awt.*;

public class RenderBackground extends RenderElement {
    public Color color;
    public RenderBackground(Color color) {
        super(RenderOrder.BACKGROUND);
        this.color = color;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(color);
        g.fillRect(0, 0, (int) ObjPos.RENDER_WINDOW_SIZE.x, (int) ObjPos.RENDER_WINDOW_SIZE.y);
    }
}
