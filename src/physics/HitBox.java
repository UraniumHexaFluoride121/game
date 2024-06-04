package physics;

import foundation.ObjPos;

public interface HitBox {
    //Positions are absolute game coordinates
    float getTop();
    float getBottom();
    float getLeft();
    float getRight();

    default boolean isColliding(HitBox box) {
        return (getRight() > box.getLeft() && box.getRight() > getLeft()) &&
                (getTop() > box.getBottom() && box.getTop() > getBottom());
    }

    //Overlap direction is how much of the hit box is overlapping
    //toward the other box. For example, if this box hits another, equally
    //sized box from the right, at the same Y-value, and is overlapping by two units,
    //the overlap will be (-2, 0). -2 on the X-axis because there are two units of
    //overlap in the -X direction, and 0 on the Y-axis because the boxes are the
    //same size and at the same height, meaning there is no "direction" for the collision to
    //be in on the Y-axis
    default ObjPos collisionOverlap(HitBox otherBox) {
        ObjPos direction = new ObjPos();
        if (otherBox.getRight() >= getLeft() && otherBox.getRight() <= getRight()) {
            direction.addX(getLeft() - otherBox.getRight());
        } else if (getRight() >= otherBox.getLeft() && getRight() <= otherBox.getRight()) {
            direction.addX(getRight() - otherBox.getLeft());
        }
        if (otherBox.getTop() >= getBottom() && otherBox.getTop() <= getTop()) {
            direction.addY(getBottom() - otherBox.getTop());
        } else if (getTop() >= otherBox.getBottom() && getTop() <= otherBox.getTop()) {
            direction.addY(getTop() - otherBox.getBottom());
        }
        return direction;
    }

    default String asString() {
        return "Left: " + getLeft() + ", Right: " + getRight() + ", Bottom: " + getBottom() + ", Top: " + +getTop();
    }
}
