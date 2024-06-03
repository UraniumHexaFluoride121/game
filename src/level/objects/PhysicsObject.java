package level.objects;

import foundation.ObjPos;
import foundation.VelocityHandler;

public abstract class PhysicsObject extends BlockLike {
    public VelocityHandler velocity = new VelocityHandler();
    public float mass;

    public PhysicsObject(ObjPos pos) {
        super(pos);
    }

    public void applyImpulse(ObjPos impulse) {
        velocity.applyImpulse(impulse, mass);
    }

    public void applyAcceleration(ObjPos acceleration, float deltaTime) {
        velocity.applyAcceleration(acceleration, deltaTime);
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        processMovement(deltaTime);
    }

    public void processMovement(float deltaTime) {
        velocity.tickExponentialDecay(deltaTime, 3f);
        velocity.tickLinearDecay(deltaTime, 1.5f);
        pos.add(velocity.copy().multiply(deltaTime));
    }
}
