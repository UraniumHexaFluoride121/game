package render.ui;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.ObjPos;

import java.util.function.Supplier;

public abstract class UIElement implements UIRenderable, Deletable {
    private final int zOrder;

    public UIElement(int zOrder) {
        this.zOrder = zOrder;
        MainPanel.GAME_RENDERER.registerUI(this);
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    @Override
    public void delete() {
        MainPanel.GAME_RENDERER.removeUI(this);
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
