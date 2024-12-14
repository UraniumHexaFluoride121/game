package level.procedural.marker.resolved;

import level.procedural.JumpSimGroup;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.LMData;
import level.procedural.marker.LayoutMarker;
import render.Renderable;

import java.awt.*;
import java.util.HashSet;
import java.util.function.Consumer;

public class LMDResolvedElement extends LMData implements Renderable {
    public ProceduralGenerator gen;
    public GeneratorType genType;
    //A set of all sim groups that this LM has
    public HashSet<JumpSimGroup> jumps = new HashSet<>();

    public LMDResolvedElement(LayoutMarker lm, ProceduralGenerator gen, GeneratorType genType) {
        super(lm);
        this.gen = gen;
        this.genType = genType;
    }

    public void setRoot() {
        for (JumpSimGroup jump : jumps) {
            jump.setRoot();
        }
    }

    public void forEachJumpSimGroup(Consumer<JumpSimGroup> action) {
        jumps.forEach(action);
    }

    @Override
    public void delete() {
        gen = null;
        genType = null;
        jumps.forEach(JumpSimGroup::delete);
    }

    public void render(Graphics2D g) {
        jumps.forEach((group) -> group.jumps.forEach((j, to) -> j.render(g)));
    }
}
