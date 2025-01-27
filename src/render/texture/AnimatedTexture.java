package render.texture;

import foundation.tick.Tickable;
import level.Level;
import level.objects.Player;
import loader.*;
import render.Renderable;
import render.TickedRenderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.Function;

public class AnimatedTexture implements TickedRenderable, Tickable, RenderEventListener {
    private final Vector<TickedRenderable> elements = new Vector<>();
    private final Vector<TickedRenderable> initial = new Vector<>();
    private final HashSet<RenderEvent> startInitialEvents;

    private int index = 0;
    private float currentTime = 0;
    private final float frameDuration;
    private final boolean pickRandomFrame;
    private boolean isOnInitial;

    private AnimatedTexture(HashSet<RenderEvent> startInitialEvents, boolean pickRandomFrame, float frameDuration) {
        this.startInitialEvents = startInitialEvents;
        this.pickRandomFrame = pickRandomFrame;
        this.frameDuration = frameDuration;
        isOnInitial = !pickRandomFrame;
    }

    public void addRenderable(TickedRenderable r) {
        elements.add(r);
    }

    public void addRenderableInitial(TickedRenderable r) {
        initial.add(r);
    }

    private Vector<TickedRenderable> getActiveList() {
        return isOnInitial && !initial.isEmpty() ? initial : elements;
    }

    @Override
    public void tick(float deltaTime) {
        currentTime += deltaTime;
        if (currentTime > frameDuration) {
            Renderable prev = getActiveList().get(index);
            currentTime -= frameDuration;
            if (pickRandomFrame) {
                index = ((int) (Math.random() * getActiveList().size()));
            } else {
                index++;
                if (index >= getActiveList().size()) {
                    index = 0;
                    isOnInitial = false;
                }
            }
            Renderable newRenderable = getActiveList().get(index);
            if (prev != newRenderable && newRenderable instanceof RenderEventListener l)
                l.onEvent(RenderEvent.ON_SWITCH_TO);
        }
        if (getActiveList().get(index) instanceof Tickable t)
            t.tick(deltaTime);
    }

    @Override
    public void render(Graphics2D g) {
        getActiveList().get(index).render(g);
    }

    @Override
    public void onEvent(RenderEvent event) {
        if (event instanceof RenderBlockUpdate u && u.type == RenderEvent.PLAYER_COLOUR_UPDATE) {
            elements.replaceAll(r -> {
                if (r instanceof TextureAsset t)
                    return t.colourModified(((Player) u.block).colour);
                return r;
            });
            initial.replaceAll(r -> {
                if (r instanceof TextureAsset t)
                    return t.colourModified(((Player) u.block).colour);
                return r;
            });
        }
        if (startInitialEvents.contains(event)) {
            if (!pickRandomFrame) {
                index = 0;
                isOnInitial = true;
                currentTime = 0;
            }
        }
        elements.forEach(t -> {
            if (t instanceof RenderEventListener listener)
                listener.onEvent(event);
        });
        initial.forEach(t -> {
            if (t instanceof RenderEventListener listener)
                listener.onEvent(event);
        });
    }

    public static Function<Level, AnimatedTexture> getAnimatedTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        JsonArray initial = obj.getOrDefault("initial", null, JsonType.JSON_ARRAY_TYPE);
        JsonArray startInitial = obj.getOrDefault("startInitialEvents", null, JsonType.JSON_ARRAY_TYPE);

        Boolean pickRandomFrame = obj.getOrDefault("pickRandomFrame", false, JsonType.BOOLEAN_JSON_TYPE);
        Float frameDuration = obj.get("frameDuration", JsonType.FLOAT_JSON_TYPE);

        ArrayList<Function<Level, ? extends TickedRenderable>> elements = new ArrayList<>();
        renderables.forEach(o -> {
            elements.add(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        ArrayList<Function<Level, ? extends TickedRenderable>> initialElements = new ArrayList<>();
        if (initial != null) {
            initial.forEach(o -> {
                initialElements.add(AssetManager.deserializeRenderable(o));
            }, JsonType.JSON_OBJECT_TYPE);
        }

        HashSet<RenderEvent> startInitialEvents = new HashSet<>();
        if (startInitial != null) {
            startInitial.forEach(event -> {
                startInitialEvents.add(RenderEvent.getRenderEvent(event));
            }, JsonType.STRING_JSON_TYPE);
        }

        return level -> {
            AnimatedTexture t = new AnimatedTexture(startInitialEvents, pickRandomFrame, frameDuration);
            for (Function<Level, ? extends TickedRenderable> element : elements) {
                t.addRenderable(element.apply(level));
            }
            for (Function<Level, ? extends TickedRenderable> initialElement : initialElements) {
                t.addRenderableInitial(initialElement.apply(level));
            }
            return t;
        };
    }

    @Override
    public boolean requiresTick() {
        return true;
    }
}
