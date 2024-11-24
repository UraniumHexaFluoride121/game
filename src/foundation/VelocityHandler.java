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

    public float getRemainingDistanceToStop(float expDecay, float linearDecay, boolean yAxis) {
        float a = Math.abs(yAxis ? y : x);
        float bc = linearDecay / expDecay;
        float t = ((float) (-Math.log(bc / (a + bc)) / expDecay));
        float dist = (float) (-bc * t - ((a + bc) * Math.exp(-expDecay * t)) / expDecay + (a + bc) / expDecay);
        return dist * Math.signum(yAxis ? y : x);
    }

    public float getRemainingTimeToStop(float expDecay, float linearDecay, boolean yAxis) {
        float a = Math.abs(yAxis ? y : x);
        float bc = linearDecay / expDecay;
        return ((float) (-Math.log(bc / (a + bc)) / expDecay));
    }

    public float getSignedRemainingTimeToStop(float expDecay, float linearDecay, boolean yAxis) {
        float a = Math.abs(yAxis ? y : x);
        float bc = linearDecay / expDecay;
        return ((float) (-Math.log(bc / (a + bc)) / expDecay));
    }

    public float getOffsetAfterTime(float expDecay, float linearDecay, boolean yAxis, float t) {
        float a = yAxis ? y : x;
        float bc = linearDecay / expDecay;
        return ((float) (-bc * t - (a + bc) * Math.exp(-expDecay * t) / expDecay + (a + bc) / expDecay));
    }

    public float getVelocityAfterTime(float expDecay, float linearDecay, boolean yAxis, float t) {
        float a = yAxis ? y : x;
        float bc = linearDecay / expDecay;
        return (float) (-bc + (a + bc) * Math.exp(-expDecay * t));
    }

    private static final float DELTA_TIME = 0.01f;

    public float getVelocityToDistance(float expDecay, float linearDecay, float acceleration, boolean yAxis, float distance) {
        VelocityHandler v = new VelocityHandler(yAxis ? y : x);
        float dist = 0;
        int loops = 0;
        while (Math.abs(dist) < Math.abs(distance) || Math.signum(dist) != Math.signum(distance)) {
            v.tickExponentialXDecay(DELTA_TIME, expDecay);
            v.tickLinearXDecay(DELTA_TIME, linearDecay);
            v.applyAcceleration(new ObjPos(acceleration), DELTA_TIME);
            dist += v.x * DELTA_TIME;
            loops++;
            if (loops > 1000)
                return Float.NaN;
        }
        return v.x;
    }

    public static float getVelocityToDistance(float initialSpeed, float expDecay, float linearDecay, float acceleration, float distance) {
        VelocityHandler v = new VelocityHandler(initialSpeed);
        float dist = 0;
        int loops = 0;
        while (Math.abs(dist) < Math.abs(distance) || Math.signum(dist) != Math.signum(distance)) {
            v.tickExponentialXDecay(DELTA_TIME, expDecay);
            v.tickLinearXDecay(DELTA_TIME, linearDecay);
            v.applyAcceleration(new ObjPos(acceleration), DELTA_TIME);
            dist += v.x * DELTA_TIME;
            loops++;
            if (loops > 1000)
                return Float.NaN;
        }
        return v.x;
    }

    public VelocityHandler copyAsVelocityHandler() {
        return new VelocityHandler(x, y);
    }
}