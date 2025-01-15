package render.ui;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.ObjPos;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class UIElement implements UIRenderable, Deletable {
    private final int zOrder;
    protected UIRegister register;

    public UIElement(int zOrder, UIRegister register) {
        this.zOrder = zOrder;
        this.register = register;
        if (register != null)
            register.registerUI(this);
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    @Override
    public void delete() {
        if (register != null) {
            register.removeUI(this);
            register = null;
        }
    }

    public static void renderOffset(ObjPos pos, Graphics2D g, Consumer<Graphics2D> render) {
        renderOffset(pos.x, pos.y, g, render);
    }

    public static void renderOffset(float x, float y, Graphics2D g, Consumer<Graphics2D> render) {
        AffineTransform prev = g.getTransform();
        g.translate(x, y);
        render.accept(g);
        g.setTransform(prev);
    }

    public Supplier<ObjPos> relativeToCamera(float xOffset, float yOffset) {
        return () -> new ObjPos(xOffset, -MainPanel.cameraY + yOffset);
    }

    public float top() {
        return MainPanel.BLOCK_DIMENSIONS.y;
    }

    public float right() {
        return MainPanel.BLOCK_DIMENSIONS.x;
    }
}
