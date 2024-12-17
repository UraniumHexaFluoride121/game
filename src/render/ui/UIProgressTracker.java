package render.ui;

import foundation.math.MathUtil;
import level.Level;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;

import java.awt.*;

public class UIProgressTracker extends UIElement {
    private RenderTextDynamic timeText, heightText;
    private long startTime = 0;
    protected Level level;

    public UIProgressTracker(int zOrder, UIRegister register, Level level) {
        super(zOrder, register);
        this.level = level;
        timeText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 2), this::getTime, 2, TextAlign.RIGHT, zOrder);
        heightText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 3.5f), this::getHeight, 2, TextAlign.RIGHT, zOrder);
    }

    public UIProgressTracker startTime() {
        startTime = System.currentTimeMillis();
        return this;
    }

    public String getTime() {
        return "*time*" + MathUtil.floatToTime((System.currentTimeMillis() - startTime) / 1000f, 3);
    }

    public String getHeight() {
        int height = Math.round(level.cameraPlayer.pos.y);
        return "*height*" + height;
    }

    @Override
    public void render(Graphics2D g) {
        timeText.render(g);
        heightText.render(g);
    }
}
