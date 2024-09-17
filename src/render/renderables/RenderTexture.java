package render.renderables;

import foundation.ObjPos;
import foundation.tick.Tickable;
import render.event.RenderEvent;
import render.event.RenderEventListener;
import render.RenderOrder;
import render.Renderable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.function.Supplier;

public class RenderTexture extends RenderGameElement implements RenderEventListener {
    private Renderable texture;
    private Tickable textureAsTickable;
    private RenderEventListener textureAsEventListener;

    public RenderTexture(RenderOrder renderOrder, Supplier<ObjPos> gamePos, Renderable texture) {
        super(renderOrder, gamePos);
        this.texture = texture;
        if (texture instanceof Tickable tickable)
            textureAsTickable = tickable;
        if (texture instanceof RenderEventListener eventListener)
            textureAsEventListener = eventListener;
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
        if (textureAsTickable != null)
            textureAsTickable.tick(deltaTime);
    }

    @Override
    public void onEvent(RenderEvent event) {
        if (textureAsEventListener != null)
            textureAsEventListener.onEvent(event);
    }

    @Override
    public void delete() {
        super.delete();
        texture = null;
        textureAsTickable = null;
        textureAsEventListener = null;
    }
}
