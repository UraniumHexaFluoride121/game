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

    public DynamicHitBox centerOrigin() {
        up = (up + down) / 2;
        down = up;
        left = (left + right) / 2;
        right = left;
        return this;
    }

    public DynamicHitBox originToRight() {
        left = left + right;
        right = 0;
        return this;
    }

    public DynamicHitBox originToLeft() {
        right = left + right;
        left = 0;
        return this;
    }

    public DynamicHitBox originToTop() {
        down = down + up;
        up = 0;
        return this;
    }

    public DynamicHitBox originToBottom() {
        up = up + down;
        down = 0;
        return this;
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
