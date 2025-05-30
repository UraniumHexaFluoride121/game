package level.procedural.jump;

import foundation.Deletable;
import foundation.Direction;
import foundation.VelocityHandler;
import foundation.math.MathUtil;
import foundation.math.ObjPos;
import level.Level;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.movement.LMDPlayerMovement;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static level.objects.PhysicsObject.*;
import static level.objects.Player.*;
import static level.procedural.Layout.*;

public class JumpSimulation implements Deletable, Renderable {
    private static final float DELTA_TIME = 0.01f;
    public static final float MAXIMUM_JUMP_HEIGHT = VelocityHandler.getMaximumHeight(JUMP_IMPULSE, EXP_Y_DECAY, 0, DEFAULT_GRAVITY.y / 2);
    private static final float[] HOLD_JUMP = new float[]{
            JUMP_IMPULSE / 5 * 4,
            JUMP_IMPULSE / 5 * 3,
            JUMP_IMPULSE / 5 * 2,
            JUMP_IMPULSE / 5 * 1,
            JUMP_IMPULSE / 5 * 0
    };
    public JumpSimGroup from, to;
    public ArrayList<LayoutMarker> fromLMs, toLMS;
    public Set<RenderGameCircle> debugRenderCircles = ConcurrentHashMap.newKeySet();
    public Set<RenderGameSquare> debugRenderSquares = ConcurrentHashMap.newKeySet();
    public StaticHitBox bound;
    public boolean hasValidJump = false;
    public boolean validatedJumpHadCollision = true;
    public int validatedCount = 0;

    public JumpSimulation(JumpSimGroup from, JumpSimGroup to, ArrayList<LayoutMarker> fromLMs, ArrayList<LayoutMarker> toLMS) {
        this.from = from;
        this.to = to;
        this.fromLMs = fromLMs;
        this.toLMS = toLMS;
    }

    public void addFromGroup() {
        from.jumps.put(this, to);
    }

    public boolean validateJump(Level level) {
        validatedJumpHadCollision = true;
        if (DEBUG_RENDER_SIM && DEBUG_RENDER) clearDebugRender();
        hasValidJump = false;
        bound = null;
        StaticHitBox playerStaticBox = AssetManager.blockHitBoxes.get("player");
        DynamicHitBox playerBox = new DynamicHitBox(playerStaticBox.up, playerStaticBox.down, playerStaticBox.left, playerStaticBox.right, this::getSimPos);
        validatedCount = 0;
        for (LayoutMarker fromLM : fromLMs) {
            for (LayoutMarker toLM : toLMS) {
                if (toLM.pos.y - fromLM.pos.y > MAXIMUM_JUMP_HEIGHT)
                    continue;
                LMDPlayerMovement data = ((LMDPlayerMovement) fromLM.data);
                for (float v : data.acceleration) {
                    for (float holdJump : HOLD_JUMP) {
                        for (ProfileType profileType : ProfileType.values()) {
                            JumpProfile profile = new JumpProfile(holdJump, v, profileType);
                            ValidationResult validationResult = validateJumpProfile(fromLM, toLM, playerBox.centerOrigin().originToBottom(), profile, level);
                            if (validationResult.validated) {
                                if (!hasValidJump) {
                                    bound = validationResult.bound;
                                    hasValidJump = true;
                                    if (DEBUG_RENDER && DEBUG_RENDER_SIM && DEBUG_RENDER_JUMP_BOUNDS)
                                        debugRenderSquares.add(new RenderGameSquare(new Color(34, 228, 178), bound));
                                }
                                validatedCount++;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        if (validatedCount > 0/* && validatedCount < 15*/)
            return true;
        hasValidJump = false;
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

    private ValidationResult validateJumpProfile(LayoutMarker fromLM, LayoutMarker toLM, DynamicHitBox playerBox, JumpProfile profile, Level level) {
        if (!hasValidJump && DEBUG_RENDER && DEBUG_RENDER_SIM) clearDebugRender();

        LMDPlayerMovement data = ((LMDPlayerMovement) toLM.data);

        if (profile.type == ProfileType.AWAY_UNTIL_ABOVE && data.approachDirection == null)
            return FAIL;

        simPos = fromLM.pos.copy();
        simVelocity = new VelocityHandler(profile.initialVelocity, Math.min(JUMP_IMPULSE, MAX_VELOCITY));
        boolean stopForward = profile.type == ProfileType.AWAY_UNTIL_ABOVE;

        boolean forwardEnabled = false;
        StaticHitBox bound = new StaticHitBox(playerBox);

        if (!level.collisionHandler.getBoxCollidingWith(playerBox).isEmpty())
            return FAIL;

        boolean hasHadCollision = false;

        while (simVelocity.y > 0 || simPos.y > toLM.pos.y) {
            Direction direction;
            if (!stopForward) {
                float remainingDistanceToStop = simVelocity.getRemainingDistanceToStop(EXP_Y_DECAY, -DEFAULT_GRAVITY.y, true);
                float remainingTimeToStop = simVelocity.getSignedRemainingTimeToStop(EXP_Y_DECAY, -DEFAULT_GRAVITY.y, true);
                float[] equation = MathUtil.findQuadratic(0, remainingTimeToStop * 2, remainingDistanceToStop);
                if (equation != null) {
                    float[] timeToHitGroundSolutions = MathUtil.solveQuadratic(equation[0], equation[1], simPos.y - toLM.pos.y);
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
                if (playerBox.getTop() > toLM.pos.y && !forwardEnabled) {
                    forwardEnabled = true;
                    stopForward = false;
                }
                if (profile.type == ProfileType.AWAY_UNTIL_ABOVE && playerBox.getTop() < toLM.pos.y) {
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

            HashSet<Direction> collisionDirections = simCollision(playerBox, level);

            bound.expandToFit(playerBox);

            if (to.isValidated(playerBox)) {
                if (!hasHadCollision)
                    validatedJumpHadCollision = false;
                return new ValidationResult(true, bound);
            }

            if (collisionDirections.contains(Direction.DOWN))
                return FAIL;
            if (!collisionDirections.isEmpty()) {
                hasHadCollision = true;
                stopForward = false;
            }
        }
        return FAIL;
    }

    private HashSet<Direction> simCollision(DynamicHitBox playerBox, Level level) {
        HashSet<Direction> directions = new HashSet<>();
        HashSet<CollisionObject> objects = level.collisionHandler.getBoxCollidingWith(playerBox);
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
            simVelocity.applyAcceleration(new ObjPos(-MOVEMENT_ACCELERATION), DELTA_TIME);
        if (movement == Direction.RIGHT)
            simVelocity.applyAcceleration(new ObjPos(MOVEMENT_ACCELERATION), DELTA_TIME);

        simVelocity.tickExponentialXDecay(DELTA_TIME, EXP_X_DECAY);
        simVelocity.tickExponentialYDecay(DELTA_TIME, EXP_Y_DECAY);
        simVelocity.tickLinearXDecay(DELTA_TIME, LINEAR_X_DECAY);
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
