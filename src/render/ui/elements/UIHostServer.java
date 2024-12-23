package render.ui.elements;

import foundation.MainPanel;
import foundation.math.ObjPos;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ButtonState;
import render.ui.button.ClickableRegister;
import render.ui.button.UIButton;

import java.awt.*;
import java.awt.event.MouseEvent;

public class UIHostServer extends UIButton {
    private final RenderTextDynamic text;

    public UIHostServer(int zOrder, UIRegister register, ClickableRegister clickableRegister) {
        super(zOrder, register, clickableRegister, ButtonState.INACTIVE, MainPanel.BLOCK_DIMENSIONS.x - 7, MainPanel.BLOCK_DIMENSIONS.y / 2 + 4f, 12, 2f);
        ObjPos pos = getCenter().addY(-0.25f);
        text = new RenderTextDynamic(RenderOrder.UI, () -> pos, this::getText, 1, TextAlign.CENTER, 0);
    }

    private String getText() {
        return state == ButtonState.INACTIVE ? "HOST TO LAN" : "LAN SERVER STARTED";
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
        MainPanel.startServer();
    }
}
