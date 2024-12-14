package level.procedural;

import foundation.Deletable;
import foundation.math.ObjPos;
import level.procedural.generator.BoundType;
import level.procedural.marker.LayoutMarker;
import physics.HitBox;
import physics.StaticHitBox;
import render.Renderable;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class JumpSimGroup implements Deletable, Renderable {
    public final ConcurrentHashMap<JumpSimulation, JumpSimGroup> jumps = new ConcurrentHashMap<>();
    private final HashSet<StaticHitBox> validationBounds = new HashSet<>();
    public final ArrayList<LayoutMarker> movementMarkers = new ArrayList<>();
    public final HashSet<Renderable> debugRenderBounds = new HashSet<>();
    public boolean root = false;
    public final ObjPos pos;

    public JumpSimGroup(ObjPos pos) {
        this.pos = pos;
    }

    public void setRoot() {
        root = true;
    }

    public boolean isValidated(HitBox box) {
        for (StaticHitBox bound : validationBounds) {
            if (bound.isColliding(box))
                return true;
        }
        return false;
    }

    public void addBound(StaticHitBox bound) {
        validationBounds.add(bound);
        if (Layout.DEBUG_RENDER && Layout.DEBUG_RENDER_VALIDATION_BOUNDS) {
            RenderGameSquare square = new RenderGameSquare(BoundType.JUMP_VALIDATION.debugColor, bound);
            debugRenderBounds.add(square);
        }
    }

    @Override
    public void delete() {
        jumps.clear();
        movementMarkers.forEach(LayoutMarker::delete);
        movementMarkers.clear();
    }

    @Override
    public void render(Graphics2D g) {
        debugRenderBounds.forEach(box -> box.render(g));
    }
}
