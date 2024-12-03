package level.procedural.marker.resolved;

import level.procedural.JumpSimulation;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.LMData;
import level.procedural.marker.LayoutMarker;
import render.Renderable;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class LMDResolvedElement extends LMData implements Renderable {
    public ProceduralGenerator gen;
    public GeneratorType genType;
    //A map with all the jumps to this LM, mapped to the data of the LM we jumped from
    public final ConcurrentHashMap<JumpSimulation, LMDResolvedElement> jumps = new ConcurrentHashMap<>();
    public boolean root = false;

    public LMDResolvedElement(LayoutMarker lm, ProceduralGenerator gen, GeneratorType genType) {
        super(lm);
        this.gen = gen;
        this.genType = genType;
    }

    public void setRoot() {
        root = true;
    }

    @Override
    public void delete() {
        gen = null;
        genType = null;
        jumps.clear();
    }

    public void render(Graphics2D g) {
        jumps.forEach((j, to) -> j.render(g));
    }
}
