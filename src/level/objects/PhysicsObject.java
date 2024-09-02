package level.objects;

import foundation.Direction;
import foundation.ObjPos;
import foundation.VelocityHandler;
import level.ObjectLayer;
import physics.*;
import render.RenderEvent;

public abstract class PhysicsObject extends BlockLike {
    public VelocityHandler velocity = new VelocityHandler();
    public Constraints constraints = new Constraints(), previousConstraints = new Constraints();
    public ObjPos prevPos;
    private boolean previouslyFalling = false;

    public static final ObjPos DEFAULT_GRAVITY = new ObjPos(0, -30);

    public PhysicsObject(ObjPos pos) {
        super(pos);
        prevPos = pos.copy();
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

        if (constraints.is(Direction.DOWN) && !previousConstraints.is(Direction.DOWN))
            renderElement.onEvent(RenderEvent.ON_BLOCK_LAND);
        previousConstraints = constraints;
    }

    public void processMovement(float deltaTime) {
        prevPos = pos.copy();
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
        constraints = new Constraints();
    }

    @Override
    public void onCollision(CollisionObject other, boolean constraintsOnly, boolean alwaysSnap) {
        DynamicHitBox thisBox = ((DynamicHitBox) getHitBox());
        HitBox otherBox = other.getHitBox();
        if (other.getCollisionBehaviour() == CollisionBehaviour.IMMOVABLE) {
            ObjPos overlap = thisBox.collisionOverlap(otherBox);
            if (Math.abs(overlap.y * velocity.x) < Math.abs(overlap.x * velocity.y)) {
                if (velocity.x < 0) {
                    if (previousConstraints.is(Direction.LEFT) && previousConstraints.to(Direction.LEFT) == otherBox.getRight()) {
                        constraints.set(Direction.LEFT, otherBox.getRight());
                        pos.x = prevPos.x;
                        return;
                    }
                } else {
                    if (previousConstraints.is(Direction.RIGHT) && previousConstraints.to(Direction.RIGHT) == otherBox.getLeft()) {
                        constraints.set(Direction.RIGHT, otherBox.getLeft());
                        pos.x = prevPos.x;
                        return;
                    }
                }
                if (velocity.y < 0) {
                    pos.y = otherBox.getTop() + thisBox.down;
                } else {
                    pos.y = otherBox.getBottom() - thisBox.up;
                }
                //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                if (Math.signum(overlap.y) == Math.signum(velocity.y))
                    velocity.y = 0;
                if (overlap.y < 0) {
                    constraints.set(Direction.DOWN, otherBox.getTop());
                } else {
                    constraints.set(Direction.UP, otherBox.getBottom());
                }
            } else {
                if (velocity.y < 0) {
                    if (previousConstraints.is(Direction.DOWN) && previousConstraints.to(Direction.DOWN) == otherBox.getTop()) {
                        constraints.set(Direction.DOWN, otherBox.getTop());
                        pos.y = prevPos.y;
                        return;
                    }
                } else {
                    if (previousConstraints.is(Direction.UP) && previousConstraints.to(Direction.UP) == otherBox.getBottom()) {
                        constraints.set(Direction.UP, otherBox.getBottom());
                        pos.y = prevPos.y;
                        return;
                    }
                }
                if (velocity.x < 0) {
                    pos.x = otherBox.getRight() + thisBox.left;
                } else {
                    pos.x = otherBox.getLeft() - thisBox.right;
                }
                if (Math.signum(overlap.x) == Math.signum(velocity.x))
                    velocity.x = 0;
                if (overlap.x < 0) {
                    constraints.set(Direction.LEFT, otherBox.getRight());
                } else {
                    constraints.set(Direction.RIGHT, otherBox.getLeft());
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
                        if (alwaysSnap || physicsObject.constraints.is(Direction.DOWN)) {
                            pos.subtractY(overlap.y);
                            if (cancelVelocity)
                                velocity.y = 0;
                            constraints.set(Direction.DOWN, otherBox.getTop());
                        } else if (constraints.is(Direction.UP)) {
                            physicsObject.pos.subtractY(overlap.y * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.y = 0;
                            physicsObject.constraints.set(Direction.UP, thisBox.getBottom());
                        } else if (!constraintsOnly) {
                            pos.subtractY(overlap.y * 0.5f);
                            physicsObject.pos.subtractY(overlap.y * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.y * getMass() + physicsObject.velocity.y * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
                                velocity.y = v;
                                physicsObject.velocity.y = v;
                            }
                        }
                    } else {
                        if (alwaysSnap || constraints.is(Direction.DOWN)) {
                            physicsObject.pos.subtractY(overlap.y * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.y = 0;
                            physicsObject.constraints.set(Direction.DOWN, thisBox.getTop());
                        } else if (physicsObject.constraints.is(Direction.UP)) {
                            pos.subtractY(overlap.y);
                            if (cancelVelocity)
                                velocity.y = 0;
                            constraints.set(Direction.UP, otherBox.getBottom());
                        } else if (!constraintsOnly) {
                            pos.subtractY(overlap.y * 0.5f);
                            physicsObject.pos.subtractY(overlap.y * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.y * getMass() + physicsObject.velocity.y * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
                                velocity.y = v;
                                physicsObject.velocity.y = v;
                            }
                        }
                    }
                } else {
                    boolean cancelVelocity = Math.signum(overlap.x) == Math.signum(collisionXVelocity);
                    if (overlap.x < 0) {
                        if (alwaysSnap || physicsObject.constraints.is(Direction.LEFT)) {
                            pos.subtractX(overlap.x);
                            if (cancelVelocity)
                                velocity.x = 0;
                            constraints.set(Direction.LEFT, otherBox.getRight());
                        } else if (constraints.is(Direction.RIGHT)) {
                            physicsObject.pos.subtractX(overlap.x * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.x = 0;
                            physicsObject.constraints.set(Direction.RIGHT, thisBox.getLeft());
                        } else if (!constraintsOnly) {
                            pos.subtractX(overlap.x * 0.5f);
                            physicsObject.pos.subtractX(overlap.x * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.x * getMass() + physicsObject.velocity.x * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
                                velocity.x = v;
                                physicsObject.velocity.x = v;
                            }
                        }
                    } else {
                        if (alwaysSnap || constraints.is(Direction.LEFT)) {
                            physicsObject.pos.subtractX(overlap.x * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.x = 0;
                            physicsObject.constraints.set(Direction.LEFT, thisBox.getRight());
                        } else if (physicsObject.constraints.is(Direction.RIGHT)) {
                            pos.subtractX(overlap.x);
                            if (cancelVelocity)
                                velocity.x = 0;
                            constraints.set(Direction.RIGHT, otherBox.getLeft());
                        } else if (!constraintsOnly) {
                            pos.subtractX(overlap.x * 0.5f);
                            physicsObject.pos.subtractX(overlap.x * -0.5f);
                            if (cancelVelocity) {
                                float v = (velocity.x * getMass() + physicsObject.velocity.x * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
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