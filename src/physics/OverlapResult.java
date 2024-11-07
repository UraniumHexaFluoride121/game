package physics;

import foundation.math.ObjPos;

public class OverlapResult {
    public final ObjPos amount; //This is a non-directional value for the overlap on the X and Y axes
    public final OverlapDirection xDir, yDir; //Whether the other box is overlapping in front (POSITIVE), behind (NEGATIVE) or NONE a direction cannot be determined

    public OverlapResult(ObjPos amount, OverlapDirection xDir, OverlapDirection yDir) {
        this.amount = amount;
        this.xDir = xDir;
        this.yDir = yDir;
    }

    @Override
    public String toString() {
        return "{" + amount + ", xDir: " + xDir + ", yDir: " + yDir + "}";
    }
}
