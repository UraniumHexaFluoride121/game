package render.ui.elements;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.ObjPos;
import render.RenderOrder;
import render.renderables.RenderText;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ButtonState;
import render.ui.button.UIButton;

import java.awt.*;

public class UIPlayButton extends UIButton {
    private final RenderText enterSeedText;

    public UIPlayButton(int zOrder, UIRegister register) {
        super(zOrder, register, ButtonState.ACTIVE, MainPanel.BLOCK_DIMENSIONS.x / 2, MainPanel.BLOCK_DIMENSIONS.y / 2 - 5, 8, 3.5f);
        ObjPos enterSeedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(0, -5.8f);
        enterSeedText = new RenderText(RenderOrder.UI, () -> enterSeedPos, "START!", 3, TextAlign.CENTER, 0);
    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        enterSeedText.render(g);
    }

    @Override
    protected void buttonClicked() {
        Main.window.startLevel();
    }
}
