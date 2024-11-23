package level.procedural.marker.resolved;

import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.LMData;
import level.procedural.marker.LayoutMarker;

public class LMDResolvedElement extends LMData {
    public ProceduralGenerator gen;
    public GeneratorType genType;

    public LMDResolvedElement(ProceduralGenerator gen, GeneratorType genType) {
        this.gen = gen;
        this.genType = genType;
    }

    @Override
    public void generateMarkers(LayoutMarker lm) {
        if (gen != null)
            gen.generateMarkers(lm, genType);
    }

    @Override
    public void delete() {
        gen = null;
        genType = null;
    }
}
