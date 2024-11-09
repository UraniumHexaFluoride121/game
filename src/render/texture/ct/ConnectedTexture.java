package render.texture.ct;

import foundation.MainPanel;
import foundation.tick.Tickable;
import level.Level;
import level.objects.BlockLike;
import loader.*;
import render.Renderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class ConnectedTexture implements Renderable, Tickable, RenderEventListener {
    private final Vector<CTElement> textures = new Vector<>();
    private final Vector<Renderable> activeTextures = new Vector<>();
    private final Vector<Tickable> activeTickables = new Vector<>();

    public void addTexture(CTElement e) {
        textures.add(e);
    }

    @Override
    public void tick(float deltaTime) {
        activeTickables.forEach(t -> t.tick(deltaTime));
    }

    @Override
    public void render(Graphics2D g) {
        activeTextures.forEach(r -> r.render(g));
    }

    @Override
    public void onEvent(RenderEvent event) {
        if (event instanceof RenderBlockUpdate u) {
            update(u.block);
        }
        activeTextures.forEach(r -> {
            if (r instanceof RenderEventListener l)
                l.onEvent(event);
        });
    }

    private void update(BlockLike parent) {
        if (!parent.getLayer().addToStatic)
            throw new IllegalArgumentException("Connected texture was applied to a \"" + parent.name + "\" block which was on the non-static ObjectLayer \"" + parent.getLayer().toString() + "\"");
        activeTextures.clear();
        activeTickables.clear();
        textures.forEach((element) -> {
            if (element.condition.test(parent, MainPanel.level)) {
                activeTextures.add(element.renderable);
                if (element.renderable instanceof Tickable t)
                    activeTickables.add(t);
            }
        });
    }

    public static Supplier<ConnectedTexture> getConnectedTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);

        ArrayList<CTElementSupplier> elements = new ArrayList<>();
        renderables.forEach(o -> {
            elements.add(new CTElementSupplier(AssetManager.deserializeRenderable(o), o.getOrDefault("condition", "true", JsonType.STRING_JSON_TYPE)));
        }, JsonType.JSON_OBJECT_TYPE);

        return () -> {
            ConnectedTexture texture = new ConnectedTexture();
            for (CTElementSupplier element : elements) {
                texture.addTexture(element.get());
            }
            return texture;
        };
    }

    private static class CTElementSupplier {
        public final BiPredicate<BlockLike, Level> condition;
        public final Supplier<? extends Renderable> renderable;

        private CTElementSupplier(Supplier<? extends Renderable> renderable, String string) {
            this.condition = (b, l) -> (boolean) CTExpression.parser.parseExpression(string).apply(new CTExpressionData(b, l));
            this.renderable = renderable;
        }

        public CTElement get() {
            return new CTElement(condition, renderable.get());
        }
    }
}
