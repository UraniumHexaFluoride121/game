package level.objects;

import foundation.ObjPos;
import foundation.VelocityHandler;
import physics.CollisionObject;
import physics.CollisionType;
import physics.HitBox;

public abstract class PhysicsObject extends BlockLike {
    public static final double COLLISION_SNAP_THRESHOLD = 0.1;
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

    @Override
    public CollisionType getCollisionType() {
        return CollisionType.DYNAMIC;
    }

    @Override
    public void onCollision(CollisionObject other) {
        HitBox thisBox = getHitBox();
        HitBox otherBox = other.getHitBox();
        if (other.getCollisionType() == CollisionType.STATIC) {
            ObjPos overlap = thisBox.collisionOverlap(otherBox);
            if (Math.abs(overlap.y * velocity.x) < Math.abs(overlap.x * velocity.y)) {
                pos.subtractY(overlap.y);
                //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                if (Math.signum(overlap.y) == Math.signum(velocity.y))
                    velocity.y = 0;
            } else {
                pos.subtractX(overlap.x);
                if (Math.signum(overlap.x) == Math.signum(velocity.x))
                    velocity.x = 0;
            }
        }
    }
}
