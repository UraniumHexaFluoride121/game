package render.ui.elements;

import foundation.MainPanel;
import foundation.math.ObjPos;
import foundation.tick.Tickable;
import level.Level;
import network.NetworkState;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ButtonState;
import render.ui.button.ClickableRegister;
import render.ui.button.TextInputType;
import render.ui.button.UITextInputButton;

import java.awt.*;
import java.awt.event.KeyEvent;

public class UISeedSelector extends UITextInputButton implements Tickable {
    private static final int REFRESH_TIME = 0;

    private final RenderTextDynamic enterSeedText;
    private final RenderTextDynamic seedText;

    public UISeedSelector(int zOrder, UIRegister register, ClickableRegister clickableRegister) {
        super(zOrder, register, clickableRegister, ButtonState.INACTIVE, MainPanel.BLOCK_DIMENSIONS.x / 2, MainPanel.BLOCK_DIMENSIONS.y / 2 + 2, 21, 6, TextInputType.DIGITS, 19, "0");
        ObjPos enterSeedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(-9.8f, 3.5f);
        enterSeedText = new RenderTextDynamic(RenderOrder.UI, () -> enterSeedPos, this::getEnterSeedString, 1.5f, TextAlign.LEFT, 0);
        ObjPos seedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(0, 0.5f);
        seedText = new RenderTextDynamic(RenderOrder.UI, () -> seedPos, this::getText, 3f, TextAlign.CENTER, 0);
    }

    private long timeOfLastInput = 0;
    private boolean refreshed = false;

    private String getEnterSeedString() {
        if (MainPanel.networkState == NetworkState.CLIENT)
            return "SEED IS DECIDED BY THE HOST";
        if (state == ButtonState.INACTIVE)
            return "CLICK TO MODIFY SEED";
        return "ENTER SEED...";
    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        if (MainPanel.networkState != NetworkState.CLIENT)
            super.keyPressed(e);
    }

    @Override
    public String getText() {
        if (MainPanel.networkState == NetworkState.CLIENT) {
            Level activeLevel = MainPanel.getLevel(MainPanel.currentLevelIndex);
            if (activeLevel == null)
                return s.toString();
            return String.valueOf(activeLevel.seed);
        }
        return super.getText();
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
                MainPanel.createNewMainLevel(getSeed());
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
