package render;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.TreeMap;

public class Renderer implements Renderable {
    public final AffineTransform transform;
    private final HashSet<Renderable> qRegister = new HashSet<>(), qRemove = new HashSet<>();
    private final TreeMap<RenderOrder, HashSet<Renderable>> renderables = new TreeMap<>();

    public Renderer() {
        this(new AffineTransform());
    }

    public Renderer(AffineTransform transform) {
        this.transform = transform;
        for (RenderOrder value : RenderOrder.values()) {
            renderables.put(value, new HashSet<>());
        }
    }

    public void register(Renderable r) {
        qRegister.add(r);
    }

    public void remove(Renderable r) {
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
        renderables.forEach((order, set) -> set.forEach(r -> r.render(g)));
        g.setTransform(prev);
    }
}
