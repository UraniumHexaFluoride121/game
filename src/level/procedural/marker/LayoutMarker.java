package level.procedural.marker;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.ObjPos;
import level.procedural.Layout;
import level.procedural.RegionType;
import level.procedural.generator.BoundType;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.GeneratorValidation;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.movement.LMDPlayerMovement;
import level.procedural.marker.movement.LMTPlayerMovement;
import level.procedural.marker.resolved.GeneratorConditionData;
import level.procedural.marker.resolved.LMDResolvedElement;
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

    public LMData data = new LMData();

    public LayoutMarker(LMType type, ObjPos pos) {
        this.type = type;
        this.pos = pos;
        if (type instanceof LMTPlayerMovement) {
            data = new LMDPlayerMovement();
        }
        if (Layout.DEBUG_RENDER)
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
            GeneratorType genType = t.getGenerator(new GeneratorConditionData(
                    MainPanel.level.getRegion(pos), t, this, MainPanel.level
            ));
            ProceduralGenerator gen = genType.generator.get();
            data = new LMDResolvedElement(gen, genType);
            gen.generate(this, genType);
        }
    }

    public void generateMarkers() {
        data.generateMarkers(this);
    }

    public void addBound(HitBox bound, BoundType type) {
        if (!bounds.containsKey(type))
            bounds.put(type, new HashSet<>());
        bounds.get(type).add(bound);
        if (Layout.DEBUG_RENDER) {
            if (!boundsDebugRenderer.containsKey(type))
                boundsDebugRenderer.put(type, new HashSet<>());
            RenderGameSquare square = new RenderGameSquare(type.debugColor, bound);
            square.setFrame();
            boundsDebugRenderer.get(type).add(square);
        }
    }

    public static GeneratorValidation isNotColliding(BoundType boundType) {
        if (boundType.collisionsAllowed == 0) {
            return (lm, otherLM, data) -> {
                HashSet<HitBox> hitBoxes = lm.bounds.get(boundType);
                if (hitBoxes == null || lm == otherLM)
                    return true;
                HashSet<HitBox> otherHitBoxes = otherLM.bounds.get(boundType);
                if (otherHitBoxes == null)
                    return true;
                for (HitBox box : hitBoxes) {
                    for (HitBox otherBox : otherHitBoxes) {
                        if (box.isColliding(otherBox))
                            return false;
                    }
                }
                return true;
            };
        } else {
            return (lm, otherLM, data) -> {
                HashSet<HitBox> hitBoxes = lm.bounds.get(boundType);
                if (hitBoxes == null || lm == otherLM)
                    return true;
                HashSet<HitBox> otherHitBoxes = otherLM.bounds.get(boundType);
                if (otherHitBoxes == null)
                    return true;
                for (HitBox box : hitBoxes) {
                    for (HitBox otherBox : otherHitBoxes) {
                        if (box.isColliding(otherBox)) {
                            data.addCollision(boundType);
                            return data.isUnderAllowed(boundType);
                        }
                    }
                }
                return true;
            };
        }
    }

    public static GeneratorValidation isNotColliding(HitBox box, BoundType boundType) {
        if (boundType.collisionsAllowed == 0) {
            return (lm, otherLM, data) -> {
                if (lm == otherLM)
                    return true;
                HashSet<HitBox> otherHitBoxes = otherLM.bounds.get(boundType);
                if (otherHitBoxes == null)
                    return true;
                for (HitBox otherBox : otherHitBoxes) {
                    if (box.isColliding(otherBox))
                        return false;
                }
                return true;
            };
        } else {
            return (lm, otherLM, data) -> {
                if (lm == otherLM)
                    return true;
                HashSet<HitBox> otherHitBoxes = otherLM.bounds.get(boundType);
                if (otherHitBoxes == null)
                    return true;
                for (HitBox otherBox : otherHitBoxes) {
                    if (box.isColliding(otherBox)) {
                        data.addCollision(boundType);
                        return data.isUnderAllowed(boundType);
                    }
                }
                return true;
            };
        }
    }

    public boolean isBoxColliding(HitBox box, BoundType type) {
        if (!hasBoundType(type))
            return false;
        for (HitBox hitBox : bounds.get(type)) {
            if (hitBox.isColliding(box))
                return true;
        }
        return false;
    }

    public boolean hasBoundType(BoundType type) {
        return bounds.containsKey(type);
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
        if (data instanceof LMDResolvedElement rData) {
            rData.render(g);
        }
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.DEBUG;
    }

    @Override
    public float getTopRenderBound() {
        return pos.y + 20;
    }

    @Override
    public float getBottomRenderBound() {
        return pos.y - 20;
    }

    @Override
    public void delete() {
        MainPanel.level.layout.removeMarker(this);
        if (Layout.DEBUG_RENDER)
            MainPanel.GAME_RENDERER.remove(this);
    }

    private void register() {
        MainPanel.level.layout.addMarker(this);
        generate();
    }
}
