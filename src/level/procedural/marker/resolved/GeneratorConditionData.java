package level.procedural.marker.resolved;

import level.Level;
import level.procedural.RegionType;
import level.procedural.marker.LayoutMarker;

public class GeneratorConditionData {
    public final RegionType region;
    public final LMTResolvedElement type;
    public final LayoutMarker marker;
    public final Level l;

    public GeneratorConditionData(RegionType region, LMTResolvedElement type, LayoutMarker marker, Level l) {
        this.region = region;
        this.type = type;
        this.marker = marker;
        this.l = l;
    }
}
