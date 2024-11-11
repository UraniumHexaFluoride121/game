package level.procedural.marker;

import level.procedural.marker.resolved.LMTResolvedElement;
import level.procedural.marker.unresolved.LMTUnresolvedElement;
import loader.JsonObject;
import render.Renderable;

import java.util.HashSet;
import java.util.List;

public abstract class LMType {
    public static final HashSet<LMType> values = new HashSet(List.of(
            LMTDebugElement.DEBUG_ELEMENT,
            LMTUnresolvedElement.PLATFORM,
            LMTResolvedElement.ISLAND_FLOATING, LMTResolvedElement.ISLAND_SIDE_ATTACHED
    ));

    public final Renderable debugRenderable;
    public final String s;

    protected LMType(String s, Renderable debugRenderable) {
        this.s = s;
        this.debugRenderable = debugRenderable;
    }

    public static LMType getLayoutMarker(String s) {
        for (LMType type : LMType.values) {
            if (type.s.equals(s))
                return type;
        }
        throw new IllegalArgumentException("Unknown layout marker type: " + s);
    }

    public void parseDataFromJson(JsonObject data) {

    }

    public boolean hasData() {
        return true;
    }

    @Override
    public String toString() {
        return s;
    }
}
