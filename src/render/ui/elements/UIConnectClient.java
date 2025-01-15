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

public class UIConnectClient extends UIButton {
    private final RenderTextDynamic text;
    public static UIClientConnectionState connectionState = UIClientConnectionState.NOT_CONNECTED;
    private static final Color backgroundTryingConnection = new Color(175, 172, 109);
    private static final Color borderTryingConnection = new Color(149, 140, 40);
    private static final Color backgroundConnectionFailed = new Color(175, 109, 109);
    private static final Color borderConnectionFailed = new Color(149, 40, 40);

    public UIConnectClient(int zOrder, UIRegister register, ClickableRegister clickableRegister) {
        super(zOrder, register, clickableRegister, ButtonState.INACTIVE, MainPanel.BLOCK_DIMENSIONS.x - 7, MainPanel.BLOCK_DIMENSIONS.y / 2 - 4, 10, 2f);
        ObjPos pos = getCenter().addY(-0.25f);
        text = new RenderTextDynamic(RenderOrder.UI, () -> pos, this::getText, 1, TextAlign.CENTER, 0);
    }

    private String getText() {
        return switch (connectionState) {
            case CONNECTED -> "CONNECTED";
            case NOT_CONNECTED -> "CONNECT";
            case CONNECTION_FAILED -> "CONNECTION FAILED";
            case TRYING_CONNECTION -> "CONNECTING...";
        };
    }

    @Override
    protected Color getBackgroundColor() {
        if (connectionState == UIClientConnectionState.TRYING_CONNECTION)
            return backgroundTryingConnection;
        if (connectionState == UIClientConnectionState.CONNECTION_FAILED)
            return backgroundConnectionFailed;
        return super.getBackgroundColor();
    }

    @Override
    protected Color getBorderColor() {
        if (connectionState == UIClientConnectionState.TRYING_CONNECTION)
            return borderTryingConnection;
        if (connectionState == UIClientConnectionState.CONNECTION_FAILED)
            return borderConnectionFailed;
        return super.getBorderColor();
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

    public static void setConnectionState(UIClientConnectionState connectionState) {
        UIConnectClient.connectionState = connectionState;
    }

    @Override
    protected void buttonClicked() {
        MainPanel.startClient();
    }
}
