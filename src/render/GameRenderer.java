package render;

import foundation.MainPanel;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.function.Supplier;

public class GameRenderer implements Renderable {
    public final AffineTransform transform;
    private final Supplier<AffineTransform> cameraTransform;
    private final HashSet<BoundedRenderable> qRegister = new HashSet<>(), qRemove = new HashSet<>();
    private final TreeMap<RenderOrder, HashSet<BoundedRenderable>> renderables = new TreeMap<>();

    public GameRenderer(AffineTransform transform, Supplier<AffineTransform> cameraTransform) {
        this.transform = transform;
        this.cameraTransform = cameraTransform;
        for (RenderOrder value : RenderOrder.values()) {
            renderables.put(value, new HashSet<>());
        }
    }

    public void register(BoundedRenderable r) {
        qRegister.add(r);
    }

    public void remove(BoundedRenderable r) {
        qRemove.add(r);
    }

    private void processQueued() {
        qRegister.forEach(r -> renderables.get(r.getRenderOrder()).add(r));
        qRegister.clear();

        qRemove.forEach(r -> renderables.get(r.getRenderOrder()).remove(r));
        qRemove.clear();
    }


    @Override
    public void render(Graphics2D g) {
        float top = -MainPanel.cameraY + MainPanel.BLOCK_DIMENSIONS.y;
        float bottom = -MainPanel.cameraY;
        processQueued();
        AffineTransform prev = g.getTransform();
        g.transform(transform);
        g.transform(cameraTransform.get());
        renderables.forEach((order, set) -> set.forEach(r -> {
            if (bottom < r.getTopBound() && top > r.getBottomBound())
                r.render(g);
        }));
        g.setTransform(prev);
    }
}
