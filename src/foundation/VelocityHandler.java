package foundation;

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

    public void tickExponentialDecay(float deltaTime, float decayConstant) {
        x = (float) (x * Math.exp(-decayConstant * deltaTime));
        y = (float) (y * Math.exp(-decayConstant * deltaTime));
    }

    public void tickLinearDecay(float deltaTime, float decayConstant) {
        float length = length();
        if (length < decayConstant * deltaTime)
            set(0, 0);
        else
            setLength(length - decayConstant * deltaTime);
    }

    public VelocityHandler copyAsForceHandler() {
        return new VelocityHandler(x, y);
    }
}