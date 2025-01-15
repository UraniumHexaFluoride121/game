package render.ui.button;

import foundation.math.ObjPos;
import loader.AssetManager;
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
    protected float x, y, width, height;

    private final StaticHitBox clickBox;
    public ButtonState state;
    public ClickableRegister clickableRegister;


    public UIButton(int zOrder, UIRegister register, ClickableRegister clickableRegister, ButtonState initialState, float x, float y, float width, float height) {
        super(zOrder, register);
        this.clickableRegister = clickableRegister;
        if (clickableRegister != null)
            clickableRegister.registerClickable(this);
        state = initialState;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clickBox = createHitBox(width, height, x, y);
    }

    protected StaticHitBox createHitBox(float width, float height, float x, float y) {
        return new StaticHitBox(height / 2, height / 2, width / 2, width / 2, new ObjPos(x, y));
    }

    public ObjPos getCenter() {
        return new ObjPos(getX(), getY());
    }

    protected Color getBorderColor() {
        return switch (state) {
            case HELD_DOWN -> borderHeldDown;
            case ACTIVE -> borderActive;
            case INACTIVE -> borderInactive;
        };
    }

    protected Color getBackgroundColor() {
        return switch (state) {
            case HELD_DOWN -> backgroundHeldDown;
            case ACTIVE -> backgroundActive;
            case INACTIVE -> backgroundInactive;
        };
    }

    @Override
    public void render(Graphics2D g) {
        g.scale(1 / 100d, 1 / 100d);
        g.setColor(getBorderColor());
        g.fillRoundRect((int) (100 * (getX() - getWidth() / 2)), (int) (100 * (getY() - getHeight() / 2)), (int) (100 * getWidth()), (int) (100 * getHeight()), (int) (100 * rounding), (int) (100 * rounding));
        g.setColor(getBackgroundColor());
        g.fillRoundRect((int) (100 * (getX() - (getWidth() / 2 - borderWidth))), (int) (100 * (getY() - (getHeight() / 2 - borderWidth))), (int) (100 * (getWidth() - borderWidth * 2)), (int) (100 * (getHeight() - borderWidth * 2)), (int) (100 * (rounding - borderWidth)), (int) (100 * (rounding - borderWidth)));
        g.scale(100, 100);
    }

    protected float getY() {
        return y;
    }

    protected float getX() {
        return x;
    }

    protected float getHeight() {
        return height;
    }

    protected float getWidth() {
        return width;
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

    protected boolean clicked = false;

    public void allowClick() {
        clicked = false;
    }

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

    @Override
    public void delete() {
        super.delete();
        if (clickableRegister != null) {
            clickableRegister.removeClickable(this);
            clickableRegister = null;
        }
    }

    protected void renderImage(Graphics2D g, String name) {
        g.translate(x - width / 2, y - height / 2);
        AssetManager.uiAssets.get(name).render(g);
        g.translate(width / 2- x, height / 2 - y);
    }

    protected void renderImage(Graphics2D g, String name, float x, float y) {
        g.translate(this.x - width / 2 + x, this.y - height / 2 + y);
        AssetManager.uiAssets.get(name).render(g);
        g.translate(width / 2 - this.x - x, height / 2 - this.y - y);
    }

    protected abstract void buttonClicked();
}
