package render.texture;

import foundation.tick.Tickable;
import loader.*;
import render.RenderEvent;
import render.RenderEventListener;
import render.Renderable;

import java.awt.*;
import java.util.HashMap;

public class EventSwitcherTexture implements Renderable, Tickable, RenderEventListener {
    private final HashMap<RenderEvent, Renderable> textures = new HashMap<>();
    private Renderable activeTexture = null;
    private Tickable activeTextureAsTickable = null;

    private EventSwitcherTexture() {
    }

    private void add(RenderEvent e, Renderable r) {
        if (activeTexture == null) {
            activeTexture = r;
            if (activeTexture instanceof Tickable t)
                activeTextureAsTickable = t;
        }
        textures.put(e, r);
    }

    @Override
    public void tick(float deltaTime) {
        if (activeTextureAsTickable != null)
            activeTextureAsTickable.tick(deltaTime);
    }

    @Override
    public void onEvent(RenderEvent event) {
        if (textures.containsKey(event)) {
            activeTexture = textures.get(event);
            if (activeTexture instanceof Tickable t)
                activeTextureAsTickable = t;
        }
        if (activeTexture instanceof RenderEventListener l) {
            l.onEvent(event);
            l.onEvent(RenderEvent.ON_SWITCH_TO);
        }
    }

    @Override
    public void render(Graphics2D g) {
        activeTexture.render(g);
    }

    public static EventSwitcherTexture getEventSwitcherTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        EventSwitcherTexture texture = new EventSwitcherTexture();

        renderables.forEach(o -> {
            System.out.println(o);
            texture.add(RenderEvent.getRenderEvent(o.get("event", JsonType.STRING_JSON_TYPE)), AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        return texture;
    }
}
