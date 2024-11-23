package physics;

import foundation.math.ObjPos;

public class StaticHitBox implements HitBox {
    public float up, down, left, right;

    public StaticHitBox(float up, float down, float left, float right, ObjPos origin) {
        this.up = origin.y + up;
        this.down = origin.y - down;
        this.left = origin.x - left;
        this.right = origin.x + right;
    }

    public StaticHitBox(float up, float down, float left, float right) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }

    public StaticHitBox(ObjPos from, ObjPos to) {
        this(to.y, from.y, from.x, to.x);
    }

    public StaticHitBox copy() {
        return new StaticHitBox(up, down, left, right);
    }

    public StaticHitBox expand(float amount) {
        up += amount;
        down -= amount;
        left -= amount;
        right += amount;
        return this;
    }

    public StaticHitBox expand(float x, float y) {
        up += y;
        down -= y;
        left -= x;
        right += x;
        return this;
    }

    public StaticHitBox expand(float up, float down, float left, float right) {
        this.up += up;
        this.down -= down;
        this.left -= left;
        this.right += right;
        return this;
    }

    public float middleX() {
        return (right - left) / 2;
    }

    public float middleY() {
        return (up - down) / 2;
    }

    @Override
    public float getTop() {
        return up;
    }

    @Override
    public float getBottom() {
        return down;
    }

    @Override
    public float getLeft() {
        return left;
    }

    @Override
    public float getRight() {
        return right;
    }
}
