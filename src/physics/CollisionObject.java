package physics;

import foundation.ObjPos;
import level.Level;

public interface CollisionObject {
    HitBox getHitBox();
    boolean hasCollision();
    CollisionType getCollisionType();

    void setCollisionData(CollisionHandler.CollisionObjectData data);
    CollisionHandler.CollisionObjectData getCollisionData();

    //Only one of the objects will have this method called. It is expected that
    //that object handle the collision for both of them
    default void onCollision(CollisionObject other) {

    }

    default void registerCollision(Level level) {
        level.collisionHandler.register(this);
    }

    default void removeCollision(Level level) {
        level.collisionHandler.remove(this);
    }
}
