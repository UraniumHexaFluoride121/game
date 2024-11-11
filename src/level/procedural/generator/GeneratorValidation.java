package level.procedural.generator;

import foundation.MainPanel;
import level.procedural.marker.LayoutMarker;

import java.util.concurrent.atomic.AtomicBoolean;

@FunctionalInterface
public interface GeneratorValidation {
    boolean validate(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type, LayoutMarker otherLM, ValidationData data);

    default GeneratorValidation and(GeneratorValidation other) {
        return (gen, lm, type, otherLM, data) -> validate(gen, lm, type, otherLM, data) && other.validate(gen, lm, type, otherLM, data);
    }

    static boolean validate(LayoutMarker marker, GeneratorType type, GeneratorValidation validation) {
        ValidationData data = new ValidationData();
        AtomicBoolean validated = new AtomicBoolean(true);
        MainPanel.level.layout.forEachMarker(marker.pos.y, 1, lm -> {
            if (!validation.validate(lm.gen, marker, type, lm, data))
                validated.set(false);
        });
        if (validated.get())
            System.out.println(marker.pos.y);
        return validated.get();
    }
}
