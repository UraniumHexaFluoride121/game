package level.objects;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import level.ObjectLayer;
import physics.CollisionBehaviour;
import physics.CollisionType;
import physics.HitBox;

public class StaticBlock extends BlockLike {
    private final CollisionType collisionType;
    public final ObjectLayer objectLayer;

    public StaticBlock(ObjPos pos, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight, CollisionType collisionType, ObjectLayer objectLayer) {
        super(pos);
        this.collisionType = collisionType;
        this.objectLayer = objectLayer;
        createHitBox(hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight);
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
        return true;
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
