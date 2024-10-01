package render.texture.ct;

import foundation.MainPanel;
import foundation.tick.Tickable;
import level.objects.BlockLike;
import loader.*;
import render.Renderable;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.event.RenderEventListener;

import java.awt.*;
import java.util.ArrayList;

public class ConnectedTexture implements Renderable, Tickable, RenderEventListener {
    private final ArrayList<CTElement> textures = new ArrayList<>();
    private final ArrayList<Renderable> activeTextures = new ArrayList<>();
    private final ArrayList<Tickable> activeTickables = new ArrayList<>();

    public void addTexture(Renderable r, String condition) {
        textures.add(new CTElement((b, l) -> (boolean) CTExpression.parser.parseExpression(condition).apply(new CTExpressionData(b, l)), r));
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

    public static ConnectedTexture getConnectedTextures(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);

        ConnectedTexture texture = new ConnectedTexture();

        renderables.forEach(o -> {
            texture.addTexture(AssetManager.deserializeRenderable(o), o.getOrDefault("condition", "true", JsonType.STRING_JSON_TYPE));
        }, JsonType.JSON_OBJECT_TYPE);

        return texture;
    }
}
