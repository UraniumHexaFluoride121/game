package render.ui.button;

import foundation.math.ObjPos;
import physics.HitBox;
import physics.StaticHitBox;
import render.ui.UIElement;
import render.ui.UIRegister;

import java.awt.*;
import java.awt.event.MouseEvent;

public abstract class UIButton extends UIElement implements Clickable {
    private static final Color backgroundInactive = new Color(163, 163, 163);
    private static final Color borderInactive = new Color(117, 117, 117);
    private static final Color backgroundActive = new Color(135, 200, 123);
    private static final Color borderActive = new Color(79, 174, 45);
    private static final Color backgroundHeldDown = new Color(120, 175, 109);
    private static final Color borderHeldDown = new Color(66, 149, 40);

    private static final float borderWidth = 0.3f;
    private static final float rounding = 1;
    private final float x, y, width, height;

    private final StaticHitBox clickBox;
    public ButtonState state;


    public UIButton(int zOrder, UIRegister register, ButtonState initialState, float x, float y, float width, float height) {
        super(zOrder, register);
        state = initialState;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clickBox = new StaticHitBox(height / 2, height / 2, width / 2, width / 2, new ObjPos(x, y));
    }

    public ObjPos getCenter() {
        return new ObjPos(x, y);
    }

    @Override
    public void render(Graphics2D g) {
        g.scale(1 / 100d, 1 / 100d);
        g.setColor(switch (state) {
            case HELD_DOWN -> borderHeldDown;
            case ACTIVE -> borderActive;
            case INACTIVE -> borderInactive;
        });
        g.fillRoundRect((int) (100 * (x - width / 2)), (int) (100 * (y - height / 2)), (int) (100 * width), (int) (100 * height), (int) (100 * rounding), (int) (100 * rounding));
        g.setColor(switch (state) {
            case HELD_DOWN -> backgroundHeldDown;
            case ACTIVE -> backgroundActive;
            case INACTIVE -> backgroundInactive;
        });
        g.fillRoundRect((int) (100 * (x - (width / 2 - borderWidth))), (int) (100 * (y - (height / 2 - borderWidth))), (int) (100 * (width - borderWidth * 2)), (int) (100 * (height - borderWidth * 2)), (int) (100 * (rounding - borderWidth)), (int) (100 * (rounding - borderWidth)));
        g.scale(100, 100);
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
                    buttonClicked();
                state = ButtonState.ACTIVE;
            }
        }
    }

    public boolean clicked = false;
    protected void keepActive(boolean pressed, boolean wasClicked) {
        if (!wasClicked) {
            if (state == ButtonState.HELD_DOWN) {
                state = clicked ? ButtonState.ACTIVE : ButtonState.INACTIVE;
            }
            return;
        }
        if (pressed)
            state = ButtonState.HELD_DOWN;
        else {
            if (!clicked && state == ButtonState.HELD_DOWN) {
                clicked = true;
                buttonClicked();
            }
            state = ButtonState.ACTIVE;
        }
    }

    protected abstract void buttonClicked();
}
