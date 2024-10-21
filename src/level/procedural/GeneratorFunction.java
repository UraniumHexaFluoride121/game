package level.procedural;

import level.procedural.marker.LayoutMarker;

@FunctionalInterface
public interface GeneratorFunction {
    void generate(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type);
}
