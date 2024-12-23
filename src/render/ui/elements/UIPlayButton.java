package render.ui.elements;

import foundation.MainPanel;
import foundation.math.ObjPos;
import network.NetworkState;
import render.RenderOrder;
import render.renderables.RenderText;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ButtonState;
import render.ui.button.ClickableRegister;
import render.ui.button.UIButton;

import java.awt.*;

public class UIPlayButton extends UIButton {
    private final RenderText startText, waitingForHostLine1, waitingForHostLine2;

    public UIPlayButton(int zOrder, UIRegister register, ClickableRegister clickableRegister) {
        super(zOrder, register, clickableRegister, ButtonState.ACTIVE, MainPanel.BLOCK_DIMENSIONS.x / 2, MainPanel.BLOCK_DIMENSIONS.y / 2 - 5, 8, 3.8f);
        ObjPos startTextPos = getCenter().add(0, -0.8f);
        ObjPos waitingForHostLine1Pos = getCenter().add(0, 0.2f);
        ObjPos waitingForHostLine2Pos = getCenter().add(0, -1.3f);
        startText = new RenderText(RenderOrder.UI, () -> startTextPos, "START!", 3, TextAlign.CENTER, 0);
        waitingForHostLine1 = new RenderText(RenderOrder.UI, () -> waitingForHostLine1Pos, "WAITING", 2, TextAlign.CENTER, 0);
        waitingForHostLine2 = new RenderText(RenderOrder.UI, () -> waitingForHostLine2Pos, "FOR HOST", 2, TextAlign.CENTER, 0);
    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        if (MainPanel.networkState == NetworkState.CLIENT && !MainPanel.levelFullyGenerated(MainPanel.currentLevelIndex)) {
            waitingForHostLine1.render(g);
            waitingForHostLine2.render(g);
        } else {
            startText.render(g);
        }
    }

    @Override
    protected void buttonClicked() {
        MainPanel.startMainLevel();
    }
}
