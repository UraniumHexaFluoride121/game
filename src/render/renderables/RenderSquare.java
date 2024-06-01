package render.renderables;

import foundation.ObjPos;
import render.RenderOrder;

import java.awt.*;

public class RenderSquare extends RenderElement {
    public Color color;
    public int up, down, left, right;
    public ObjPos pos;

    public RenderSquare(RenderOrder renderOrder, Color color, int up, int down, int left, int right, ObjPos pos) {
        super(renderOrder);
        this.color = color;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.pos = pos;
    }

    public RenderSquare(RenderOrder renderOrder, Color color, int size, ObjPos pos) {
        this(renderOrder, color, size / 2, size / 2, size / 2, size / 2, pos);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillRect(
                ((int) pos.x) - left,
                ((int) pos.y) - up,
                left + right,
                up + down
        );
    }
}
