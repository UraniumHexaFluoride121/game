package level.objects;

import foundation.math.ObjPos;
import foundation.tick.TickOrder;
import level.ObjectLayer;
import physics.CollisionBehaviour;
import physics.CollisionType;
import physics.HitBox;
import render.renderables.RenderTexture;

public class StaticBlock extends BlockLike {
    private final CollisionType collisionType;
    public final ObjectLayer objectLayer;
    public final boolean hasCollision;

    public StaticBlock(ObjPos pos, String name, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight, CollisionType collisionType, ObjectLayer objectLayer, boolean hasCollision) {
        super(pos, name);
        this.collisionType = collisionType;
        this.objectLayer = objectLayer;
        this.hasCollision = hasCollision;
        createHitBox(hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight);
    }

    @Override
    public boolean blockRequiresTick() {
        return !(renderElement instanceof RenderTexture texture) || texture.requiresTick();
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.ANIMATIONS_ONLY;
    }

    @Override
    public HitBox getHitBox() {
        return hitBox;
    }

    @Override
    public boolean hasCollision() {
        return hasCollision;
    }

    @Override
    public CollisionType getCollisionType() {
        return collisionType;
    }

    @Override
    public CollisionBehaviour getCollisionBehaviour() {
        return CollisionBehaviour.IMMOVABLE;
    }

    @Override
    public ObjectLayer getLayer() {
        return objectLayer;
    }
}
