package level.procedural.marker;

import foundation.Deletable;

public class LMData implements Deletable {
    public LayoutMarker lm;
    public LMData(LayoutMarker lm) {
        this.lm = lm;
    }

    @Override
    public void delete() {

    }
}
