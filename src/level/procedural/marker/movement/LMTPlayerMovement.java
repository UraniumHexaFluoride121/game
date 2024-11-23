package level.procedural.marker.movement;

import foundation.math.ObjPos;
import level.procedural.marker.LMType;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameSquare;

import java.awt.*;

//General element type markers that have not been resolved to something more specific based on
//the surrounding blocks
public class LMTPlayerMovement extends LMType {
    public static final LMTPlayerMovement STATIC_JUMP = new LMTPlayerMovement(
            "static_jump",
            new RenderGameSquare(RenderOrder.DEBUG, Color.MAGENTA, 0.3f, ObjPos::new)
    );

    protected LMTPlayerMovement(String s, Renderable debugRenderable) {
        super(s, debugRenderable);
    }

    @Override
    public boolean hasData() {
        return false;
    }
}
