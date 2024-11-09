package level.procedural.generator;

import java.awt.*;

//Used to define different types of bounds used by procedural generation elements.
//It may be necessary to have more strict bounds for collision than decorations, for example.
public enum BoundType {
    //This bound type exists to reduce crowding. Will only take effect if a procedural element is
    //overlapping at least 2 other boxes of the same type
    OVER_CROWDING(new Color(159, 255, 22), 2),

    //Obstruction boxes should overlap when it would result in a significant gameplay obstruction
    OBSTRUCTION(new Color(255, 170, 42), 0),

    //This encompasses the actual collision as closely as possible
    COLLISION(new Color(205, 59, 59), 0);

    public final Color debugColor;
    public final int collisionsAllowed;

    BoundType(Color debugColor, int collisionsAllowed) {
        this.debugColor = debugColor;
        this.collisionsAllowed = collisionsAllowed;
    }
}
