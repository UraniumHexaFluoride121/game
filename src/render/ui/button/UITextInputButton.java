package render.ui.button;

import render.ui.UIRegister;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public abstract class UITextInputButton extends UIButton implements KeyListener {
    protected StringBuilder s = new StringBuilder();

    private final TextInputType inputType;
    private final int maxLength;

    public UITextInputButton(int zOrder, UIRegister register, ClickableRegister clickableRegister, ButtonState initialState, float x, float y, float width, float height, TextInputType inputType, int maxLength, String initialText) {
        super(zOrder, register, clickableRegister, initialState, x, y, width, height);
        s.append(initialText);
        this.inputType = inputType;
        this.maxLength = maxLength;
    }

    abstract protected void textContentsChanged();

    private boolean initial = true;

    public String getText() {
        return s.toString().toUpperCase();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        if (state == ButtonState.INACTIVE)
            return;

        char c = e.getKeyChar();
        if (inputType == TextInputType.DIGITS) {
            if (inputType.isValid(c) && s.length() < maxLength) {
                if (initial) {
                    if (c == '0')
                        return;
                    else
                        s.setCharAt(0, c);
                    initial = false;
                } else
                    s.append(c);
                if (c != '0') {
                    while (s.charAt(0) == '0')
                        s.deleteCharAt(0);
                }
                textContentsChanged();
                return;
            }
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (!s.isEmpty()) {
                    s.deleteCharAt(s.length() - 1);
                    if (s.isEmpty()) {
                        initial = true;
                        s.append(0);
                    }
                    textContentsChanged();
                }
            }
        } else {
            if (inputType.isValid(c) && s.length() < maxLength) {
                s.append(c);
                textContentsChanged();
                return;
            }
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (!s.isEmpty()) {
                    s.deleteCharAt(s.length() - 1);
                    textContentsChanged();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public synchronized void render(Graphics2D g) {
        super.render(g);
    }
}
