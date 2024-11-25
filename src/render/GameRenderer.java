package render;

import foundation.MainPanel;
import level.RandomType;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class GameRenderer implements Renderable {
    public final AffineTransform transform;
    private final Supplier<AffineTransform> cameraTransform;
    private final Set<BoundedRenderable> qRegister = ConcurrentHashMap.newKeySet(), qRemove = ConcurrentHashMap.newKeySet();
    private final TreeMap<RenderOrder, TreeMap<Integer, HashSet<BoundedRenderable>>> renderables = new TreeMap<>();

    public GameRenderer(AffineTransform transform, Supplier<AffineTransform> cameraTransform) {
        this.transform = transform;
        this.cameraTransform = cameraTransform;
        for (RenderOrder value : RenderOrder.values()) {
            renderables.put(value, new TreeMap<>());
        }
    }

    public synchronized void register(BoundedRenderable r) {
        qRegister.add(r);
    }

    public synchronized void remove(BoundedRenderable r) {
        qRemove.add(r);
    }

    private synchronized void processQueued() {
        qRegister.forEach(r -> {
            if (!renderables.get(r.getRenderOrder()).containsKey(r.getZOrder()))
                renderables.get(r.getRenderOrder()).put(r.getZOrder(), new HashSet<>());
            renderables.get(r.getRenderOrder()).get(r.getZOrder()).add(r);
        });
        qRegister.clear();

        qRemove.forEach(r -> {
            if (!renderables.get(r.getRenderOrder()).containsKey(r.getZOrder()))
                return;
            renderables.get(r.getRenderOrder()).get(r.getZOrder()).remove(r);
        });
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
        renderables.forEach((order, zSet) -> zSet.forEach((z, set) -> {
            set.forEach(r -> {
                if (bottom < r.getTopRenderBound() && top > r.getBottomRenderBound())
                    r.render(g);
            });
        }));
        g.setTransform(prev);
    }

    private Random zOrderSource;

    public synchronized int getNextZOrder() {
        if (zOrderSource == null)
            zOrderSource = MainPanel.level.randomHandler.getRandom(RandomType.Z_ORDER);
        return zOrderSource.nextInt();
    }
}
