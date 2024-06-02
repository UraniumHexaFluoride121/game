package render.renderables;

import foundation.ObjPos;
import render.RenderOrder;

import java.awt.*;
import java.util.function.Supplier;

public class RenderGameSquare extends RenderGameElement {
    public Color color;
    public float up, down, left, right;

    public RenderGameSquare(RenderOrder renderOrder, Color color, float up, float down, float left, float right, Supplier<ObjPos> gamePos) {
        super(renderOrder, gamePos);
        this.color = color;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }

    public RenderGameSquare(RenderOrder renderOrder, Color color, float size, Supplier<ObjPos> gamePos) {
        this(renderOrder, color, size / 2, size / 2, size / 2, size / 2, gamePos);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(color);
        g.scale(1 / 1000d, 1 / 1000d);
        g.fillRect(
                (int) ((gamePos.get().x - left) * 1000),
                (int) ((gamePos.get().y - down) * 1000),
                (int) ((left + right) * 1000),
                (int) ((up + down) * 1000)
        );
        g.scale(1000, 1000);
    }
}
