package render.texture;

import foundation.tick.Tickable;
import loader.*;
import render.RenderEvent;
import render.RenderEventListener;
import render.Renderable;

import java.awt.*;
import java.util.ArrayList;

public class LayeredTexture implements Renderable, Tickable, RenderEventListener {
    private final ArrayList<Renderable> textures = new ArrayList<>();
    private final ArrayList<Tickable> tickableTextures = new ArrayList<>();
    private final ArrayList<RenderEventListener> renderEventTextures = new ArrayList<>();

    private LayeredTexture() {
    }

    public void add(Renderable r) {
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

    public static LayeredTexture getLayeredTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        LayeredTexture texture = new LayeredTexture();

        renderables.forEach(o -> {
            texture.add(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        return texture;
    }
}
