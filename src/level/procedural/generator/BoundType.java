package level.procedural.generator;

import java.awt.*;

//Used to define different types of bounds used by procedural generation elements.
//It may be necessary to have more strict bounds for collision than decorations, for example.
public enum BoundType {
    //This bound type exists to reduce crowding. Will only take effect if a procedural element is
    //overlapping at least 2 other boxes of the same type
    OVERCROWDING(new Color(159, 255, 22), 2, "overcrowding"),

    //Obstruction boxes should overlap when it would result in a significant gameplay obstruction
    OBSTRUCTION(new Color(255, 170, 42), 0, "obstruction"),

    //This encompasses the actual collision as closely as possible
    COLLISION(new Color(205, 59, 59), 0, "collision");

    public final Color debugColor;
    public final int collisionsAllowed;

    private final String s;

    BoundType(Color debugColor, int collisionsAllowed, String s) {
        this.debugColor = debugColor;
        this.collisionsAllowed = collisionsAllowed;
        this.s = s;
    }

    public static BoundType getBoundType(String s) {
        for (BoundType order : BoundType.values()) {
            if (order.s.equals(s))
                return order;
        }
        throw new IllegalArgumentException("Unknown bound type: " + s);
    }

    @Override
    public String toString() {
        return s;
    }
}
