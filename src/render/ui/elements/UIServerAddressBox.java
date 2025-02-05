package render.ui.elements;

import foundation.MainPanel;
import foundation.math.ObjPos;
import physics.HitBox;
import physics.StaticHitBox;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;
import render.ui.UIRegister;
import render.ui.button.ClickableRegister;
import render.ui.button.TextInputType;
import render.ui.button.UITextInputButton;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static render.ui.button.ButtonState.*;
import static render.ui.elements.UIClientConnectionState.*;
import static render.ui.elements.UIConnectClient.*;

public class UIServerAddressBox extends UITextInputButton {
    private final RenderTextDynamic enterServerAddressText;
    private final RenderTextDynamic serverAddressTextSize1, serverAddressTextSize2, serverAddressTextSize3;

    private UIConnectClient connectButton;
    private StaticHitBox enlargedBox;

    public UIServerAddressBox(int zOrder, UIRegister register, ClickableRegister clickableRegister) {
        super(zOrder, register, clickableRegister, INACTIVE, MainPanel.BLOCK_DIMENSIONS.x - 7, MainPanel.BLOCK_DIMENSIONS.y / 2, 12, 2, TextInputType.IP_ADDRESS, 40, "");
        enlargedBox = createHitBox(getWidth(), getHeight() + 5, getX(), getY() - 2.5f);
        connectButton = new UIConnectClient(0, null, null);
        ObjPos enterServerAddressTextPos = getCenter().addY(-0.25f);
        enterServerAddressText = new RenderTextDynamic(RenderOrder.UI, () -> enterServerAddressTextPos, this::getEnterAddressString, 1f, TextAlign.CENTER, 0);
        ObjPos serverAddressText1Pos = getCenter().add(0, -2f);
        serverAddressTextSize1 = new RenderTextDynamic(RenderOrder.UI, () -> serverAddressText1Pos, this::getText, 2f, TextAlign.CENTER, 0);
        ObjPos serverAddressText2Pos = getCenter().add(0, -1.7f);
        serverAddressTextSize2 = new RenderTextDynamic(RenderOrder.UI, () -> serverAddressText2Pos, this::getText, 1f, TextAlign.CENTER, 0);
        ObjPos serverAddressText3Pos = getCenter().add(0, -1.5f);
        serverAddressTextSize3 = new RenderTextDynamic(RenderOrder.UI, () -> serverAddressText3Pos, this::getText, 0.5f, TextAlign.CENTER, 0);
    }

    private String getEnterAddressString() {
        if (connectionState == CONNECTED)
            return "CONNECTED!";
        if (state == INACTIVE)
            return "CONNECT TO HOST";
        return "ENTER HOST ADDRESS...";
    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        if (connectionState == CONNECTED) {
            MainPanel.startMainLevel();
            return;
        }
        if (state != INACTIVE && e.getKeyCode() == KeyEvent.VK_ENTER) {
            MainPanel.startClient();
            return;
        }
        super.keyPressed(e);
    }

    @Override
    public void allowClick() {
        super.allowClick();
        connectButton.allowClick();
    }

    @Override
    protected float getHeight() {
        return isSmall() ? super.getHeight() : super.getHeight() + 5;
    }

    @Override
    protected float getY() {
        return isSmall() ? super.getY() : super.getY() - 2.5f;
    }

    private boolean isSmall() {
        return state == INACTIVE || connectionState == CONNECTED;
    }

    @Override
    public HitBox clickBox() {
        return isSmall() ? super.clickBox() : enlargedBox;
    }

    @Override
    public void onClick(MouseEvent e, ObjPos pos, boolean pressed, boolean wasClicked) {
        if (connectionState == CONNECTED)
            keepActive(pressed, wasClicked);
        else {
            boolean connectButtonClicked = connectButton.clickBox().isPositionInside(pos);
            if (!connectButtonClicked)
                super.onClick(e, pos, pressed, wasClicked);
            connectButton.onClick(e, pos, pressed, wasClicked && connectButtonClicked);
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (isSmall()) {
            renderImage(g, "lan");
        } else {
            super.render(g);
        }
        enterServerAddressText.render(g);
        if (state != INACTIVE && connectionState != CONNECTED) {
            connectButton.render(g);
            int textWidth = serverAddressTextSize1.calculateTotalWidth();
            if (textWidth > 180)
                serverAddressTextSize3.render(g);
            else if (textWidth > 90)
                serverAddressTextSize2.render(g);
            else
                serverAddressTextSize1.render(g);
        }
    }

    @Override
    protected void textContentsChanged() {

    }

    @Override
    protected void buttonClicked() {

    }

    @Override
    public void delete() {
        super.delete();
        connectButton.delete();
    }
}
