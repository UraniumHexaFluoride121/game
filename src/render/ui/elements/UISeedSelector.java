package render.ui.elements;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.ObjPos;
import foundation.tick.Tickable;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ButtonState;
import render.ui.button.TextInputType;
import render.ui.button.UITextInputButton;

import java.awt.*;

public class UISeedSelector extends UITextInputButton implements Tickable {
    private static final int REFRESH_TIME = 0;

    private final RenderTextDynamic enterSeedText;
    private final RenderTextDynamic seedText;

    public UISeedSelector(int zOrder, UIRegister register) {
        super(zOrder, register, ButtonState.INACTIVE, MainPanel.BLOCK_DIMENSIONS.x / 2, MainPanel.BLOCK_DIMENSIONS.y / 2 + 2, 21, 6, TextInputType.DIGITS, 19, "0");
        ObjPos enterSeedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(-9.8f, 3.5f);
        enterSeedText = new RenderTextDynamic(RenderOrder.UI, () -> enterSeedPos, this::getEnterSeedString, 1.5f, TextAlign.LEFT, 0);
        ObjPos seedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(0, 0.5f);
        seedText = new RenderTextDynamic(RenderOrder.UI, () -> seedPos, this::getText, 3f, TextAlign.CENTER, 0);
    }

    private long timeOfLastInput = 0;
    private boolean refreshed = false;

    private String getEnterSeedString() {
        if (state == ButtonState.INACTIVE)
            return "CLICK TO MODIFY SEED";
        return "ENTER SEED...";
    }

    public long getSeed() {
        return Long.parseLong(s.toString());
    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        enterSeedText.render(g);
        seedText.render(g);
    }

    @Override
    public synchronized void tick(float deltaTime) {
        long time = System.currentTimeMillis();
        if (!refreshed && time - timeOfLastInput > REFRESH_TIME) {
            refreshed = true;
            if (!s.isEmpty())
                Main.window.createAndSetNewLevel(getSeed());
        }
    }

    @Override
    protected void textContentsChanged() {
        timeOfLastInput = System.currentTimeMillis();
        refreshed = false;
    }

    @Override
    protected void buttonClicked() {

    }
}
