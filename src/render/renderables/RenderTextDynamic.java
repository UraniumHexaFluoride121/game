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

public class RenderTextDynamic extends RenderGameElement implements BoundedRenderable {

    private final int zOrder;
    private final float scale;
    private final TextAlign textAlign;
    private final Supplier<String> text;

    public RenderTextDynamic(RenderOrder renderOrder, Supplier<ObjPos> gamePos, Supplier<String> text, float scale, TextAlign textAlign) {
        super(renderOrder, gamePos);
        this.scale = scale;
        this.textAlign = textAlign;
        this.text = text;
        zOrder = MainPanel.GAME_RENDERER.getNextZOrder();
    }

    @Override
    public synchronized void render(Graphics2D g) {
        if (gamePos == null)
            return;
        ArrayList<GlyphData> glyphs = new ArrayList<>();
        int totalWidth = 0;
        char[] chars = text.get().toCharArray();
        boolean specialChar = false;
        StringBuilder s = new StringBuilder();
        for (char c : chars) {
            if (c == '*') {
                if (specialChar) {
                    GlyphData e = AssetManager.specialGlyphs.get(s.toString());
                    if (e == null)
                        throw new RuntimeException("Could not find special character glyph for string: \"" + s + "\"");
                    totalWidth += e.width();
                    glyphs.add(e);
                    s = new StringBuilder();
                }
                specialChar = !specialChar;
                continue;
            }
            if (specialChar) {
                s.append(c);
                continue;
            }
            GlyphData e = AssetManager.glyphs.get(c);
            if (e == null)
                throw new RuntimeException("Could not find glyph for character: \"" + c + "\"");
            totalWidth += e.width();
            glyphs.add(e);
        }
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
