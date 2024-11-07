package foundation;

import foundation.math.ObjPos;

public class VelocityHandler extends ObjPos {
    public VelocityHandler() {

    }

    public VelocityHandler(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public VelocityHandler(float length) {
        x = length;
    }

    public void applyImpulse(ObjPos impulse, float mass) {
        x += impulse.x / mass;
        y += impulse.y / mass;
    }

    public void applyAcceleration(ObjPos acceleration, float deltaTime) {
        x += acceleration.x * deltaTime;
        y += acceleration.y * deltaTime;
    }

    public void tickExponentialXDecay(float deltaTime, float decayConstant) {
        x = (float) (x * Math.exp(-decayConstant * deltaTime));
    }

    public void tickExponentialYDecay(float deltaTime, float decayConstant) {
        y = (float) (y * Math.exp(-decayConstant * deltaTime));
    }

    public void tickLinearDecay(float deltaTime, float decayConstant) {
        float length = length();
        if (length < decayConstant * deltaTime)
            set(0, 0);
        else
            setLength(length - decayConstant * deltaTime);
    }

    public void tickLinearXDecay(float deltaTime, float decayConstant) {
        if (Math.abs(x) < decayConstant * deltaTime)
            x = 0;
        else
            x -= Math.signum(x) * decayConstant * deltaTime;
    }

    public VelocityHandler copyAsVelocityHandler() {
        return new VelocityHandler(x, y);
    }
}