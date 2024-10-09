package physics;

public class WorldBorderObject implements CollisionObject {
    private CollisionHandler.CollisionObjectData data = new CollisionHandler.CollisionObjectData(0, 0);
    private final StaticHitBox hitBox;

    public WorldBorderObject(StaticHitBox hitBox) {
        this.hitBox = hitBox;
    }

    @Override
    public HitBox getHitBox() {
        return hitBox;
    }

    @Override
    public boolean hasCollision() {
        return true;
    }

    @Override
    public CollisionType getCollisionType() {
        return CollisionType.STATIC;
    }

    @Override
    public CollisionBehaviour getCollisionBehaviour() {
        return CollisionBehaviour.IMMOVABLE;
    }

    @Override
    public void setCollisionData(CollisionHandler.CollisionObjectData data) {
        this.data = data;
    }

    @Override
    public CollisionHandler.CollisionObjectData getCollisionData() {
        return data;
    }

    @Override
    public float getFriction() {
        return 1;
    }

    @Override
    public float getBounciness() {
        return 0;
    }
}
