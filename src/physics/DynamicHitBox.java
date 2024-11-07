package physics;

import foundation.Deletable;
import foundation.math.ObjPos;

import java.util.function.Supplier;

public class DynamicHitBox implements HitBox, Deletable {
    public float up, down, left, right;
    public Supplier<ObjPos> origin;

    public DynamicHitBox(float up, float down, float left, float right, Supplier<ObjPos> origin) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.origin = origin;
    }


    @Override
    public float getTop() {
        return origin.get().y + up;
    }

    @Override
    public float getBottom() {
        return origin.get().y - down;
    }

    @Override
    public float getLeft() {
        return origin.get().x - left;
    }

    @Override
    public float getRight() {
        return origin.get().x + right;
    }

    //Remove reference to parent object
    @Override
    public void delete() {
        origin = null;
    }
}
