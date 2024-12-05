package render;

import foundation.MainPanel;
import foundation.math.RandomType;
import level.objects.StaticBlock;
import render.ui.UIRenderable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static level.Level.*;

public class GameRenderer implements Renderable {
    public final AffineTransform transform;
    private final Supplier<AffineTransform> cameraTransform;
    private final Set<BoundedRenderable>
            qRegister = ConcurrentHashMap.newKeySet(),
            qRemove = ConcurrentHashMap.newKeySet(),
            nonStatics = ConcurrentHashMap.newKeySet();
    private final Set<UIRenderable>
            qRegisterUI = ConcurrentHashMap.newKeySet(),
            qRemoveUI = ConcurrentHashMap.newKeySet();
    private Set<BoundedRenderable>[] statics;
    private TreeMap<RenderOrder, TreeMap<Integer, HashSet<BoundedRenderable>>> renderables = new TreeMap<>();
    private TreeMap<Integer, HashSet<UIRenderable>> uiElements = new TreeMap<>();

    public GameRenderer(AffineTransform transform, Supplier<AffineTransform> cameraTransform) {
        this.transform = transform;
        this.cameraTransform = cameraTransform;
    }

    public void createStaticsSet(int sectionCount) {
        statics = new Set[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            statics[i] = ConcurrentHashMap.newKeySet();
        }
    }

    public synchronized void registerUI(UIRenderable r) {
        qRegisterUI.add(r);
    }

    public synchronized void removeUI(UIRenderable r) {
        qRemoveUI.add(r);
    }

    public synchronized void register(BoundedRenderable r) {
        qRegister.add(r);
    }

    public synchronized void remove(BoundedRenderable r) {
        qRemove.add(r);
    }

    private synchronized void processQueued() {
        qRegister.forEach(r -> {
            if (r instanceof StaticBlock) {
                statics[yPosToSection(r.getTopRenderBound())].add(r);
                statics[yPosToSection(r.getBottomRenderBound())].add(r);
            } else
                nonStatics.add(r);
        });
        qRegister.clear();

        qRemove.forEach(r -> {
            if (r instanceof StaticBlock) {
                statics[yPosToSection(r.getTopRenderBound())].remove(r);
                statics[yPosToSection(r.getBottomRenderBound())].remove(r);
            } else
                nonStatics.remove(r);
        });
        qRemove.clear();


        for (UIRenderable r : qRegisterUI) {
            if (!uiElements.containsKey(r.getZOrder()))
                uiElements.put(r.getZOrder(), new HashSet<>());
            uiElements.get(r.getZOrder()).add(r);
        }
        qRegisterUI.clear();

        for (UIRenderable r : qRemoveUI) {
            if (!uiElements.containsKey(r.getZOrder()))
                continue;
            uiElements.get(r.getZOrder()).remove(r);
        }
        qRemoveUI.clear();
    }

    public int prevTop = -1, prevBottom = -1;

    @Override
    public synchronized void render(Graphics2D g) {
        float bottom = -MainPanel.cameraY;
        float top = -MainPanel.cameraY + MainPanel.BLOCK_DIMENSIONS.y;
        int bottomSection = yPosToSection(bottom);
        int topSection = yPosToSection(top);

        boolean rebuild = !qRegister.isEmpty() || !qRemove.isEmpty() || prevBottom != bottomSection || prevTop != topSection;
        processQueued();
        if (rebuild) {
            rebuildRenderMap();
            prevBottom = bottomSection;
            prevTop = topSection;
        }

        AffineTransform prev = g.getTransform();
        g.transform(transform);
        g.transform(cameraTransform.get());
        renderables.forEach((order, zSet) -> zSet.forEach((z, set) -> {
            set.forEach(r -> {
                if (bottom < r.getTopRenderBound() && top > r.getBottomRenderBound())
                    r.render(g);
            });
        }));
        uiElements.forEach((z, set) -> set.forEach(r -> r.render(g)));
        g.setTransform(prev);
    }

    public synchronized void rebuildRenderMap() {
        int bottom = MainPanel.level.yPosToSection(-MainPanel.cameraY);
        int top = MainPanel.level.yPosToSection(-MainPanel.cameraY + MainPanel.BLOCK_DIMENSIONS.y);
        renderables = new TreeMap<>();
        for (RenderOrder value : RenderOrder.values()) {
            renderables.put(value, new TreeMap<>());
        }
        for (BoundedRenderable r : nonStatics) {
            if (!renderables.get(r.getRenderOrder()).containsKey(r.getZOrder()))
                renderables.get(r.getRenderOrder()).put(r.getZOrder(), new HashSet<>());
            renderables.get(r.getRenderOrder()).get(r.getZOrder()).add(r);
        }
        for (int i = bottom; i <= top; i++) {
            for (BoundedRenderable r : statics[i]) {
                if (!renderables.get(r.getRenderOrder()).containsKey(r.getZOrder()))
                    renderables.get(r.getRenderOrder()).put(r.getZOrder(), new HashSet<>());
                renderables.get(r.getRenderOrder()).get(r.getZOrder()).add(r);
            }
        }
    }

    private Random zOrderSource;

    public synchronized int getNextZOrder() {
        if (zOrderSource == null)
            zOrderSource = MainPanel.level.randomHandler.getRandom(RandomType.Z_ORDER);
        return zOrderSource.nextInt();
    }

    public int yPosToSection(float y) {
        return ((int) (y / SECTION_SIZE));
    }
}
