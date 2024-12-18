package render.ui;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.ObjPos;
import foundation.tick.Tickable;
import physics.HitBox;
import physics.StaticHitBox;
import render.RenderOrder;
import render.renderables.RenderTextDynamic;
import render.renderables.TextAlign;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class UISeedSelector extends UIElement implements Tickable, Clickable {
    private static final int REFRESH_TIME = 0;

    private static final Color backgroundInactive = new Color(163, 163, 163);
    private static final Color borderInactive = new Color(117, 117, 117);
    private static final Color backgroundActive = new Color(135, 200, 123);
    private static final Color borderActive = new Color(79, 174, 45);
    private static final Color backgroundHeldDown = new Color(120, 175, 109);
    private static final Color borderHeldDown = new Color(66, 149, 40);
    private static final float borderWidth = 0.3f;
    private final RenderTextDynamic enterSeedText;
    private final RenderTextDynamic seedText;
    private final StringBuilder seed = new StringBuilder().append(0);

    private ButtonState state = ButtonState.INACTIVE;
    private final StaticHitBox clickBox;

    public UISeedSelector(int zOrder, UIRegister register) {
        super(zOrder, register);
        clickBox = new StaticHitBox(6, 0, 0, 21, MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(-10.5f, -1));
        ObjPos enterSeedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(-9.8f, 3.5f);
        enterSeedText = new RenderTextDynamic(RenderOrder.UI, () -> enterSeedPos, this::getEnterSeedString, 1.5f, TextAlign.LEFT, 0);
        ObjPos seedPos = MainPanel.BLOCK_DIMENSIONS.copy().divide(2).add(0, 0.5f);
        seedText = new RenderTextDynamic(RenderOrder.UI, () -> seedPos, this::getSeedString, 3f, TextAlign.CENTER, 0);
    }

    private long timeOfLastInput = 0;
    private boolean refreshed = false;

    private boolean initial = true;

    public synchronized void keyPressed(KeyEvent e) {
        if (state == ButtonState.INACTIVE)
            return;
        char c = e.getKeyChar();
        if (Character.isDigit(c) && seed.length() < 18) {
            if (initial) {
                if (c == '0')
                    return;
                else
                    seed.setCharAt(0, c);
                initial = false;
            } else
                seed.append(c);
            if (c != '0') {
                int i = 0;
                while (seed.charAt(0) == '0')
                    seed.deleteCharAt(0);
            }
            timeOfLastInput = System.currentTimeMillis();
            refreshed = false;
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (!seed.isEmpty()) {
                seed.deleteCharAt(seed.length() - 1);
                if (seed.isEmpty()) {
                    initial = true;
                    seed.append(0);
                }
                timeOfLastInput = System.currentTimeMillis();
                refreshed = false;
            }
        }
    }

    private String getSeedString() {
        return seed.toString();
    }

    private String getEnterSeedString() {
        if (state == ButtonState.INACTIVE)
            return "CLICK TO SELECT SEED";
        return "ENTER SEED...";
    }

    public long getSeed() {
        return Long.parseLong(seed.toString());
    }

    @Override
    public void render(Graphics2D g) {
        g.scale(1 / 100d, 1 / 100d);
        g.setColor(switch (state) {
            case HELD_DOWN -> borderHeldDown;
            case ACTIVE -> borderActive;
            case INACTIVE -> borderInactive;
        });
        g.fillRoundRect((int) (100 * (MainPanel.BLOCK_DIMENSIONS.x / 2 - 10.5f)), (int) (100 * (MainPanel.BLOCK_DIMENSIONS.y / 2 - 1)), 100 * 21, 100 * 6, 100 * 1, 100 * 1);
        g.setColor(switch (state) {
            case HELD_DOWN -> backgroundHeldDown;
            case ACTIVE -> backgroundActive;
            case INACTIVE -> backgroundInactive;
        });
        g.fillRoundRect((int) (100 * (MainPanel.BLOCK_DIMENSIONS.x / 2 - (10.5f - borderWidth))), (int) (100 * (MainPanel.BLOCK_DIMENSIONS.y / 2 - (1 - borderWidth))), (int) (100 * (21 - borderWidth * 2)), (int) (100 * (6 - borderWidth * 2)), (int) (100 * (1 - borderWidth)), (int) (100 * (1 - borderWidth)));
        g.scale(100, 100);
        enterSeedText.render(g);
        seedText.render(g);
    }

    @Override
    public synchronized void tick(float deltaTime) {
        long time = System.currentTimeMillis();
        if (!refreshed && time - timeOfLastInput > REFRESH_TIME) {
            refreshed = true;
            if (!seed.isEmpty())
                Main.window.createAndSetNewLevel(getSeed());
        }
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
            else
                state = ButtonState.ACTIVE;
        }
    }
}
