package level.procedural.marker;

import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;

public interface GeneratorLMFunction {
    void generateMarkers(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type);
}
