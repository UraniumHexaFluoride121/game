package level.procedural.marker.movement;

import foundation.Direction;
import foundation.VelocityHandler;
import level.objects.PhysicsObject;
import level.objects.Player;
import level.procedural.marker.LMData;

import java.util.HashSet;

public class LMDPlayerMovement extends LMData {
    public HashSet<Float> acceleration = new HashSet<>();
    public Direction approachDirection = null;

    public LMDPlayerMovement() {
        acceleration.add(0f);
    }

    public LMDPlayerMovement addAcceleration(float blocks, float friction) {
        acceleration.add(VelocityHandler.getVelocityToDistance(0, PhysicsObject.EXP_X_DECAY * friction, PhysicsObject.LINEAR_X_DECAY * friction, Player.MOVEMENT_ACCELERATION * Math.signum(blocks), blocks));
        return this;
    }

    public LMDPlayerMovement setApproachDirection(Direction approachDirection) {
        this.approachDirection = approachDirection;
        return this;
    }
}
