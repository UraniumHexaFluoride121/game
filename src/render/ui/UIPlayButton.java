package render.ui;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.ObjPos;
import physics.HitBox;
import physics.StaticHitBox;
import render.RenderOrder;
import render.renderables.RenderText;
import render.renderables.TextAlign;

import java.awt.*;
import java.awt.event.MouseEvent;

public class UIPlayButton extends UIElement implements Clickable {
    private static final Color backgroundInactive = new Color(163, 163, 163);
    private static final Color borderInactive = new Color(117, 117, 117);
    private static final Color backgroundActive = new Color(135, 200, 123);
    private static final Color borderActive = new Color(79, 174, 45);
    private static final Color backgroundHeldDown = new Color(120, 175, 109);
    private static final Color borderHeldDown = new Color(66, 149, 40);
    private static final float borderWidth = 0.3f;
    private final RenderText enterSeedText;
    private static final float y = -5, width = 8, height = 3;

    private ButtonState state = ButtonState.ACTIVE;
    private final StaticHitBox clickBox;

    public UIPlayButton(int zOrder, UIRegister register) {
        super(zOrder, register);
        clickBox = new StaticHitBox(height / 2, height / 2, width / 2, width / 2, MainPanel.BLOCK_DIMENSIONS.copy().divide(2).addY(y));
        ObjPos enterSeedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(0, y - 0.8f);
        enterSeedText = new RenderText(RenderOrder.UI, () -> enterSeedPos, "GO!", 3, TextAlign.CENTER, 0);
    }

    @Override
    public void render(Graphics2D g) {
        g.scale(1 / 100d, 1 / 100d);
        g.setColor(switch (state) {
            case HELD_DOWN -> borderHeldDown;
            case ACTIVE -> borderActive;
            case INACTIVE -> borderInactive;
        });
        g.fillRoundRect((int) (100 * (MainPanel.BLOCK_DIMENSIONS.x / 2 - width / 2)), (int) (100 * (MainPanel.BLOCK_DIMENSIONS.y / 2 - height / 2 + y)), (int) (100 * width), (int) (100 * height), 100 * 1, 100 * 1);
        g.setColor(switch (state) {
            case HELD_DOWN -> backgroundHeldDown;
            case ACTIVE -> backgroundActive;
            case INACTIVE -> backgroundInactive;
        });
        g.fillRoundRect((int) (100 * (MainPanel.BLOCK_DIMENSIONS.x / 2 - (width / 2 - borderWidth))), (int) (100 * (MainPanel.BLOCK_DIMENSIONS.y / 2 - (height / 2 - borderWidth) + y)), (int) (100 * (width - borderWidth * 2)), (int) (100 * (height - borderWidth * 2)), (int) (100 * (1 - borderWidth)), (int) (100 * (1 - borderWidth)));
        g.scale(100, 100);
        enterSeedText.render(g);
    }

    @Override
    public HitBox clickBox() {
        return clickBox;
    }

    @Override
    public void onClick(MouseEvent e, ObjPos pos, boolean pressed, boolean wasClicked) {
        if (!wasClicked)
            state = ButtonState.INACTIVE;
        else {
            if (pressed)
                state = ButtonState.HELD_DOWN;
            else {
                if (state == ButtonState.HELD_DOWN)
                    Main.window.startLevel();
                state = ButtonState.ACTIVE;
            }
        }
    }
}
