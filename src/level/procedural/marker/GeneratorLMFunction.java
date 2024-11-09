package level.procedural.marker;

import level.procedural.GeneratorType;
import level.procedural.ProceduralGenerator;

public interface GeneratorLMFunction {
    void generateMarkers(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type);
}
