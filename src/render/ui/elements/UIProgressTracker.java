package render.ui.elements;

import foundation.math.MathUtil;
import level.Level;
import loader.AssetManager;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;
import render.ui.UIElement;
import render.ui.UIRegister;

import java.awt.*;

public class UIProgressTracker extends UIElement {
    private RenderTextDynamic timeText, heightText, maxHeightText;
    public long startTime = 0;
    private int maxHeight = 0;
    protected Level level;

    public UIProgressTracker(int zOrder, UIRegister register, Level level) {
        super(zOrder, register);
        this.level = level;
        timeText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 2), this::getTime, 2, TextAlign.RIGHT, zOrder);
        heightText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 4.1f), this::getHeight, 2, TextAlign.RIGHT, zOrder);
        maxHeightText = new RenderTextDynamic(RenderOrder.UI, relativeToCamera(right() - 1, top() - 4.8f), this::getMaxHeight, 1, TextAlign.RIGHT, zOrder);
    }

    public UIProgressTracker startTime() {
        startTime = System.currentTimeMillis();
        return this;
    }

    public String getTime() {
        return "*time*" + MathUtil.floatToTimeSpaced(Math.max(0, (System.currentTimeMillis() - startTime) / 1000f), 3);
    }

    public String getHeight() {
        if (level.cameraPlayer == null)
            return "0";
        int height = Math.max(0, Math.round(level.cameraPlayer.pos.y));
        maxHeight = Math.max(maxHeight, height);
        return "*height*" + height;
    }

    public String getMaxHeight() {
        return "/" + maxHeight;
    }

    @Override
    public void render(Graphics2D g) {
        float seconds = Math.max(0, (System.currentTimeMillis() - startTime) / 1000f);
        renderOffset(relativeToCamera(right() - 9.5f, top() - 2.7f).get(), g, g2 -> {
            AssetManager.uiAssets.get("time_ms").render(g2);
        });
        renderOffset(relativeToCamera(right() - 9.5f, top() - 2.7f).get(), g, g2 -> {
            AssetManager.uiAssets.get("time_s").render(g2);
        });
        if (seconds >= 60)
            renderOffset(relativeToCamera(right() - 9.5f, top() - 2.7f).get(), g, g2 -> {
                AssetManager.uiAssets.get("time_m").render(g2);
            });
        if (seconds >= 60 * 60)
            renderOffset(relativeToCamera(right() - 9.5f, top() - 2.7f).get(), g, g2 -> {
                AssetManager.uiAssets.get("time_h").render(g2);
            });
        timeText.render(g);
        heightText.render(g);
        maxHeightText.render(g);
    }
}
