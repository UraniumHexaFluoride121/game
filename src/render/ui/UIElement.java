package render.ui;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.ObjPos;

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
