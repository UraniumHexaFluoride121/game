package level.procedural.marker.movement;

import foundation.Direction;
import foundation.Main;
import foundation.VelocityHandler;
import level.objects.PhysicsObject;
import level.objects.Player;
import level.procedural.Layout;
import level.procedural.marker.LMData;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;
import physics.StaticHitBox;
import render.Renderable;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.util.HashSet;

public class LMDPlayerMovement extends LMData {
    public HashSet<Float> acceleration = new HashSet<>();
    public HashSet<Renderable> debugRenderables = new HashSet<>();
    public Direction approachDirection = null;

    public LMDPlayerMovement(LayoutMarker lm) {
        super(lm);
        acceleration.add(0f);
    }

    private static final Color DEBUG_ACCELERATION_COLOR_RIGHT = new Color(153, 255, 0);
    private static final Color DEBUG_ACCELERATION_COLOR_LEFT = new Color(0, 255, 234);

    public LMDPlayerMovement addAcceleration(float blocks, float friction, boolean right) {
        StaticHitBox playerBox = AssetManager.blockHitBoxes.get("player");
        float playerWidth = playerBox.left + playerBox.right;
        //Check that acceleration cannot be calculated with the player starting outside the world border
        if (lm.pos.x - blocks < playerWidth)
            blocks = lm.pos.x - playerWidth;
        else if (lm.pos.x - blocks > Main.BLOCKS_X - playerWidth)
            blocks = lm.pos.x - (Main.BLOCKS_X - playerWidth);
        //If the resulting acceleration is in the wrong direction than was intended, skip adding it
        if (blocks == 0 || blocks > 0 != right)
            return this;
        acceleration.add(VelocityHandler.getVelocityToDistance(0, PhysicsObject.EXP_X_DECAY * friction, PhysicsObject.LINEAR_X_DECAY * friction, Player.MOVEMENT_ACCELERATION * Math.signum(blocks), blocks));
        if (Layout.DEBUG_RENDER && Layout.DEBUG_RENDER_JUMP_MOVEMENT_DISTANCE) {
            if (blocks > 0)
                debugRenderables.add(new RenderGameSquare(DEBUG_ACCELERATION_COLOR_RIGHT, new StaticHitBox(lm.pos.copy().addY(0.05f), lm.pos.copy().add(-blocks, 0.05f))));
            else
                debugRenderables.add(new RenderGameSquare(DEBUG_ACCELERATION_COLOR_LEFT, new StaticHitBox(lm.pos.copy().addY(-0.05f), lm.pos.copy().add(-blocks, -0.05f))));
        }
        return this;
    }

    public LMDPlayerMovement setApproachDirection(Direction approachDirection) {
        this.approachDirection = approachDirection;
        return this;
    }
}
