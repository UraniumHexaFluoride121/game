package render.ui.elements;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.ObjPos;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ButtonState;
import render.ui.button.UIButton;

import java.awt.*;
import java.awt.event.MouseEvent;

public class UIConnectToClient extends UIButton {
    private final RenderTextDynamic text;

    public UIConnectToClient(int zOrder, UIRegister register) {
        super(zOrder, register, ButtonState.INACTIVE, MainPanel.BLOCK_DIMENSIONS.x - 5, MainPanel.BLOCK_DIMENSIONS.y / 2 - 1, 9, 2f);
        ObjPos pos = getCenter().addY(-0.25f);
        text = new RenderTextDynamic(RenderOrder.UI, () -> pos, this::getText, 1, TextAlign.CENTER, 0);
    }

    private String getText() {
        return state == ButtonState.INACTIVE ? "CONNECT TO LAN SERVER" : "CONNECTED";
    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        text.render(g);
    }

    @Override
    public void onClick(MouseEvent e, ObjPos pos, boolean pressed, boolean wasClicked) {
        keepActive(pressed, wasClicked);
    }

    @Override
    protected void buttonClicked() {
        Main.window.startClient();
    }
}
