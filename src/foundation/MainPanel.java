package foundation;

import render.RenderOrder;
import render.Renderer;
import render.renderables.RenderBackground;
import render.renderables.RenderSquare;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class MainPanel extends JFrame {
    public static AffineTransform gameTransform = new AffineTransform();
    public static final Renderer GAME_RENDERER = new Renderer(gameTransform);
    public void init() {
        GAME_RENDERER.register(new RenderSquare(RenderOrder.NONE, Color.RED, 20, new ObjPos(100, 100)));
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
}
