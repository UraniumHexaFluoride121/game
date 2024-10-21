package render;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.function.Supplier;

public class GameRenderer implements Renderable {
    public final AffineTransform transform;
    private final Supplier<AffineTransform> cameraTransform;
    private final HashSet<OrderedRenderable> qRegister = new HashSet<>(), qRemove = new HashSet<>();
    private final TreeMap<RenderOrder, HashSet<OrderedRenderable>> renderables = new TreeMap<>();

    public GameRenderer(AffineTransform transform, Supplier<AffineTransform> cameraTransform) {
        this.transform = transform;
        this.cameraTransform = cameraTransform;
        for (RenderOrder value : RenderOrder.values()) {
            renderables.put(value, new HashSet<>());
        }
    }

    public void register(OrderedRenderable r) {
        qRegister.add(r);
    }

    public void remove(OrderedRenderable r) {
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
        processQueued();
        AffineTransform prev = g.getTransform();
        g.transform(transform);
        g.transform(cameraTransform.get());
        renderables.forEach((order, set) -> set.forEach(r -> r.render(g)));
        g.setTransform(prev);
    }
}
