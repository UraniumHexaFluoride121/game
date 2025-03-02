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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EventSwitcherTexture implements TickedRenderable, Tickable, RenderEventListener {
    private final HashMap<RenderEvent, TickedRenderable> textures = new HashMap<>();
    private TickedRenderable activeTexture = null;
    private Tickable activeTextureAsTickable = null;

    private EventSwitcherTexture() {
    }

    private synchronized void add(RenderEvent e, TickedRenderable r) {
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
    public synchronized void onEvent(RenderEvent event) {
        if (textures.containsKey(event)) {
            activeTexture = textures.get(event);
            if (activeTexture instanceof Tickable t)
                activeTextureAsTickable = t;
            else
                activeTextureAsTickable = null;
        }
        textures.forEach((e, t) -> {
            if (t instanceof RenderEventListener l) {
                l.onEvent(event);
            }
        });
        if (event instanceof RenderBlockUpdate u && u.type == RenderEvent.PLAYER_COLOUR_UPDATE) {
            textures.replaceAll((e, r) -> {
                if (r instanceof TextureAsset t) {
                    TextureAsset newAsset = t.colourModified(((Player) u.block).colour);
                    if (r == activeTexture) {
                        activeTexture = newAsset;
                    }
                    return newAsset;
                }
                return r;
            });
        }
        if (activeTexture instanceof RenderEventListener l) {
            l.onEvent(event);
            if (textures.get(event) == activeTexture)
                l.onEvent(RenderEvent.ON_SWITCH_TO);
        }
    }

    @Override
    public void render(Graphics2D g) {
        activeTexture.render(g);
    }

    public static Function<Level, EventSwitcherTexture> getEventSwitcherTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);

        HashMap<RenderEvent, Function<Level, ? extends TickedRenderable>> textures = new HashMap<>();
        renderables.forEach(o -> {
            textures.put(RenderEvent.getRenderEvent(o.get("event", JsonType.STRING_JSON_TYPE)), AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        return level -> {
            EventSwitcherTexture texture = new EventSwitcherTexture();
            for (Map.Entry<RenderEvent, Function<Level, ? extends TickedRenderable>> entry : textures.entrySet()) {
                texture.add(entry.getKey(), entry.getValue().apply(level));
            }
            return texture;
        };
    }

    @Override
    public boolean requiresTick() {
        for (TickedRenderable r : textures.values()) {
            if (r.requiresTick())
                return true;
        }
        return false;
    }
}
