package render.renderables;

import foundation.math.ObjPos;
import foundation.tick.Tickable;
import level.objects.Player;
import render.RenderOrder;
import render.TickedRenderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;
import render.texture.TextureAsset;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.function.Supplier;

public class RenderTexture extends RenderGameElement implements RenderEventListener, TickedRenderable {
    private TickedRenderable texture;
    private Tickable textureAsTickable;
    private RenderEventListener textureAsEventListener;

    public RenderTexture(RenderOrder renderOrder, Supplier<ObjPos> gamePos, TickedRenderable texture) {
        super(renderOrder, gamePos);
        updateTexture(texture);
    }

    private void updateTexture(TickedRenderable texture) {
        this.texture = texture;
        if (texture instanceof Tickable tickable)
            textureAsTickable = tickable;
        if (texture instanceof RenderEventListener eventListener)
            textureAsEventListener = eventListener;
    }

    @Override
    public synchronized void render(Graphics2D g) {
        if (gamePos == null)
            return;
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
        if (event instanceof RenderBlockUpdate u && u.type == RenderEvent.PLAYER_COLOUR_UPDATE && texture instanceof TextureAsset t) {
            updateTexture(t.colourModified(((Player) u.block).colour));
        }
    }

    @Override
    public void delete() {
        super.delete();
        texture = null;
        textureAsTickable = null;
        textureAsEventListener = null;
    }

    @Override
    public boolean requiresTick() {
        return texture.requiresTick();
    }
}
