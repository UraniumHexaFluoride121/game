package level.objects;

import foundation.Direction;
import foundation.MainPanel;
import foundation.VelocityHandler;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.ObjectLayer;
import physics.*;
import render.event.RenderEvent;

public abstract class PhysicsObject extends BlockLike {
    public static final float BOUNCE_THRESHOLD = 2;

    public VelocityHandler velocity = new VelocityHandler(), previousVelocity = new VelocityHandler();
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
        //All velocity processing must happen BEFORE this point so that the new velocity
        //can be applied on the same tick
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
        previousVelocity = velocity.copyAsVelocityHandler();
    }

    @Override
    public void dynamicPostTick(float deltaTime) {
        if (constraints.is(Direction.UP) || constraints.is(Direction.DOWN)) {

        }
        if (constraints.is(Direction.LEFT) || constraints.is(Direction.RIGHT)) {
        }
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
            return MathHelper.clamp(0, 1, getBounciness());
        return MathHelper.clamp(0, 1, getBounciness() + (blockBounciness / blockCount));
    }

    private static final boolean PRINT_DEBUG_COLLISION = false;

    @Override
    public void onCollision(CollisionObject other, boolean constraintsOnly, boolean alwaysSnap) {
        DynamicHitBox thisBox = ((DynamicHitBox) getHitBox());
        HitBox otherBox = other.getHitBox();
        if (other.getCollisionBehaviour() == CollisionBehaviour.IMMOVABLE) {
            ObjPos overlap = thisBox.collisionOverlap(otherBox);
            if (velocity.x < 0) {
                if (previousConstraints.is(Direction.LEFT) && previousConstraints.to(Direction.LEFT) == otherBox.getRight()) {
                    if (previousConstraints.to(Direction.DOWN) != otherBox.getTop() && previousConstraints.to(Direction.UP) != otherBox.getBottom()) {
                        constraints.set(Direction.LEFT, otherBox.getRight(), otherBox);
                        pos.x = prevPos.x;
                        velocity.x = 0;
                        return;
                    }
                }
            } else {
                if (previousConstraints.is(Direction.RIGHT) && previousConstraints.to(Direction.RIGHT) == otherBox.getLeft()) {
                    if (previousConstraints.to(Direction.DOWN) != otherBox.getTop() && previousConstraints.to(Direction.UP) != otherBox.getBottom()) {
                        constraints.set(Direction.RIGHT, otherBox.getLeft(), otherBox);
                        pos.x = prevPos.x;
                        velocity.x = 0;
                        return;
                    }
                }
            }
            if (velocity.y < 0) {
                if (previousConstraints.is(Direction.DOWN) && previousConstraints.to(Direction.DOWN) == otherBox.getTop()) {
                    if (previousConstraints.to(Direction.LEFT) != otherBox.getRight() && previousConstraints.to(Direction.RIGHT) != otherBox.getLeft()) {
                        constraints.set(Direction.DOWN, otherBox.getTop(), otherBox);
                        pos.y = prevPos.y;
                        velocity.y = 0;
                        return;
                    }
                }
            } else {
                if (previousConstraints.is(Direction.UP) && previousConstraints.to(Direction.UP) == otherBox.getBottom()) {
                    if (previousConstraints.to(Direction.LEFT) != otherBox.getRight() && previousConstraints.to(Direction.RIGHT) != otherBox.getLeft()) {
                        constraints.set(Direction.UP, otherBox.getBottom(), otherBox);
                        pos.y = prevPos.y;
                        velocity.y = 0;
                        return;
                    }
                }
            }
            if (Math.abs(overlap.y * velocity.x) < Math.abs(overlap.x * velocity.y)) {
                //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                if (Math.signum(overlap.y) == Math.signum(velocity.y) || velocity.y == 0) {
                    if (Math.abs(velocity.y) > BOUNCE_THRESHOLD) {
                        float computedBounciness = computeBounciness(velocity.y > 0 ? Direction.UP : Direction.DOWN);
                        velocity.y = -velocity.y * computedBounciness;
                    } else
                        velocity.y = 0;
                    if (overlap.y < 0) {
                        constraints.set(Direction.DOWN, otherBox.getTop(), otherBox);
                        if (constraints.box(Direction.LEFT, Direction.UP) == otherBox.getTop()) {
                            constraints.remove(Direction.LEFT);
                            velocity.x = previousVelocity.x;
                        }
                        if (constraints.box(Direction.RIGHT, Direction.UP) == otherBox.getTop()) {
                            constraints.remove(Direction.RIGHT);
                            velocity.x = previousVelocity.x;
                        }
                    } else {
                        constraints.set(Direction.UP, otherBox.getBottom(), otherBox);
                        if (constraints.box(Direction.LEFT, Direction.DOWN) == otherBox.getBottom()) {
                            constraints.remove(Direction.LEFT);
                            velocity.x = previousVelocity.x;
                        }
                        if (constraints.box(Direction.RIGHT, Direction.DOWN) == otherBox.getBottom()) {
                            constraints.remove(Direction.RIGHT);
                            velocity.x = previousVelocity.x;
                        }
                    }
                }
                if (overlap.y < 0) {
                    pos.y = otherBox.getTop() + thisBox.down;
                } else {
                    pos.y = otherBox.getBottom() - thisBox.up;
                }
            } else {
                if (Math.signum(overlap.x) == Math.signum(velocity.x) || velocity.x == 0) {
                    if (Math.abs(velocity.x) > BOUNCE_THRESHOLD) {
                        float computedBounciness = computeBounciness(velocity.x > 0 ? Direction.RIGHT : Direction.LEFT);
                        velocity.x = -velocity.x * computedBounciness;
                    } else
                        velocity.x = 0;
                    if (overlap.x < 0) {
                        constraints.set(Direction.LEFT, otherBox.getRight(), otherBox);
                        if (constraints.box(Direction.UP, Direction.RIGHT) == otherBox.getRight()) {
                            constraints.remove(Direction.UP);
                            velocity.y = previousVelocity.y;
                        }
                        if (constraints.box(Direction.DOWN, Direction.RIGHT) == otherBox.getRight()) {
                            constraints.remove(Direction.DOWN);
                            velocity.y = previousVelocity.y;
                        }
                    } else {
                        constraints.set(Direction.RIGHT, otherBox.getLeft(), otherBox);
                        if (constraints.box(Direction.UP, Direction.LEFT) == otherBox.getLeft()) {
                            constraints.remove(Direction.UP);
                            velocity.y = previousVelocity.y;
                        }
                        if (constraints.box(Direction.DOWN, Direction.LEFT) == otherBox.getLeft()) {
                            constraints.remove(Direction.DOWN);
                            velocity.y = previousVelocity.y;
                        }
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
                DynamicHitBox otherDBox = ((DynamicHitBox) otherBox);
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
                //Print physics debug info
                if (PRINT_DEBUG_COLLISION/* && (this instanceof Player || physicsObject instanceof Player)*/) {
                    System.out.println("####################################");
                    System.out.println("constraints only " + constraintsOnly);
                    System.out.println("always snap " + alwaysSnap);
                    System.out.println("isY " + isYAxis);
                    System.out.println(constraints);
                    System.out.println(physicsObject.constraints);
                    System.out.println(this);
                    System.out.println(physicsObject);
                    System.out.println("overlap: " + overlap);
                    System.out.println("v this: " + velocity);
                    System.out.println("v other: " + physicsObject.velocity);
                    System.out.println("v x: " + collisionXVelocity);
                    System.out.println("v y: " + collisionYVelocity);
                    System.out.println("pos this: " + pos);
                    System.out.println("pos other: " + physicsObject.pos);
                }

                if (isYAxis) {
                    //Cancel out velocity, but only if the velocity was facing toward the colliding hit box
                    boolean cancelVelocity = Math.signum(overlap.y) == Math.signum(collisionYVelocity);

                    if (overlap.y < 0) {
                        if (physicsObject.constraints.is(Direction.DOWN)) {
                            pos.y = otherBox.getTop() + thisBox.down;
                            if (cancelVelocity)
                                velocity.y = 0;
                            constraints.set(Direction.DOWN, otherBox.getTop());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(1);
                        } else if (constraints.is(Direction.UP)) {
                            physicsObject.pos.y = thisBox.getBottom() - otherDBox.up;
                            if (cancelVelocity)
                                physicsObject.velocity.y = 0;
                            physicsObject.constraints.set(Direction.UP, thisBox.getBottom());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(2);
                        } else if (alwaysSnap) {
                            pos.y = otherBox.getTop() + thisBox.down;
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println("always snap 1");
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
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(3);
                        }
                    } else {
                        if (constraints.is(Direction.DOWN)) {
                            physicsObject.pos.y = thisBox.getTop() + otherDBox.down;
                            if (cancelVelocity)
                                physicsObject.velocity.y = 0;
                            physicsObject.constraints.set(Direction.DOWN, thisBox.getTop());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(4);
                        } else if (physicsObject.constraints.is(Direction.UP)) {
                            pos.y = otherBox.getBottom() - thisBox.up;
                            if (cancelVelocity)
                                velocity.y = 0;
                            constraints.set(Direction.UP, otherBox.getBottom());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(5);
                        } else if (alwaysSnap) {
                            pos.y = otherBox.getBottom() - thisBox.up;
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println("always snap 2");
                        } else if (!constraintsOnly) {
                            pos.subtractY(overlap.y * 0.501f);
                            physicsObject.pos.subtractY(overlap.y * -0.501f);
                            if (cancelVelocity) {
                                float v = (velocity.y * getMass() + physicsObject.velocity.y * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
                                velocity.y = v;
                                physicsObject.velocity.y = v;
                            }
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(6);
                        }
                    }
                } else {
                    boolean cancelVelocity = Math.signum(overlap.x) == Math.signum(collisionXVelocity);
                    if (overlap.x < 0) {
                        if (physicsObject.constraints.is(Direction.LEFT)) {
                            pos.x = otherBox.getRight() + thisBox.left;
                            if (cancelVelocity)
                                velocity.x = 0;
                            constraints.set(Direction.LEFT, otherBox.getRight());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(1);
                        } else if (constraints.is(Direction.RIGHT)) {
                            physicsObject.pos.x = thisBox.getLeft() - otherDBox.right;
                            physicsObject.pos.subtractX(overlap.x * -1);
                            if (cancelVelocity)
                                physicsObject.velocity.x = 0;
                            physicsObject.constraints.set(Direction.RIGHT, thisBox.getLeft());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(2);
                        } else if (alwaysSnap) {
                            pos.x = otherBox.getRight() + thisBox.left;
                            constraints.set(Direction.LEFT, otherBox.getRight());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println("always snap 1");
                        } else if (!constraintsOnly) {
                            pos.subtractX(overlap.x * 0.501f);
                            physicsObject.pos.subtractX(overlap.x * -0.501f);
                            if (cancelVelocity) {
                                float v = (velocity.x * getMass() + physicsObject.velocity.x * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
                                velocity.x = v;
                                physicsObject.velocity.x = v;
                            }
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(3);
                        }
                    } else {
                        if (constraints.is(Direction.LEFT)) {
                            physicsObject.pos.x = thisBox.getRight() + otherDBox.left;
                            if (cancelVelocity)
                                physicsObject.velocity.x = 0;
                            physicsObject.constraints.set(Direction.LEFT, thisBox.getRight());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(4);
                        } else if (physicsObject.constraints.is(Direction.RIGHT)) {
                            pos.x = otherBox.getLeft() - thisBox.right;
                            if (cancelVelocity)
                                velocity.x = 0;
                            constraints.set(Direction.RIGHT, otherBox.getLeft());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(5);
                        } else if (alwaysSnap) {
                            pos.x = otherBox.getLeft() - thisBox.right;
                            constraints.set(Direction.RIGHT, otherBox.getLeft());
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println("always snap 2");
                        } else if (!constraintsOnly) {
                            pos.subtractX(overlap.x * 0.501f);
                            physicsObject.pos.subtractX(overlap.x * -0.501f);
                            if (cancelVelocity) {
                                float v = (velocity.x * getMass() + physicsObject.velocity.x * physicsObject.getMass()) / (getMass() + physicsObject.getMass());
                                velocity.x = v;
                                physicsObject.velocity.x = v;
                            }
                            if (PRINT_DEBUG_COLLISION)
                                System.out.println(6);
                        }
                    }
                }
            }
        }
    }
}