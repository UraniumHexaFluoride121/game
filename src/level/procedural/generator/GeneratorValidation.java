package level.procedural.generator;

import level.procedural.marker.LayoutMarker;

@FunctionalInterface
public interface GeneratorValidation {
    boolean validate(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type, LayoutMarker otherLM, ValidationData data);

    default GeneratorValidation and(GeneratorValidation other) {
        return (gen, lm, type, otherLM, data) -> validate(gen, lm, type, otherLM, data) && other.validate(gen, lm, type, otherLM, data);
    }
}
