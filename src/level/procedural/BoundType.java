package level.procedural;

import java.awt.*;

//Used to define different types of bounds used by procedural generation elements.
//It may be necessary to have more strict bounds for collision than decorations, for example.
public enum BoundType {
    COLLISION(new Color(205, 59, 59)),
    PADDED_COLLISION(new Color(255, 151, 42));

    public final Color debugColor;

    BoundType(Color debugColor) {
        this.debugColor = debugColor;
    }
}
