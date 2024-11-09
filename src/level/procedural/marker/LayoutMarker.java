package level.procedural.marker;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.ObjPos;
import level.procedural.*;
import level.procedural.marker.resolved.GeneratorConditionData;
import level.procedural.marker.resolved.LMTResolvedElement;
import level.procedural.marker.unresolved.LMTUnresolvedElement;
import level.procedural.marker.unresolved.ResolverConditionData;
import physics.HitBox;
import render.BoundedRenderable;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.HashSet;

public class LayoutMarker implements BoundedRenderable, Deletable {
    private final HashMap<BoundType, HashSet<HitBox>> bounds = new HashMap<>();
    private final HashMap<BoundType, HashSet<Renderable>> boundsDebugRenderer = new HashMap<>();
    public LMType type;
    public final ObjPos pos;

    public ProceduralGenerator gen = null;
    public GeneratorType genType = null;

    public LayoutMarker(LMType type, ObjPos pos) {
        this.type = type;
        this.pos = pos;
        if (Layout.DEBUG_LAYOUT_RENDER)
            MainPanel.GAME_RENDERER.register(this);
    }

    public LayoutMarker(String type, ObjPos pos) {
        this(LMType.getLayoutMarker(type), pos);
    }

    public void generate() {
        if (type instanceof LMTUnresolvedElement t) {
            type = t.resolve(new ResolverConditionData(
                    MainPanel.level.getRegion(pos), t, this, MainPanel.level
            ));
            generate();
        } else if (type instanceof LMTResolvedElement t) {
            genType = t.getGenerator(new GeneratorConditionData(
                    MainPanel.level.getRegion(pos), t, this, MainPanel.level
            ));
            gen = genType.generator.get();
            gen.generate(this, genType);
        }
    }

    public void generateMarkers() {
        if (gen != null)
            gen.generateMarkers(this, genType);
    }

    public void addBound(HitBox bound, BoundType type) {
        if (!bounds.containsKey(type))
            bounds.put(type, new HashSet<>());
        bounds.get(type).add(bound);
        if (Layout.DEBUG_LAYOUT_RENDER) {
            if (!boundsDebugRenderer.containsKey(type))
                boundsDebugRenderer.put(type, new HashSet<>());
            ObjPos pos = new ObjPos(bound.getLeft(), bound.getBottom());
            RenderGameSquare square = new RenderGameSquare(RenderOrder.DEBUG, type.debugColor, bound.getTop() - bound.getBottom(), 0, 0, bound.getRight() - bound.getLeft(), () -> pos);
            square.setFrame();
            boundsDebugRenderer.get(type).add(square);
        }
    }

    public static GeneratorValidation isNotColliding(BoundType boundType) {
        return (gen, lm, type1, otherLM) -> {
            HashSet<HitBox> hitBoxes = lm.bounds.get(boundType);
            if (hitBoxes == null || lm == otherLM)
                return true;
            for (HitBox box : hitBoxes) {
                HashSet<HitBox> otherHitBoxes = otherLM.bounds.get(boundType);
                if (otherHitBoxes == null)
                    return true;
                for (HitBox otherBox : otherHitBoxes) {
                    if (box.isColliding(otherBox))
                        return false;
                }
            }
            return true;
        };
    }

    public RegionType getRegion() {
        return MainPanel.level.getRegion(pos);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LayoutMarker m) {
            return m.pos.equals(pos) && m.type == type;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + type.toString() + " at pos " + pos.toString() + "}";
    }

    @Override
    public void render(Graphics2D g) {
        AffineTransform prev = g.getTransform();
        g.translate(pos.x, pos.y);
        type.debugRenderable.render(g);
        g.setTransform(prev);
        for (HashSet<Renderable> types : boundsDebugRenderer.values()) {
            for (Renderable r : types) {
                r.render(g);
            }
        }
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.DEBUG;
    }

    @Override
    public float getTopBound() {
        return pos.y + 30;
    }

    @Override
    public float getBottomBound() {
        return pos.y - 30;
    }

    @Override
    public void delete() {
        MainPanel.level.layout.removeMarker(this);
        if (Layout.DEBUG_LAYOUT_RENDER)
            MainPanel.GAME_RENDERER.remove(this);
    }

    private void register() {
        MainPanel.level.layout.addMarker(this);
        generate();
    }
}
