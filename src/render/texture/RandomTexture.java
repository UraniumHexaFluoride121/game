package render.texture;

import foundation.tick.Tickable;
import loader.*;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;
import render.Renderable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class RandomTexture implements Renderable, RenderEventListener, Tickable {
    private final ArrayList<Renderable> textures = new ArrayList<>();
    private final HashSet<RenderEvent> events = new HashSet<>();
    private Renderable activeTexture = null;
    private Tickable tickable = null;
    private final boolean guaranteeUnique;

    public RandomTexture(boolean guaranteeUnique) {
        this.guaranteeUnique = guaranteeUnique;
    }

    private void add(Renderable r) {
        textures.add(r);
        switchToNewTexture();
    }

    private void registerEvent(RenderEvent e) {
        events.add(e);
    }

    private void switchToNewTexture() {
        if (guaranteeUnique) {
            Renderable prev = activeTexture;
            while (prev == activeTexture) {
                activeTexture = textures.get((int) (Math.random() * textures.size()));
            }
        } else {
            activeTexture = textures.get((int) (Math.random() * textures.size()));
        }
        if (activeTexture instanceof Tickable t) {
            tickable = t;
        } else {
            tickable = null;
        }
    }

    @Override
    public void tick(float deltaTime) {
        if (tickable != null)
            tickable.tick(deltaTime);
    }

    @Override
    public void onEvent(RenderEvent event) {
        //Randomise the initial texture
        if (event instanceof RenderBlockUpdate u && u.type == RenderEvent.ON_GAME_INIT)
            switchToNewTexture();

        //Randomise the texture upon receiving an event specified in the events list
        if (events.contains(event))
            switchToNewTexture();

        if (activeTexture instanceof RenderEventListener l)
            l.onEvent(event);
    }

    @Override
    public void render(Graphics2D g) {
        activeTexture.render(g);
    }

    public static RandomTexture getRandomTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        RandomTexture texture = new RandomTexture(obj.getOrDefault("guaranteeUnique", false, JsonType.BOOLEAN_JSON_TYPE));

        renderables.forEach(o -> {
            texture.add(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        JsonArray events = obj.get("randomiseEvents", JsonType.JSON_ARRAY_TYPE);

        events.forEach(e -> {
            texture.registerEvent(RenderEvent.getRenderEvent(e));
        }, JsonType.STRING_JSON_TYPE);

        if (texture.textures.isEmpty())
            throw new RuntimeException("Random list with resource " + resource.toString() + " has no items");

        if (texture.guaranteeUnique && texture.textures.size() == 1)
            throw new RuntimeException("Random list with resource " + resource.toString() + " has only one item, and can therefore not guarantee unique");

        return texture;
    }
}
