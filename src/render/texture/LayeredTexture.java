package render.texture;

import foundation.tick.Tickable;
import loader.*;
import render.event.RenderEvent;
import render.event.RenderEventListener;
import render.Renderable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.function.Supplier;

public class LayeredTexture implements Renderable, Tickable, RenderEventListener {
    private final Vector<Renderable> textures = new Vector<>();
    private final Vector<Tickable> tickableTextures = new Vector<>();
    private final Vector<RenderEventListener> renderEventTextures = new Vector<>();

    private LayeredTexture() {
    }

    private void add(Renderable r) {
        textures.add(r);
        if (r instanceof Tickable t)
            tickableTextures.add(t);
        if (r instanceof RenderEventListener l)
            renderEventTextures.add(l);
    }

    @Override
    public void tick(float deltaTime) {
        tickableTextures.forEach(t -> t.tick(deltaTime));
    }

    @Override
    public void onEvent(RenderEvent event) {
        renderEventTextures.forEach(l -> l.onEvent(event));
    }

    @Override
    public void render(Graphics2D g) {
        textures.forEach(r -> r.render(g));
    }

    public static Supplier<LayeredTexture> getLayeredTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);

        ArrayList<Supplier<? extends Renderable>> textures = new ArrayList<>();
        renderables.forEach(o -> {
            textures.add(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        return () -> {
            LayeredTexture texture = new LayeredTexture();
            for (Supplier<? extends Renderable> supplier : textures) {
                texture.add(supplier.get());
            }
            return texture;
        };
    }
}
