package foundation;

import foundation.input.InputType;
import foundation.math.MathUtil;
import foundation.math.ObjPos;
import foundation.tick.RegisteredTickable;
import foundation.tick.TickOrder;
import level.Level;
import loader.AssetManager;
import loader.ResourceLocation;
import render.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;

public class MainPanel extends JFrame implements KeyListener, MouseListener, RegisteredTickable {
    //path to the main level file
    public static final ResourceLocation LEVEL_PATH = new ResourceLocation("level.json");
    public static AffineTransform gameTransform = new AffineTransform();

    public static ObjPos DEVICE_WINDOW_SIZE; //the physical screen size, in pixels
    public static ObjPos RENDER_WINDOW_SIZE; //the size of the render box, in pixels
    public static ObjPos BLOCK_DIMENSIONS; //the size of the render box, in blocks

    public GameState state = GameState.MAIN_MENU;

    private Level activeLevel;

    private final UIRenderer mainMenuUI = new UIRenderer();
    private final ArrayList<Clickable> mainMenuClickables = new ArrayList<>();
    private UISeedSelector seedSelector;
    private UIPlayButton playButton;
    private float levelTint = 0.3f;
    private float mainMenuYOffset = 0f;

    public MainPanel() throws HeadlessException {
        super();
        registerTickable();
    }

    public void init() {
        AssetManager.readRegions(LEVEL_PATH);
        AssetManager.readBlocks(LEVEL_PATH);
        AssetManager.readGlyphs(LEVEL_PATH);
        AssetManager.readLayoutMarkerData(LEVEL_PATH);
        seedSelector = new UISeedSelector(0, mainMenuUI);
        mainMenuClickables.add(seedSelector);
        playButton = new UIPlayButton(0, mainMenuUI);
        mainMenuClickables.add(playButton);
    }

    public void createAndSetNewLevel(long seed) {
        if (activeLevel != null && !activeLevel.deleted)
            activeLevel.delete();
        activeLevel = new Level(seed);
        activeLevel.init();
        cameraY = 1;
    }

    public void startLevel() {
        state = GameState.PLAYING_LEVEL;
        if (activeLevel.seed != seedSelector.getSeed())
            createAndSetNewLevel(seedSelector.getSeed());
        activeLevel.generateFull();
        activeLevel.createUI();
    }

    @Override
    public void paintComponents(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (levelTint != 1)
            g2d.drawImage(levelToImage(), new RescaleOp(new float[]{levelTint}, new float[]{0}, g2d.getRenderingHints()), 0, 0);
        else
            drawLevel(g2d);

        if (mainMenuYOffset != BLOCK_DIMENSIONS.y) {
            AffineTransform prev = g2d.getTransform();
            g2d.transform(gameTransform);
            g2d.translate(0, mainMenuYOffset);
            mainMenuUI.render(g2d);
            g2d.setTransform(prev);
        }
    }

    public BufferedImage levelToImage() {
        BufferedImage image = new BufferedImage(DEVICE_WINDOW_SIZE.xInt(), DEVICE_WINDOW_SIZE.yInt(), Image.SCALE_DEFAULT);
        Graphics2D g2d = image.createGraphics();
        drawLevel(g2d);
        return image;
    }

    public void drawLevel(Graphics2D g2d) {

        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        //Render background separately so that it's not affected by camera movement
        if (activeLevel != null && !activeLevel.deleted) {
            AffineTransform prev = g2d.getTransform();
            g2d.transform(gameTransform);
            activeLevel.background.render(g2d);
            g2d.setTransform(prev);

            activeLevel.gameRenderer.render(g2d);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state == GameState.PLAYING_LEVEL && activeLevel != null && !activeLevel.deleted)
            activeLevel.inputHandler.queueInput(InputType.KEY_PRESSED, e);
        if (state == GameState.MAIN_MENU) {
            seedSelector.keyPressed(e);
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
                startLevel();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (state == GameState.PLAYING_LEVEL && activeLevel != null && !activeLevel.deleted)
            activeLevel.inputHandler.queueInput(InputType.KEY_RELEASED, e);
    }

    @Override
    public void delete() {

    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.LEVEL_UPDATES;
    }

    public static float cameraY = 1;

    public static AffineTransform getCameraTransform() {
        AffineTransform t = new AffineTransform();
        t.translate(0, cameraY);
        return t;
    }

    @Override
    public void tick(float deltaTime) {
        if (state == GameState.MAIN_MENU) {
            seedSelector.tick(deltaTime);
            levelTint = MathUtil.linearTo(levelTint, 0.3f, 3, deltaTime);
            mainMenuYOffset = MathUtil.linearTo(mainMenuYOffset, 0, 30, deltaTime);
        } else {
            levelTint = MathUtil.linearTo(levelTint, 1, 3, deltaTime);
            mainMenuYOffset = MathUtil.linearTo(mainMenuYOffset, BLOCK_DIMENSIONS.y, BLOCK_DIMENSIONS.y * 3, deltaTime);
        }
        if (activeLevel != null && !activeLevel.deleted) {
            float y = Math.min(0, -activeLevel.cameraPlayer.pos.y + activeLevel.getCameraOffset());
            if (cameraY == 1)
                cameraY = y;
            else {
                cameraY += (y - cameraY) * deltaTime * 2.5f;
                cameraY += Math.min(Math.abs(y - cameraY), 0.3f * deltaTime) * Math.signum(y - cameraY);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point point = getMousePosition();
        if (point == null)
            return;
        ObjPos pos = new ObjPos(point).divide((float) gameTransform.getScaleX(), ((float) gameTransform.getScaleY())).addY(BLOCK_DIMENSIONS.y);
        if (state == GameState.MAIN_MENU)
            mainMenuClickables.forEach(c -> c.onClick(e, pos, true, c.clickBox().isPositionInside(pos)));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point point = getMousePosition();
        if (point == null)
            return;
        ObjPos pos = new ObjPos(point).divide((float) gameTransform.getScaleX(), ((float) gameTransform.getScaleY())).addY(BLOCK_DIMENSIONS.y);
        if (state == GameState.MAIN_MENU)
            mainMenuClickables.forEach(c -> c.onClick(e, pos, false, c.clickBox().isPositionInside(pos)));
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
