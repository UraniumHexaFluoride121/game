package foundation;

import foundation.input.InputHandler;
import foundation.input.InputType;
import level.Level;
import loader.AssetManager;
import loader.ResourceLocation;
import render.Renderer;
import render.renderables.RenderBackground;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;

public class MainPanel extends JFrame implements KeyListener {
    //path to the main level file
    public static final ResourceLocation LEVEL_PATH = new ResourceLocation("level.json");
    public static AffineTransform gameTransform = new AffineTransform();
    public static final Renderer GAME_RENDERER = new Renderer(gameTransform);

    public static ObjPos DEVICE_WINDOW_SIZE; //the physical screen size, in pixels
    public static ObjPos RENDER_WINDOW_SIZE; //the size of the render box, in pixels
    public static ObjPos BLOCK_DIMENSIONS; //the size of the render box, in blocks

    public static Level level = new Level(300);

    public void init() {
        AssetManager.readBlocks(LEVEL_PATH);
        AssetManager.createAllLevelSections(LEVEL_PATH);
        //level.addBlocks(AssetManager.blocks.get("player").apply(new ObjPos(1, 1)));
        /*player = new Player(new ObjPos(4, 2), level.inputHandler).init();
        level.addBlocks(player);
        BlockLike blue = new PhysicsBlock(new ObjPos(2.5, 7)).init();
        BlockLike blue2 = new PhysicsBlock(new ObjPos(7, 3)).init();
        BlockLike blue3 = new PhysicsBlock(new ObjPos(7, 5)).init();
        level.addBlocks(blue, blue2, blue3);
        BlockLike red = new MovableBlock(new ObjPos(4, 5)).init();
        BlockLike red2 = new MovableBlock(new ObjPos(5, 5)).init();
        BlockLike red3 = new MovableBlock(new ObjPos(4, 6)).init();
        level.addBlocks(red, red2, red3);
        level.addBlocks(new MovableBlock(new ObjPos(2, 3)).init());*/
        GAME_RENDERER.register(new RenderBackground(Color.WHITE));
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
}
