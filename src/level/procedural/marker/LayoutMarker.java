package level.procedural.marker;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.math.ObjPos;
import level.procedural.GeneratorType;
import level.procedural.Layout;
import level.procedural.ProceduralGenerator;
import level.procedural.RegionType;
import level.procedural.marker.resolved.GeneratorConditionData;
import level.procedural.marker.resolved.LMTResolvedElement;
import level.procedural.marker.unresolved.LMTUnresolvedElement;
import level.procedural.marker.unresolved.ResolverConditionData;
import render.OrderedRenderable;
import render.RenderOrder;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class LayoutMarker implements OrderedRenderable, Deletable {
    public final LMType type;
    public final ObjPos pos;

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
            LMTResolvedElement resolvedElement = t.resolve(new ResolverConditionData(
                    MainPanel.level.getRegion(pos), t, this, MainPanel.level
            ));
            new LayoutMarker(resolvedElement, pos).register();
            delete();
        } else if (type instanceof LMTResolvedElement t) {
            GeneratorType genType = t.getGenerator(new GeneratorConditionData(
                    MainPanel.level.getRegion(pos), t, this, MainPanel.level
            ));
            ProceduralGenerator gen = genType.generator.get();
            gen.generate(this, genType);
            gen.generateMarkers();
        }
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
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.DEBUG;
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
