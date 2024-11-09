package physics;

import level.Level;

public interface CollisionObject {
    HitBox getHitBox();
    boolean hasCollision();
    default boolean hasWorldBorderCollision() {
        return true;
    }
    CollisionType getCollisionType();
    CollisionBehaviour getCollisionBehaviour();

    void setCollisionData(CollisionHandler.CollisionObjectData data);
    CollisionHandler.CollisionObjectData getCollisionData();

    //The collision handler invokes this method just before the collision
    //checks are done
    default void dynamicPreTick(float deltaTime) {

    }

    //The collision handler invokes this method just after the collision
    //checks are done
    default void dynamicPostTick(float deltaTime) {

    }

    //Only one of the objects will have this method called. It is expected that
    //that object handle the collision for both of them. constraintsOnly if for the
    //initial collision to set everything in place before allowing physics objects to
    //push each other, preventing cases where for example a block could "catch" on another
    //while moving over it when it's not supposed to. alwaysSnap is set if the collision could
    //not be solved, forcing blocks to snap regardless of constraints as a final attempt
    //at solving the collision. This can for example solve situations with three blocks in a
    //row where the middle one gets bounced back and forth, where snapping would force it into
    //place.
    default void onCollision(CollisionObject other, boolean constrainsOnly, boolean alwaysSnap) {

    }

    float getFriction();
    float getBounciness();

    default void registerCollision(Level level) {
        level.collisionHandler.register(this);
    }

    default void removeCollision(Level level) {
        level.collisionHandler.remove(this);
    }
}
