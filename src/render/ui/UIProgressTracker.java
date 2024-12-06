package render.ui;

import foundation.MainPanel;
import foundation.math.MathHelper;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;

import java.awt.*;

public class UIProgressTracker extends UIElement {
    private RenderTextDynamic timeText, heightText;
    private long startTime = 0;

    public UIProgressTracker(int zOrder) {
        super(zOrder);
        timeText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 2), this::getTime, 2, TextAlign.RIGHT);
        heightText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 3.5f), this::getHeight, 2, TextAlign.RIGHT);
    }

    public UIProgressTracker startTime() {
        startTime = System.currentTimeMillis();
        return this;
    }

    public String getTime() {
        return "*time*" + MathHelper.floatToString((System.currentTimeMillis() - startTime) / 1000f, 3);
    }

    public String getHeight() {
        return "*height*" + MathHelper.floatToString(Math.round(MainPanel.level.cameraPlayer.pos.y), 0);
    }

    @Override
    public void render(Graphics2D g) {
        timeText.render(g);
        heightText.render(g);
    }
}
