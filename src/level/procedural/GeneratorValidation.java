package level.procedural;

import level.procedural.marker.LayoutMarker;

@FunctionalInterface
public interface GeneratorValidation {
    boolean validate(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type, LayoutMarker otherLM);
}
