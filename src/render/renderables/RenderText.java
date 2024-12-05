package render.renderables;

import foundation.MainPanel;
import foundation.math.ObjPos;
import loader.AssetManager;
import loader.GlyphData;
import render.BoundedRenderable;
import render.RenderOrder;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.function.Supplier;

public class RenderText extends RenderGameElement implements BoundedRenderable {
    public static final float TEXT_SCALE_DEFAULT = 2;

    private ArrayList<GlyphData> glyphs = new ArrayList<>();
    private final int zOrder;
    private final float scale;
    private final TextAlign textAlign;

    private int totalWidth = 0;

    public RenderText(RenderOrder renderOrder, Supplier<ObjPos> gamePos, String text, float scale, TextAlign textAlign) {
        super(renderOrder, gamePos);
        this.scale = scale;
        this.textAlign = textAlign;
        zOrder = MainPanel.GAME_RENDERER.getNextZOrder();
        char[] chars = text.toCharArray();
        for (char c : chars) {
            GlyphData e = AssetManager.glyphs.get(c);
            if (e == null)
                throw new RuntimeException("Could not find glyph for character: \"" + c + "\"");
            totalWidth += e.width();
            glyphs.add(e);
        }
    }

    @Override
    public synchronized void render(Graphics2D g) {
        if (gamePos == null)
            return;
        AffineTransform prev = g.getTransform();
        g.translate(gamePos.get().x, gamePos.get().y);
        g.scale(scale, scale);
        g.translate(switch (textAlign) {
            case LEFT -> 0;
            case RIGHT -> -totalWidth;
            case CENTER -> -totalWidth / 2f;
        } / 16, 0);
        for (GlyphData glyph : glyphs) {
            glyph.asset().render(g);
            g.translate(glyph.width() / 16d, 0);
        }
        g.setTransform(prev);
    }

    @Override
    public float getTopRenderBound() {
        return gamePos.get().y + 4;
    }

    @Override
    public float getBottomRenderBound() {
        return gamePos.get().y - 4;
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }
}
