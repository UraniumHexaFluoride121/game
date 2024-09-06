package render.texture;

import foundation.tick.Tickable;
import loader.*;
import render.RenderEvent;
import render.RenderEventListener;
import render.Renderable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class AnimatedTexture implements Renderable, Tickable, RenderEventListener {
    private final ArrayList<Renderable> elements = new ArrayList<>();
    private final ArrayList<Renderable> initial = new ArrayList<>();
    private final HashSet<RenderEvent> startInitialEvents = new HashSet<>();

    private int index = 0;
    private float currentTime = 0;
    private final float frameDuration;
    private final boolean pickRandomFrame;
    private boolean isOnInitial;

    private AnimatedTexture(boolean pickRandomFrame, float frameDuration) {
        this.pickRandomFrame = pickRandomFrame;
        this.frameDuration = frameDuration;
        isOnInitial = !pickRandomFrame;
    }

    public void addRenderable(Renderable r) {
        elements.add(r);
    }

    public void addStartInitialEvent(RenderEvent r) {
        startInitialEvents.add(r);
    }

    public void addRenderableInitial(Renderable r) {
        initial.add(r);
    }

    private ArrayList<Renderable> getActiveList() {
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
        if (startInitialEvents.contains(event)) {
            if (!pickRandomFrame) {
                index = 0;
                isOnInitial = true;
                currentTime = 0;
            }
        }
        if (getActiveList().get(index) instanceof RenderEventListener listener)
            listener.onEvent(event);
    }

    public static AnimatedTexture getAnimatedTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        JsonArray initial = obj.getOrDefault("initial", null, JsonType.JSON_ARRAY_TYPE);
        JsonArray startInitial = obj.getOrDefault("startInitialEvents", null, JsonType.JSON_ARRAY_TYPE);

        AnimatedTexture texture = new AnimatedTexture(
                obj.getOrDefault("pickRandomFrame", false, JsonType.BOOLEAN_JSON_TYPE),
                obj.get("frameDuration", JsonType.FLOAT_JSON_TYPE));

        renderables.forEach(o -> {
            texture.addRenderable(AssetManager.deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);

        if (initial != null) {
            initial.forEach(o -> {
                texture.addRenderableInitial(AssetManager.deserializeRenderable(o));
            }, JsonType.JSON_OBJECT_TYPE);
        }

        if (startInitial != null) {
            startInitial.forEach(event -> {
                texture.addStartInitialEvent(AssetManager.deserializeRenderEvent(event));
            }, JsonType.STRING_JSON_TYPE);
        }

        return texture;
    }
}
