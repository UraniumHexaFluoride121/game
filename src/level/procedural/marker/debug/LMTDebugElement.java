package level.procedural.marker.debug;

import foundation.math.ObjPos;
import level.procedural.marker.LMType;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameCircle;

import java.awt.*;

//A debug marker to mark out positions from the procedural generation visually
public class LMTDebugElement extends LMType {
    public static final LMTDebugElement DEBUG_ELEMENT = new LMTDebugElement(
            "debug",
            new RenderGameCircle(RenderOrder.DEBUG, new Color(118, 249, 255), 0.5f, ObjPos::new)
    );

    protected LMTDebugElement(String s, Renderable debugRenderable) {
        super(s, debugRenderable);
    }

    @Override
    public boolean hasData() {
        return false;
    }
}
