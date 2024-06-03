package physics;

import foundation.ObjPos;

public class StaticHitBox implements HitBox {
    public float up, down, left, right;
    public ObjPos origin;

    public StaticHitBox(float up, float down, float left, float right, ObjPos origin) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.origin = origin;
    }


    @Override
    public float getTop() {
        return origin.y + up;
    }

    @Override
    public float getBottom() {
        return origin.y - down;
    }

    @Override
    public float getLeft() {
        return origin.x - left;
    }

    @Override
    public float getRight() {
        return origin.x + right;
    }
}
