package level.objects;

import foundation.ObjPos;
import foundation.VelocityHandler;
import level.ObjectLayer;
import physics.CollisionBehaviour;
import physics.CollisionObject;
import physics.CollisionType;
import physics.HitBox;
import render.RenderEvent;

public abstract class PhysicsObject extends BlockLike {
    public VelocityHandler velocity = new VelocityHandler();
    public boolean downConstrained = false, upConstrained = false, leftConstrained = false, rightConstrained = false;
    private boolean previouslyOnGround = true, previouslyFalling = false;
    private VelocityHandler preCollisionVelocity;

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
        if (velocity.y < 0 && !previouslyFalling)
            renderElement.onEvent(RenderEvent.ON_BLOCK_FALLING);
        previouslyFalling = velocity.y < 0;

        super.tick(deltaTime);
        processMovement(deltaTime);

        if (downConstrained && !previouslyOnGround)
            renderElement.onEvent(RenderEvent.ON_BLOCK_LAND);
        previouslyOnGround = downConstrained;
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
    public ObjectLayer getLayer() {
        return ObjectLayer.DYNAMIC;
    }

    @Override
    public void dynamicPreTick(float deltaTime) {
        downConstrained = false;
        upConstrained = false;
        leftConstrained = false;
        rightConstrained = false;
        preCollisionVelocity = velocity.copyAsVelocityHandler();
    }

    @Override
    public void onCollision(CollisionObject other, boolean constraintsOnly, boolean alwaysSnap) {
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
                    downConstrained = true;
                } else {
                    upConstrained = true;
                }
            } else {
                pos.subtractX(overlap.x);
                if (Math.signum(overlap.x) == Math.signum(velocity.x))
                    velocity.x = 0;
                if (overlap.x < 0) {
                    leftConstrained = true;
                } else {
                    rightConstrained = true;
                }
            }
        } else {
            if (other instanceof PhysicsObject physicsObject) {
                ObjPos overlap = thisBox.collisionOverlap(otherBox);
                float collisionYVelocity = velocity.y - physicsObject.velocity.y;
                float collisionXVelocity = velocity.x - physicsObject.velocity.x;
                if (Math.abs(overlap.y * (collisionXVelocity + 0.05f)) < Math.abs(overlap.x * (collisionYVelocity + 0.05f))) {
                    //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                    boolean cancelVelocity = Math.signum(overlap.y) == Math.signum(collisionYVelocity);

                    if (overlap.y < 0) {
                        if (alwaysSnap || physicsObject.downConstrained) {
                            pos.subtractY(overlap.y);
                            if (cancelVelocity)
                                velocity.y = 0;
                            downConstrained = true;
                        } else if (upConstrained) {
                            physicsObject.pos.subtractY(overlap.y * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.y = 0;
                            physicsObject.upConstrained = true;
                        } else if (!constraintsOnly) {
                            pos.subtractY(overlap.y * 0.5f);
                            physicsObject.pos.subtractY(overlap.y * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.y + physicsObject.velocity.y) / 2;
                                velocity.y = v;
                                physicsObject.velocity.y = v;
                            }
                        }
                    } else {
                        if (alwaysSnap || downConstrained) {
                            physicsObject.pos.subtractY(overlap.y * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.y = 0;
                            physicsObject.downConstrained = true;
                        } else if (physicsObject.upConstrained) {
                            pos.subtractY(overlap.y);
                            if (cancelVelocity)
                                velocity.y = 0;
                            upConstrained = true;
                        } else if (!constraintsOnly) {
                            pos.subtractY(overlap.y * 0.5f);
                            physicsObject.pos.subtractY(overlap.y * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.y + physicsObject.velocity.y) / 2;
                                velocity.y = v;
                                physicsObject.velocity.y = v;
                            }
                        }
                    }
                } else {
                    boolean cancelVelocity = Math.signum(overlap.x) == Math.signum(collisionXVelocity);

                    if (overlap.x < 0) {
                        if (alwaysSnap || physicsObject.leftConstrained) {
                            pos.subtractX(overlap.x);
                            if (cancelVelocity)
                                velocity.x = 0;
                            leftConstrained = true;
                        } else if (rightConstrained) {
                            physicsObject.pos.subtractX(overlap.x * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.x = 0;
                            physicsObject.rightConstrained = true;
                        } else if (!constraintsOnly) {
                            pos.subtractX(overlap.x * 0.5f);
                            physicsObject.pos.subtractX(overlap.x * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.x + physicsObject.velocity.x) / 2;
                                velocity.x = v;
                                physicsObject.velocity.x = v;
                            }
                        }
                    } else {
                        if (alwaysSnap || leftConstrained) {
                            physicsObject.pos.subtractX(overlap.x * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.x = 0;
                            physicsObject.leftConstrained = true;
                        } else if (physicsObject.rightConstrained) {
                            pos.subtractX(overlap.x);
                            if (cancelVelocity)
                                velocity.x = 0;
                            rightConstrained = true;
                        } else if (!constraintsOnly) {
                            pos.subtractX(overlap.x * 0.5f);
                            physicsObject.pos.subtractX(overlap.x * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.x + physicsObject.velocity.x) / 2;
                                velocity.x = v;
                                physicsObject.velocity.x = v;
                            }
                        }
                    }
                }
            }
        }
    }
}