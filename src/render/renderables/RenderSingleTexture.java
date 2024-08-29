package render.renderables;

import foundation.ObjPos;
import render.AnimatedTexture;
import render.RenderOrder;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.function.Supplier;

public class RenderSingleTexture extends RenderGameElement {
    private final AnimatedTexture texture;

    public RenderSingleTexture(RenderOrder renderOrder, Supplier<ObjPos> gamePos, AnimatedTexture texture) {
        super(renderOrder, gamePos);
        this.texture = texture;
    }

    @Override
    public void render(Graphics2D g) {
        AffineTransform prev = g.getTransform(), t = new AffineTransform();
        t.translate(gamePos.get().x, gamePos.get().y);
        g.transform(t);
        texture.render(g);

        g.setTransform(prev);
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        texture.tick(deltaTime);
    }
}
