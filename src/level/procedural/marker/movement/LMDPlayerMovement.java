package level.procedural.marker.movement;

import level.procedural.JumpSimulation;
import level.procedural.marker.LMData;
import render.Renderable;

import java.awt.*;
import java.util.HashSet;

public class LMDPlayerMovement extends LMData implements Renderable {
    //A set with all the jumps from this LM
    public final HashSet<JumpSimulation> jumps = new HashSet<>();

    @Override
    public void delete() {
        jumps.clear();
    }

    public void render(Graphics2D g) {
        jumps.forEach(j -> j.render(g));
    }
}
