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
import level.procedural.marker.resolved.LMDResolvedElement;
import loader.AssetManager;
import physics.CollisionObject;
import physics.DynamicHitBox;
import physics.HitBox;
import physics.StaticHitBox;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameCircle;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.util.HashSet;

import static level.objects.PhysicsObject.*;
import static level.objects.Player.*;
import static level.procedural.Layout.*;

public class JumpSimulation implements Deletable, Renderable {
    private static final float[] HOLD_JUMP = new float[]{
            JUMP_IMPULSE / 5 * 4,
            JUMP_IMPULSE / 5 * 3,
            JUMP_IMPULSE / 5 * 2,
            JUMP_IMPULSE / 5 * 1,
            JUMP_IMPULSE / 5 * 0
    };
    public static final boolean DEBUG_RENDER_SIM = true;
    public LayoutMarker from, to;
    public HashSet<LayoutMarker> fromLMs, toLMS;
    public HashSet<RenderGameCircle> debugRenderCircles = new HashSet<>();
    public HashSet<RenderGameSquare> debugRenderSquares = new HashSet<>();
    public StaticHitBox bound;
    public boolean hasValidJump = false;

    public JumpSimulation(LayoutMarker from, LayoutMarker to, HashSet<LayoutMarker> fromLMs, HashSet<LayoutMarker> toLMS) {
        this.from = from;
        this.to = to;
        this.fromLMs = fromLMs;
        this.toLMS = toLMS;
    }

    public void addToLM() {
        if (to.data instanceof LMDResolvedElement toData) {
            toData.jumps.add(this);
        }
    }

    public boolean validateJump() {
        if (DEBUG_RENDER_SIM && DEBUG_RENDER) clearDebugRender();
        hasValidJump = false;
        bound = null;
        HashSet<LayoutMarker> collisionMarkers = new HashSet<>();
        MainPanel.level.layout.forEachMarker(from.pos.y, 1, lm -> {
            if (lm.hasBoundType(BoundType.COLLISION))
                collisionMarkers.add(lm);
        });
        StaticHitBox playerStaticBox = AssetManager.blockHitBoxes.get("player");
        DynamicHitBox playerBox = new DynamicHitBox(playerStaticBox.up, playerStaticBox.down, playerStaticBox.left, playerStaticBox.right, this::getSimPos);
        int validatedCount = 0;
        for (LayoutMarker fromLM : fromLMs) {
            for (LayoutMarker toLM : toLMS) {
                LMDPlayerMovement data = ((LMDPlayerMovement) fromLM.data);
                for (float v : data.acceleration) {
                    for (float holdJump : HOLD_JUMP) {
                        for (ProfileType profileType : ProfileType.values()) {
                            JumpProfile profile = new JumpProfile(holdJump, v, profileType);
                            ValidationResult validationResult = validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), collisionMarkers, profile);
                            if (validationResult.validated) {
                                if (!hasValidJump) {
                                    bound = validationResult.bound;
                                    hasValidJump = true;
                                    if (DEBUG_RENDER && DEBUG_RENDER_SIM)
                                        debugRenderSquares.add(new RenderGameSquare(new Color(34, 228, 178), bound));
                                }
                                validatedCount++;
                            }
                        }
                    }
                }
            }
        }
        if (validatedCount > 0/* && validatedCount < 12*/)
            return true;
        clearDebugRender();
        return false;
    }

    private void clearDebugRender() {
        debugRenderSquares.clear();
        debugRenderCircles.clear();
    }

    private static final Color color1 = new Color(115, 24, 225);
    private static final Color color2 = new Color(212, 24, 225);
    private static final Color color3 = new Color(24, 111, 225);
    private static final Color color4 = new Color(24, 225, 208);

    private ValidationResult validateJumpProfile(LayoutMarker fromLM, LayoutMarker toLM, DynamicHitBox playerBox, HashSet<LayoutMarker> collisionMarkers, JumpProfile profile) {
        if (!hasValidJump && DEBUG_RENDER && DEBUG_RENDER_SIM) clearDebugRender();

        LMDPlayerMovement data = ((LMDPlayerMovement) toLM.data);

        if (profile.type == ProfileType.AWAY_UNTIL_ABOVE && data.approachDirection == null)
            return FAIL;

        simPos = fromLM.pos.copy();
        simVelocity = new VelocityHandler(profile.initialVelocity, JUMP_IMPULSE);
        boolean stopForward = profile.type == ProfileType.AWAY_UNTIL_ABOVE;

        boolean forwardEnabled = false;
        StaticHitBox bound = new StaticHitBox(playerBox);

        if (!MainPanel.level.collisionHandler.getBoxCollidingWith(playerBox).isEmpty())
            return FAIL;

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
                if (playerBox.getBottom() > toLM.pos.y && !forwardEnabled) {
                    forwardEnabled = true;
                    stopForward = false;
                }
                if (profile.type == ProfileType.AWAY_UNTIL_ABOVE && playerBox.getBottom() < toLM.pos.y) {
                    if (data.approachDirection == Direction.LEFT) {
                        if (playerBox.getRight() > toLM.pos.x)
                            direction = Direction.LEFT;
                        else
                            direction = null;
                    } else {
                        if (playerBox.getLeft() < toLM.pos.x)
                            direction = Direction.RIGHT;
                        else
                            direction = null;
                    }
                } else
                    direction = null;
            } else {
                direction = simPos.x < toLM.pos.x ? Direction.RIGHT : Direction.LEFT;
            }
            if (!hasValidJump && DEBUG_RENDER && DEBUG_RENDER_SIM) {
                ObjPos debugPos = simPos.copy();
                debugRenderCircles.add(new RenderGameCircle(RenderOrder.DEBUG, (profile.initialVelocity == 0) ? (stopForward ? color2 : color1) : (stopForward ? color4 : color3), 0.2f, () -> debugPos));
                //debugRenderSquares.add(new RenderGameSquare(color, playerBox));
            }
            simTick(direction, simVelocity.y > profile.holdJump);

            HashSet<Direction> collisionDirections = simCollision(playerBox, collisionMarkers);

            bound.expandToFit(playerBox);

            if (to.isBoxColliding(playerBox, BoundType.JUMP_VALIDATION))
                return new ValidationResult(true, bound);

            if (collisionDirections != null) {
                if (collisionDirections.contains(Direction.DOWN))
                    return FAIL;
                if (!collisionDirections.isEmpty())
                    stopForward = false;
            }
        }
        return FAIL;
    }

    private static final float DELTA_TIME = 0.03f;

    private HashSet<Direction> simCollision(DynamicHitBox playerBox, HashSet<LayoutMarker> collisionMarkers) {
        boolean lmColliding = false;
        for (LayoutMarker collisionMarker : collisionMarkers) {
            if (collisionMarker.isBoxColliding(playerBox, BoundType.COLLISION)) {
                lmColliding = true;
                break;
            }
        }
        if (!lmColliding)
            return null;
        HashSet<Direction> directions = new HashSet<>();
        HashSet<CollisionObject> objects = MainPanel.level.collisionHandler.getBoxCollidingWith(playerBox);
        for (CollisionObject object : objects) {
            HitBox otherBox = object.getHitBox();
            if (!playerBox.isColliding(otherBox))
                continue;
            ObjPos overlap = playerBox.collisionOverlap(otherBox);
            if (Math.abs(overlap.y * simVelocity.x) < Math.abs(overlap.x * simVelocity.y)) {
                if (Math.signum(overlap.y) == Math.signum(simVelocity.y) || simVelocity.y == 0) {
                    simVelocity.y = 0;
                }
                if (overlap.y < 0) {
                    simPos.y = otherBox.getTop() + playerBox.down;
                    directions.add(Direction.DOWN);
                } else {
                    simPos.y = otherBox.getBottom() - playerBox.up;
                    directions.add(Direction.UP);
                }
            } else {
                if (Math.signum(overlap.x) == Math.signum(simVelocity.x) || simVelocity.x == 0) {
                    simVelocity.x = 0;
                }
                if (overlap.x < 0) {
                    simPos.x = otherBox.getRight() + playerBox.left;
                    directions.add(Direction.LEFT);
                } else {
                    simPos.x = otherBox.getLeft() - playerBox.right;
                    directions.add(Direction.RIGHT);
                }
            }
        }
        return directions;
    }

    private void simTick(Direction movement, boolean holdJump) {
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

    private static final ValidationResult FAIL = new ValidationResult(false, null);

    private record ValidationResult(boolean validated, StaticHitBox bound) {
    }

    private enum ProfileType {
        FORWARD, AWAY_UNTIL_ABOVE
    }

    private record JumpProfile(float holdJump, float initialVelocity, ProfileType type) {
    }
}
