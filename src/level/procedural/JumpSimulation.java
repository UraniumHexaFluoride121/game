package level.procedural;

import foundation.Deletable;
import foundation.Direction;
import foundation.MainPanel;
import foundation.VelocityHandler;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.objects.PhysicsObject;
import level.objects.Player;
import level.procedural.generator.BoundType;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.movement.LMDPlayerMovement;
import loader.AssetManager;
import physics.DynamicHitBox;
import physics.StaticHitBox;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameCircle;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.util.HashSet;

import static level.objects.PhysicsObject.*;

public class JumpSimulation implements Deletable, Renderable {
    public static final boolean DEBUG_RENDER_SIM = true;
    public LayoutMarker from, to;
    public HashSet<LayoutMarker> fromLMs, toLMS;
    public HashSet<RenderGameCircle> debugRenderCircles = new HashSet<>();
    public HashSet<RenderGameSquare> debugRenderSquares = new HashSet<>();

    public JumpSimulation(LayoutMarker from, LayoutMarker to, HashSet<LayoutMarker> fromLMs, HashSet<LayoutMarker> toLMS) {
        this.from = from;
        this.to = to;
        this.fromLMs = fromLMs;
        this.toLMS = toLMS;
        for (LayoutMarker fromLM : fromLMs) {
            if (fromLM.data instanceof LMDPlayerMovement fromData) {
                fromData.jumps.add(this);
            }
        }
    }

    public boolean validateJump() {
        HashSet<LayoutMarker> collisionMarkers = new HashSet<>();
        MainPanel.level.layout.forEachMarker(from.pos.y, 1, lm -> {
            if (lm.hasBoundType(BoundType.COLLISION))
                collisionMarkers.add(lm);
        });
        boolean debugRender = true;
        for (LayoutMarker fromLM : fromLMs) {
            for (LayoutMarker toLM : toLMS) {
                StaticHitBox playerStaticBox = AssetManager.blockHitBoxes.get("player");
                DynamicHitBox playerBox = new DynamicHitBox(playerStaticBox.up, playerStaticBox.down, playerStaticBox.left, playerStaticBox.right, this::getSimPos);
                validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), Player.JUMP_IMPULSE / 10 * 9, debugRender);
                validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), Player.JUMP_IMPULSE / 4 * 3, debugRender);
                validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), Player.JUMP_IMPULSE / 2, debugRender);
                validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), Player.JUMP_IMPULSE / 4, debugRender);
                validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), 0, debugRender);
                debugRender = false;
            }
        }
        return true;
    }

    private static final Color color1 = new Color(115, 24, 225);
    private static final Color color2 = new Color(212, 24, 225);

    private boolean validateJumpProfile(LayoutMarker fromLM, LayoutMarker toLM, DynamicHitBox playerBox, float holdJump, boolean debugRender) {
        simPos = fromLM.pos.copy();
        simVelocity = new VelocityHandler(0, Player.JUMP_IMPULSE);
        if (debugRender && Layout.DEBUG_RENDER)
            debugRenderCircles.add(new RenderGameCircle(RenderOrder.DEBUG, Color.YELLOW, 0.5f, () -> toLM.pos));

        boolean stopForward = false;
        while (simVelocity.y > 0 || simPos.y > toLM.pos.y) {
            Direction direction;
            if (!stopForward) {
                float remainingDistanceToStop = simVelocity.getRemainingDistanceToStop(EXP_Y_DECAY, -DEFAULT_GRAVITY.y, true);
                float remainingTimeToStop = simVelocity.getSignedRemainingTimeToStop(EXP_Y_DECAY, -DEFAULT_GRAVITY.y, true);
                float[] equation = MathHelper.findQuadratic(0, remainingTimeToStop * 2, remainingDistanceToStop);
                if (equation != null) {
                    float[] timeToHitGroundSolutions = MathHelper.solveQuadratic(equation[0], equation[1], simPos.y - toLM.pos.y);
                    if (timeToHitGroundSolutions != null) {
                        float timeToHitGround = timeToHitGroundSolutions[0] < 0 ? timeToHitGroundSolutions[1] : timeToHitGroundSolutions[0];
                        float remainingDist = simVelocity.getOffsetAfterTime(EXP_X_DECAY, LINEAR_X_DECAY, false, timeToHitGround);
                        float dist = toLM.pos.x - simPos.x;

                        if (Math.abs(remainingDist) > Math.abs(dist) && Math.signum(remainingDist) == Math.signum(dist))
                            stopForward = true;
                    }
                }
            }
            if (stopForward) {
                direction = null;
            } else {
                direction = fromLM.pos.x < toLM.pos.x ? Direction.RIGHT : Direction.LEFT;
            }
            if (debugRender && Layout.DEBUG_RENDER && DEBUG_RENDER_SIM) {
                ObjPos debugPos = simPos.copy();
                debugRenderCircles.add(new RenderGameCircle(RenderOrder.DEBUG, stopForward ? color2 : color1, 0.2f, () -> debugPos));
                //debugRenderSquares.add(new RenderGameSquare(color, playerBox));
            }
            simTick(direction, simVelocity.y > holdJump, playerBox);
        }
        return true;
    }

    private static final float DELTA_TIME = 0.03f;

    private void simTick(Direction movement, boolean holdJump, DynamicHitBox playerBox) {
        if (holdJump)
            simVelocity.applyAcceleration(DEFAULT_GRAVITY.copy().divide(-2), DELTA_TIME);
        if (movement == Direction.LEFT)
            simVelocity.applyAcceleration(new ObjPos(-Player.MOVEMENT_ACCELERATION), DELTA_TIME);
        if (movement == Direction.RIGHT)
            simVelocity.applyAcceleration(new ObjPos(Player.MOVEMENT_ACCELERATION), DELTA_TIME);

        simVelocity.tickExponentialXDecay(DELTA_TIME, PhysicsObject.EXP_X_DECAY);
        simVelocity.tickExponentialYDecay(DELTA_TIME, PhysicsObject.EXP_Y_DECAY);
        simVelocity.tickLinearXDecay(DELTA_TIME, PhysicsObject.LINEAR_X_DECAY);
        simVelocity.applyAcceleration(DEFAULT_GRAVITY, DELTA_TIME);
        simPos.add(simVelocity.copy().multiply(DELTA_TIME));

    }

    private ObjPos simPos;
    private VelocityHandler simVelocity;

    private ObjPos getSimPos() {
        return simPos;
    }

    @Override
    public void delete() {
        fromLMs.clear();
        toLMS.clear();
        from = null;
        to = null;
    }

    @Override
    public void render(Graphics2D g) {
        if (!DEBUG_RENDER_SIM)
            return;
        debugRenderCircles.forEach(r -> r.render(g));
        debugRenderSquares.forEach(r -> r.render(g));
    }
}
