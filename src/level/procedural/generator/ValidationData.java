package level.procedural.generator;

import java.util.HashMap;

public class ValidationData {
    public HashMap<BoundType, Integer> collisions = new HashMap<>();

    public ValidationData() {
        for (BoundType type : BoundType.values()) {
            if (type.collisionsAllowed > 0)
                collisions.put(type, 0);
        }
    }

    public void addCollision(BoundType type) {
        collisions.put(type, collisions.get(type) + 1);
    }

    public boolean isUnderAllowed(BoundType type) {
        return collisions.get(type) <= type.collisionsAllowed;
    }
}
