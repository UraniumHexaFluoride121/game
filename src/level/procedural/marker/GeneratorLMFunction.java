package level.procedural.marker;

import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;

public interface GeneratorLMFunction {
    void generateMarkers(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type);

    default GeneratorLMFunction andThen(GeneratorLMFunction other) {
        return ((gen, lm, type) -> {
            generateMarkers(gen, lm, type);
            other.generateMarkers(gen, lm, type);
        });
    }
}
