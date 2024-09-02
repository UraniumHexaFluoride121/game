package physics;

import foundation.Direction;

import java.util.HashMap;

public class Constraints {
    public HashMap<Direction, Boolean> isConstrained = new HashMap<>();
    public HashMap<Direction, Float> constrainedTo = new HashMap<>();

    public Constraints() {
        for (Direction direction : Direction.values()) {
            isConstrained.put(direction, false);
        }
    }

    public boolean is(Direction d) {
        return isConstrained.get(d);
    }

    public float to(Direction d) {
        return constrainedTo.get(d);
    }

    public void set(Direction d, float to) {
        isConstrained.put(d, true);
        constrainedTo.put(d, to);
    }

    @Override
    public String toString() {
        return "is constrained: " + isConstrained.toString() + ", constrained to: " + constrainedTo.toString();
    }
}
