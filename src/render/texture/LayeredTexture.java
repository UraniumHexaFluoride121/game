package render.texture;

import foundation.tick.Tickable;
import level.Level;
import level.objects.Player;
import loader.*;
import render.TickedRenderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.function.Function;

public class LayeredTexture implements TickedRenderable, Tickable, RenderEventListener {
    private final Vector<TickedRenderable> textures = new Vector<>();
    private final Vector<Tickable> tickableTextures = new Vector<>();
    private final Vector<RenderEventListener> renderEventTextures = new Vector<>();

    private LayeredTexture() {
    }

    private void add(TickedRenderable r) {
        textures.add(r);
        if (r instanceof Tickable t)
            tickableTextures.add(t);
        if (r instanceof RenderEventListener l)
            renderEventTextures.add(l);
    }

    private void updateTextureCaches() {
        tickableTextures.clear();
        renderEventTextures.clear();
        textures.forEach(r -> {
            if (r instanceof Tickable t)
                tickableTextures.add(t);
            if (r instanceof RenderEventListener l)
                renderEventTextures.add(l);
        });
    }

    @Override
    public void tick(float deltaTime) {
        tickableTextures.forEach(t -> t.tick(deltaTime));
    }

    @Override
    public void onEvent(RenderEvent event) {
        renderEventTextures.forEach(l -> l.onEvent(event));
        if (event instanceof RenderBlockUpdate u && u.type == RenderEvent.PLAYER_COLOUR_UPDATE) {
            textures.replaceAll(r -> {
                if (r instanceof TextureAsset t)
                    return t.colourModified(((Player) u.block).colour);
                return r;
            });
            updateTextureCaches();
        }
    }

    @Override
    public void render(Graphics2D g) {
        textures.forEach(r -> r.render(g));
    }

    public static Function<Level, LayeredTexture> getLayeredTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);

        ArrayList<Function<Level, ? extends TickedRenderable>> textures = new ArrayList<>();
        renderables.forEach(o -> {
            textures.add(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        return level -> {
            LayeredTexture texture = new LayeredTexture();
            for (Function<Level, ? extends TickedRenderable> supplier : textures) {
                texture.add(supplier.apply(level));
            }
            return texture;
        };
    }

    @Override
    public boolean requiresTick() {
        for (TickedRenderable texture : textures) {
            if (texture.requiresTick())
                return true;
        }
        return false;
    }
}
