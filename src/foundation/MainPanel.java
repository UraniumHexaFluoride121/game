package foundation;

import foundation.input.InputHandler;
import foundation.input.InputType;
import foundation.math.ObjPos;
import foundation.tick.RegisteredTickable;
import foundation.tick.TickOrder;
import level.Level;
import loader.AssetManager;
import loader.ResourceLocation;
import render.GameRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;

public class MainPanel extends JFrame implements KeyListener, RegisteredTickable {
    //path to the main level file
    public static final ResourceLocation LEVEL_PATH = new ResourceLocation("level.json");
    public static AffineTransform gameTransform = new AffineTransform();
    public static final GameRenderer GAME_RENDERER = new GameRenderer(gameTransform, MainPanel::getCameraTransform);

    public static ObjPos DEVICE_WINDOW_SIZE; //the physical screen size, in pixels
    public static ObjPos RENDER_WINDOW_SIZE; //the size of the render box, in pixels
    public static ObjPos BLOCK_DIMENSIONS; //the size of the render box, in blocks

    public static Level level;

    public MainPanel() throws HeadlessException {
        super();
        registerTickable();
    }

    public void init() {
        AssetManager.readRegions(LEVEL_PATH);
        AssetManager.readBlocks(LEVEL_PATH);
        level = new Level();
        AssetManager.readLayoutMarkerData(LEVEL_PATH);
        level.init();
        //Expression parser debug
        /*System.out.println(CTExpression.parser.parseExpression("block[u].hasCollision").apply(new CTExpressionData(
                level.getBlock(ObjectLayer.FOREGROUND, 4, 1), level
        )));*/
    }

    public static InputHandler getInputHandler() {
        return level.inputHandler;
    }

    @Override
    public void paintComponents(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        //Render background outside of GAME_RENDERER so that it's not affected by camera movement
        AffineTransform prev = g2d.getTransform();
        g2d.transform(gameTransform);
        level.background.render(g2d);
        g2d.setTransform(prev);

        GAME_RENDERER.render(g2d);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        level.inputHandler.queueInput(InputType.KEY_PRESSED, e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        level.inputHandler.queueInput(InputType.KEY_RELEASED, e);
    }

    @Override
    public void delete() {

    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.ANIMATIONS_ONLY;
    }

    public static float cameraY = 1;

    public static AffineTransform getCameraTransform() {
        AffineTransform t = new AffineTransform();
        t.translate(0, cameraY);
        return t;
    }

    @Override
    public void tick(float deltaTime) {
        float y = Math.min(0, -level.cameraPlayer.pos.y + level.getCameraOffset());
        if (cameraY == 1)
            cameraY = y;
        else {
            cameraY += (y - cameraY) * deltaTime * 2.5f;
            cameraY += Math.min(Math.abs(y - cameraY), 0.3f * deltaTime) * Math.signum(y - cameraY);
        }
    }
}
