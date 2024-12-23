package level.objects;

import foundation.math.ObjPos;
import foundation.tick.TickOrder;
import level.Level;
import physics.HitBox;

public class PhysicsBlock extends PhysicsObject {
    private static int indexCounter = 0;
    public int index;

    public PhysicsBlock(ObjPos pos, String name, float mass, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight, Level level) {
        super(pos, name, mass, level);
        index = indexCounter++;
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
