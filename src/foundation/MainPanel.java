package foundation;

import render.RenderOrder;
import render.Renderer;
import render.renderables.RenderBackground;
import render.renderables.RenderGameSquare;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class MainPanel extends JFrame {
    public static AffineTransform gameTransform = new AffineTransform();
    public static final Renderer GAME_RENDERER = new Renderer(gameTransform);

    public static ObjPos DEVICE_WINDOW_SIZE; //the physical screen size, in pixels
    public static ObjPos RENDER_WINDOW_SIZE; //the size of the render box, in pixels
    public static ObjPos BLOCK_DIMENSIONS; //the size of the render box, in blocks

    public void init() {
        GAME_RENDERER.register(new RenderGameSquare(RenderOrder.NONE, Color.RED, 1, () -> new ObjPos(2.5f, 2.5f)));
        GAME_RENDERER.register(new RenderBackground(Color.WHITE));
        System.out.println(BLOCK_DIMENSIONS);
    }

    @Override
    public void paintComponents(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        GAME_RENDERER.render(g2d);
    }
}
