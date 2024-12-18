package level.procedural.generator;

import level.Level;
import level.procedural.marker.LayoutMarker;

import java.util.concurrent.atomic.AtomicBoolean;

@FunctionalInterface
public interface GeneratorValidation {
    boolean validate(LayoutMarker lm, LayoutMarker otherLM, ValidationData data);

    default GeneratorValidation and(GeneratorValidation other) {
        return (lm, otherLM, data) -> validate(lm, otherLM, data) && other.validate(lm, otherLM, data);
    }

    static boolean validate(LayoutMarker marker, GeneratorValidation validation, Level level) {
        ValidationData data = new ValidationData();
        AtomicBoolean validated = new AtomicBoolean(true);
        level.layout.forEachMarker(marker.pos.y, 3, lm -> {
            if (!validation.validate(marker, lm, data))
                validated.set(false);
        });/*
        if (validated.get() && marker.data instanceof LMDResolvedElement d) {
            System.out.println(d.genType.s);
            System.out.println(marker.pos.y);
        }*/
        return validated.get();
    }
}
