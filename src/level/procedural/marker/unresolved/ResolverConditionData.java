package level.procedural.marker.unresolved;

import level.Level;
import level.procedural.RegionType;
import level.procedural.marker.LayoutMarker;

public class ResolverConditionData {
    public final RegionType region;
    public final LMTUnresolvedElement type;
    public final LayoutMarker marker;
    public final Level l;

    public ResolverConditionData(RegionType region, LMTUnresolvedElement type, LayoutMarker marker, Level l) {
        this.region = region;
        this.type = type;
        this.marker = marker;
        this.l = l;
    }
}
