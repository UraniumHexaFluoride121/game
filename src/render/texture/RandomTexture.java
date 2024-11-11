package render.texture;

import foundation.MainPanel;
import foundation.tick.Tickable;
import level.RandomType;
import loader.*;
import render.Renderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;
import java.util.function.Supplier;

public class RandomTexture implements Renderable, RenderEventListener, Tickable {
    private final Vector<Renderable> textures = new Vector<>();
    private final HashSet<RenderEvent> events;
    private Renderable activeTexture = null;
    private Tickable tickable = null;
    private final boolean guaranteeUnique;
    //Used for deterministic texture randomisation. Should only be used for block updates
    private Random textureRandom;

    public RandomTexture(HashSet<RenderEvent> events, boolean guaranteeUnique) {
        this.events = events;
        textureRandom = MainPanel.level.randomHandler.generateNewRandomSource(RandomType.TEXTURE);
        this.guaranteeUnique = guaranteeUnique;
    }

    private void add(Renderable r) {
        textures.add(r);
        switchToNewTexture(textureRandom::nextDouble);
    }

    private void switchToNewTexture(Supplier<Double> random) {
        if (guaranteeUnique) {
            Renderable prev = activeTexture;
            while (prev == activeTexture) {
                activeTexture = textures.get((int) (random.get() * textures.size()));
            }
        } else {
            activeTexture = textures.get((int) (random.get() * textures.size()));
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
        if (event instanceof RenderBlockUpdate u && u.type == RenderEvent.ON_GAME_INIT) {
            switchToNewTexture(textureRandom::nextDouble);
        }

        //Randomise the texture upon receiving an event specified in the events list
        if (events.contains(event))
            switchToNewTexture(Math::random);

        if (activeTexture instanceof RenderEventListener l)
            l.onEvent(event);
    }

    @Override
    public void render(Graphics2D g) {
        activeTexture.render(g);
    }

    public static Supplier<RandomTexture> getRandomTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        Boolean guaranteeUnique = obj.getOrDefault("guaranteeUnique", false, JsonType.BOOLEAN_JSON_TYPE);

        ArrayList<Supplier<? extends Renderable>> elements = new ArrayList<>();
        renderables.forEach(o -> {
            elements.add(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        JsonArray events = obj.getOrDefault("randomiseEvents", null, JsonType.JSON_ARRAY_TYPE);

        HashSet<RenderEvent> eventSet = new HashSet<>();
        if (events != null) {
            events.forEach(e -> {
                eventSet.add(RenderEvent.getRenderEvent(e));
            }, JsonType.STRING_JSON_TYPE);
        }

        if (elements.isEmpty())
            throw new RuntimeException("Random list with resource " + resource.toString() + " has no items");

        if (guaranteeUnique && elements.size() == 1)
            throw new RuntimeException("Random list with resource " + resource.toString() + " has only one item, and can therefore not guarantee unique");

        return () -> {
            RandomTexture texture = new RandomTexture(eventSet, guaranteeUnique);
            for (Supplier<? extends Renderable> element : elements) {
                texture.add(element.get());
            }
            return texture;
        };
    }
}
