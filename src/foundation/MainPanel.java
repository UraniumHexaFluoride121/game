package foundation;

import foundation.input.InputType;
import level.Level;
import level.objects.BlockLike;
import level.objects.DebugSquare;
import level.objects.PhysicsBlock;
import level.objects.Player;
import render.Renderer;
import render.renderables.RenderBackground;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;

public class MainPanel extends JFrame implements KeyListener {
    public static AffineTransform gameTransform = new AffineTransform();
    public static final Renderer GAME_RENDERER = new Renderer(gameTransform);

    public static ObjPos DEVICE_WINDOW_SIZE; //the physical screen size, in pixels
    public static ObjPos RENDER_WINDOW_SIZE; //the size of the render box, in pixels
    public static ObjPos BLOCK_DIMENSIONS; //the size of the render box, in blocks

    public static Level level = new Level(300);

    BlockLike player;

    public void init() {
        player = new Player(new ObjPos(4, 2), Color.YELLOW, level.inputHandler).init();
        level.addBlocks(player);
        BlockLike blue = new PhysicsBlock(new ObjPos(2, 2), Color.BLUE).init();
        level.addBlocks(blue);
        BlockLike red = new DebugSquare(new ObjPos(4, 5), Color.RED).init();
        BlockLike red2 = new DebugSquare(new ObjPos(5, 5), Color.RED).init();
        BlockLike red3 = new DebugSquare(new ObjPos(4, 6), Color.RED).init();
        level.addBlocks(red, red2, red3);
        level.addBlocks(new DebugSquare(new ObjPos(2, 3), Color.GREEN).init());
        GAME_RENDERER.register(new RenderBackground(Color.WHITE));
    }

    @Override
    public void paintComponents(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

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
