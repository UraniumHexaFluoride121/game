package level.procedural.marker.resolved;

import level.procedural.JumpSimulation;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.LMData;
import level.procedural.marker.LayoutMarker;
import render.Renderable;

import java.awt.*;
import java.util.HashSet;

public class LMDResolvedElement extends LMData implements Renderable {
    public ProceduralGenerator gen;
    public GeneratorType genType;
    //A set with all the jumps from this LM
    public final HashSet<JumpSimulation> jumps = new HashSet<>();
    public boolean root = false;

    public LMDResolvedElement(ProceduralGenerator gen, GeneratorType genType) {
        this.gen = gen;
        this.genType = genType;
    }

    public void setRoot() {
        root = true;
    }

    @Override
    public void generateMarkers(LayoutMarker lm) {
        if (gen != null)
            gen.generateMarkers(lm, genType);
    }

    @Override
    public void delete() {
        gen = null;
        genType = null;
        jumps.clear();
    }

    public void render(Graphics2D g) {
        jumps.forEach(j -> j.render(g));
    }
}
