package level.objects;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import physics.HitBox;

public class PhysicsBlock extends PhysicsObject {
    public PhysicsBlock(ObjPos pos, float mass, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight) {
        super(pos, mass);
        createHitBox(hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight);
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.BLOCK_MOVEMENT;
    }

    @Override
    public HitBox getHitBox() {
        return hitBox;
    }

    @Override
    public boolean hasCollision() {
        return true;
    }
}
