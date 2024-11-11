package physics;

import foundation.Direction;

import java.util.HashMap;

public class Constraints {
    public HashMap<Direction, Boolean> isConstrained = new HashMap<>();
    public HashMap<Direction, Float> constrainedTo = new HashMap<>();
    public HashMap<Direction, Float> onceConstrainedTo = new HashMap<>();
    public HashMap<Direction, HitBox> constrainedToBox = new HashMap<>();

    public Constraints() {
        for (Direction direction : Direction.values()) {
            isConstrained.put(direction, false);
        }
    }

    public boolean is(Direction d) {
        return isConstrained.get(d);
    }

    public float to(Direction d) {
        return constrainedTo.getOrDefault(d, -1f);
    }

    public float box(Direction d, Direction boxDirection) {
        HitBox box = constrainedToBox.get(d);
        if (box == null)
            return -1;
        return box.get(boxDirection);
    }

    public void set(Direction d, float to) {
        isConstrained.put(d, true);
        constrainedTo.put(d, to);
        onceConstrainedTo.put(d, to);
    }

    public void set(Direction d, float to, HitBox box) {
        isConstrained.put(d, true);
        constrainedTo.put(d, to);
        onceConstrainedTo.put(d, to);
        constrainedToBox.put(d, box);
    }

    public void remove(Direction d) {
        isConstrained.put(d, false);
        constrainedTo.remove(d);
        constrainedToBox.remove(d);
    }

    public boolean onceConstrainedTo(Direction d) {
        return onceConstrainedTo.get(d) != null;
    }

    @Override
    public String toString() {
        return "is constrained: " + isConstrained.toString() + ", constrained to: " + constrainedTo.toString();
    }
}
