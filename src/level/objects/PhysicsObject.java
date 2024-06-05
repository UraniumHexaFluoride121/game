package level.objects;

import foundation.ObjPos;
import foundation.VelocityHandler;
import physics.CollisionBehaviour;
import physics.CollisionObject;
import physics.CollisionType;
import physics.HitBox;

public abstract class PhysicsObject extends BlockLike {
    public VelocityHandler velocity = new VelocityHandler();
    public boolean onGround = false;

    public static final ObjPos DEFAULT_GRAVITY = new ObjPos(0, -30);

    public PhysicsObject(ObjPos pos) {
        super(pos);
    }

    public void applyImpulse(ObjPos impulse) {
        velocity.applyImpulse(impulse, getMass());
    }

    public void applyAcceleration(ObjPos acceleration, float deltaTime) {
        velocity.applyAcceleration(acceleration, deltaTime);
    }

    public ObjPos getGravity() {
        return DEFAULT_GRAVITY;
    }

    public float getMass() {
        return 0.1f;
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        processMovement(deltaTime);
    }

    public void processMovement(float deltaTime) {
        velocity.applyAcceleration(getGravity(), deltaTime);
        velocity.tickExponentialXDecay(deltaTime, 3f);
        velocity.tickExponentialYDecay(deltaTime, .5f);
        velocity.tickLinearXDecay(deltaTime, 1.5f);
        pos.add(velocity.copy().multiply(deltaTime));
    }

    @Override
    public CollisionType getCollisionType() {
        return CollisionType.DYNAMIC;
    }

    @Override
    public CollisionBehaviour getCollisionBehaviour() {
        return CollisionBehaviour.PHYSICS;
    }

    @Override
    public void dynamicPreTick(float deltaTime) {
        onGround = false;
    }

    @Override
    public void onCollision(CollisionObject other) {
        HitBox thisBox = getHitBox();
        HitBox otherBox = other.getHitBox();
        if (other.getCollisionBehaviour() == CollisionBehaviour.IMMOVABLE) {
            ObjPos overlap = thisBox.collisionOverlap(otherBox);
            if (Math.abs(overlap.y * velocity.x) < Math.abs(overlap.x * velocity.y)) {
                pos.subtractY(overlap.y);
                //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                if (Math.signum(overlap.y) == Math.signum(velocity.y))
                    velocity.y = 0;
                if (overlap.y < 0) {
                    onGround = true;
                }
            } else {
                pos.subtractX(overlap.x);
                if (Math.signum(overlap.x) == Math.signum(velocity.x))
                    velocity.x = 0;
            }
        }
    }
}
