package render.texture;

import foundation.MainPanel;
import foundation.math.MathHelper;
import foundation.tick.Tickable;
import level.RandomType;
import loader.*;
import render.Renderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;

import java.awt.*;
import java.awt.geom.AffineTransform;
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

    private final boolean isRandomlyRotated;
    private final RotationType rotationType;
    private final float xPivot, yPivot;
    private AffineTransform transform = null, prevTransform = null;

    public RandomTexture(HashSet<RenderEvent> events, boolean guaranteeUnique, boolean isRandomlyRotated, RotationType rotationType, float xPivot, float yPivot) {
        this.events = events;
        this.isRandomlyRotated = isRandomlyRotated;
        this.rotationType = rotationType;
        this.xPivot = xPivot;
        this.yPivot = yPivot;
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
            while (prev == activeTexture && (transform == null || transform.equals(prevTransform))) {
                activeTexture = textures.get((int) (random.get() * textures.size()));
                createNewTransform(random);
            }
        } else {
            activeTexture = textures.get((int) (random.get() * textures.size()));
            createNewTransform(random);
        }
        if (activeTexture instanceof Tickable t) {
            tickable = t;
        } else {
            tickable = null;
        }
    }

    private void createNewTransform(Supplier<Double> random) {
        if (!isRandomlyRotated)
            return;
        prevTransform = transform;
        transform = new AffineTransform();
        transform.translate(xPivot, yPivot);
        transform.rotate(Math.toRadians(rotationType.getRotation(random)));
        transform.translate(-xPivot, -yPivot);
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
        if (isRandomlyRotated) {
            AffineTransform prevRenderTransform = g.getTransform();
            g.transform(transform);
            activeTexture.render(g);
            g.setTransform(prevRenderTransform);
        } else
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

        if (obj.containsName("rotation")) {
            JsonObject rotationObj = obj.get("rotation", JsonType.JSON_OBJECT_TYPE);
            RotationType type = RotationType.getRotationType(rotationObj.get("type", JsonType.STRING_JSON_TYPE));
            float xPivot = rotationObj.getOrDefault("xPivot", 8f, JsonType.FLOAT_JSON_TYPE) / 16;
            float yPivot = rotationObj.getOrDefault("yPivot", 8f, JsonType.FLOAT_JSON_TYPE) / 16;

            return () -> {
                RandomTexture texture = new RandomTexture(eventSet, guaranteeUnique, true, type, xPivot, yPivot);
                for (Supplier<? extends Renderable> element : elements) {
                    texture.add(element.get());
                }
                return texture;
            };
        }

        if (guaranteeUnique && elements.size() == 1)
            throw new RuntimeException("Random list with resource " + resource.toString() + " has only one item, and can therefore not guarantee unique");

        return () -> {
            RandomTexture texture = new RandomTexture(eventSet, guaranteeUnique, false, null, 0, 0);
            for (Supplier<? extends Renderable> element : elements) {
                texture.add(element.get());
            }
            return texture;
        };
    }

    private enum RotationType {
        RANDOM_90("random_90", new float[]{
                0, 90, 180, 270
        }),
        RANDOM_180("random_180", new float[]{
                0, 180
        });

        public final String s;
        public final float[] possibleRotations;

        RotationType(String s, float[] possibleRotations) {
            this.s = s;
            this.possibleRotations = possibleRotations;
        }

        public static RotationType getRotationType(String s) {
            for (RotationType order : RotationType.values()) {
                if (order.s.equals(s))
                    return order;
            }
            throw new IllegalArgumentException("Unknown rotation type: " + s);
        }

        public float getRotation(Supplier<Double> randomSource) {
            return possibleRotations[MathHelper.randIntBetween(0, possibleRotations.length - 1, randomSource)];
        }

        @Override
        public String toString() {
            return s;
        }
    }
}
