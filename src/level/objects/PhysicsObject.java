package level.objects;

import foundation.*;
import level.ObjectLayer;
import physics.*;
import render.event.RenderEvent;

public abstract class PhysicsObject extends BlockLike {
    public static final float BOUNCE_THRESHOLD = 2;

    public VelocityHandler velocity = new VelocityHandler();
    public Constraints constraints = new Constraints(), previousConstraints = new Constraints();
    public ObjPos prevPos;
    private boolean previouslyFalling = false;
    public final float mass;

    public static final ObjPos DEFAULT_GRAVITY = new ObjPos(0, -30);

    public PhysicsObject(ObjPos pos, String name, float mass) {
        super(pos, name);
        prevPos = pos.copy();
        this.mass = mass;
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
        return mass;
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
        float f = computeFriction();
        velocity.tickExponentialXDecay(deltaTime, 3f * f);
        velocity.tickExponentialYDecay(deltaTime, .5f);
        velocity.tickLinearXDecay(deltaTime, 1.5f * f);
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

    public float computeFriction() {
        float blockBelowFriction = 0;
        int blockBelowCount = 0;
        for (int i = 0; i < 5; i++) {
            CollisionObject objectBelow = MainPanel.level.collisionHandler.getObjectAt(new ObjPos(MathHelper.lerp(hitBox.getLeft(), hitBox.getRight(), MathHelper.normalise(0, 4, i)), hitBox.getBottom() - 0.05f));
            if (objectBelow != null) {
                blockBelowCount++;
                blockBelowFriction += objectBelow.getFriction();
            }
        }
        if (blockBelowCount == 0)
            return getFriction();
        return getFriction() * (blockBelowFriction / blockBelowCount);
    }

    public float computeBounciness(Direction d) {
        float blockBounciness = 0;
        int blockCount = 0;
        for (int i = 0; i < 5; i++) {
            ObjPos samplePos = switch (d) {
                case DOWN ->
                        new ObjPos(MathHelper.lerp(hitBox.getLeft(), hitBox.getRight(), MathHelper.normalise(0, 4, i)), hitBox.getBottom() - 0.05f);
                case UP ->
                        new ObjPos(MathHelper.lerp(hitBox.getLeft(), hitBox.getRight(), MathHelper.normalise(0, 4, i)), hitBox.getTop() + 0.05f);
                case LEFT ->
                        new ObjPos(hitBox.getLeft() - 0.05f, MathHelper.lerp(hitBox.getBottom(), hitBox.getTop(), MathHelper.normalise(0, 4, i)));
                case RIGHT ->
                        new ObjPos(hitBox.getRight() + 0.05f, MathHelper.lerp(hitBox.getBottom(), hitBox.getTop(), MathHelper.normalise(0, 4, i)));
            };
            CollisionObject object = MainPanel.level.collisionHandler.getObjectAt(samplePos);
            if (object != null) {
                blockCount++;
                blockBounciness += object.getBounciness();
            }
        }
        if (blockCount == 0)
            return Math.min(1, getBounciness());
        return Math.min(1, getBounciness() + (blockBounciness / blockCount));
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
                //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                if (Math.signum(overlap.y) == Math.signum(velocity.y)) {
                    boolean bounce = Math.abs(velocity.y) > BOUNCE_THRESHOLD;
                    float computedBounciness = computeBounciness(velocity.y > 0 ? Direction.UP : Direction.DOWN);
                    if (bounce)
                        velocity.y = -velocity.y * computedBounciness;
                    else
                        velocity.y = 0;
                    if (overlap.y < 0) {
                        constraints.set(Direction.DOWN, otherBox.getTop());
                    } else {
                        constraints.set(Direction.UP, otherBox.getBottom());
                    }
                }
                if (overlap.y < 0) {
                    pos.y = otherBox.getTop() + thisBox.down;
                } else {
                    pos.y = otherBox.getBottom() - thisBox.up;
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
                if (Math.signum(overlap.x) == Math.signum(velocity.x)) {
                    boolean bounce = Math.abs(velocity.x) > BOUNCE_THRESHOLD;
                    float computedBounciness = computeBounciness(velocity.x > 0 ? Direction.RIGHT : Direction.LEFT);
                    if (bounce)
                        velocity.x = -velocity.x * computedBounciness;
                    else
                        velocity.x = 0;
                    if (overlap.x < 0) {
                        constraints.set(Direction.LEFT, otherBox.getRight());
                    } else {
                        constraints.set(Direction.RIGHT, otherBox.getLeft());
                    }
                }
                if (overlap.x < 0) {
                    pos.x = otherBox.getRight() + thisBox.left;
                } else {
                    pos.x = otherBox.getLeft() - thisBox.right;
                }
            }
        } else {
            if (other instanceof PhysicsObject physicsObject) {
                ObjPos overlap = thisBox.collisionOverlap(otherBox);
                float collisionYVelocity = velocity.y - physicsObject.velocity.y;
                float collisionXVelocity = velocity.x - physicsObject.velocity.x;
                //There are cases where we know that a collision is "incorrect" to calculate
                //on the Y-axis, and we therefore want to calculate it on the X-axis. We also do the
                //same if we detect an incorrect collision on the X-axis
                boolean forceXCollision = Math.signum(overlap.y) != Math.signum(collisionYVelocity);
                boolean forceYCollision = Math.signum(overlap.x) != Math.signum(collisionXVelocity);
                boolean isYAxis = Math.abs(overlap.y * (collisionXVelocity + 0.05f)) < Math.abs(overlap.x * (collisionYVelocity + 0.05f));
                if (!constraintsOnly) {
                    if (isYAxis) {
                        if (forceXCollision && Math.abs(overlap.x * 100) < Math.abs(overlap.y))
                            isYAxis = false;
                    } else {
                        if (forceYCollision && Math.abs(overlap.y * 100) < Math.abs(overlap.x)) {
                            isYAxis = true;
                        }
                    }
                }
                //PPrint physics debug info
                /*if (this instanceof Player || physicsObject instanceof Player) {
                    System.out.println("####################################");
                    System.out.println("constraints only " + constraintsOnly);
                    System.out.println("always snap " + alwaysSnap);
                    System.out.println("isY " + isYAxis);
                    System.out.println(this);
                    System.out.println(overlap);
                    System.out.println(velocity);
                    System.out.println(physicsObject.velocity);
                    System.out.println(collisionXVelocity);
                    System.out.println(collisionYVelocity);
                    System.out.println(pos);
                    System.out.println(physicsObject.pos);
                }*/

                if (isYAxis) {
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
                            //We make sure that the objects are separated slightly further apart than necessary,
                            //because otherwise they could still intersect even after separation due to floating-point errors
                            pos.subtractY(overlap.y * 0.501f);
                            physicsObject.pos.subtractY(overlap.y * -0.501f);
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
                            pos.subtractY(overlap.y * 0.501f);
                            physicsObject.pos.subtractY(overlap.y * -0.501f);
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
                            pos.subtractX(overlap.x * 0.501f);
                            physicsObject.pos.subtractX(overlap.x * -0.501f);
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
                            pos.subtractX(overlap.x * 0.501f);
                            physicsObject.pos.subtractX(overlap.x * -0.501f);
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