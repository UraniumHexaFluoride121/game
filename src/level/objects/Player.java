package level.objects;

import foundation.Direction;
import foundation.math.ObjPos;
import foundation.input.InputHandler;
import foundation.input.InputHandlingOrder;
import foundation.input.InputType;
import render.event.RenderEvent;

import java.awt.event.KeyEvent;

public class Player extends PhysicsBlock {
    private boolean space, left, right;
    private boolean isLongJump = false;

    public Player(ObjPos pos, String name, float mass, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight, InputHandler handler) {
        super(pos, name, mass, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight);
        handler.addInput(InputType.KEY_PRESSED, e -> {
            space = true;
        }, e -> e.getKeyCode() == KeyEvent.VK_SPACE, InputHandlingOrder.MOVEMENT_UP, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            space = false;
            isLongJump = false;
        }, e -> e.getKeyCode() == KeyEvent.VK_SPACE, InputHandlingOrder.MOVEMENT_UP, false);

        handler.addInput(InputType.KEY_PRESSED, e -> {
            if (!left) {
                if (right)
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
                else
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_LEFT);
            }
            left = true;
        }, e -> e.getKeyCode() == KeyEvent.VK_A, InputHandlingOrder.MOVEMENT_LEFT, false);

        handler.addInput(InputType.KEY_PRESSED, e -> {
            if (!right) {
                if (left)
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
                else
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_RIGHT);
            }
            right = true;
        }, e -> e.getKeyCode() == KeyEvent.VK_D, InputHandlingOrder.MOVEMENT_RIGHT, false);

        handler.addInput(InputType.KEY_RELEASED, e -> {
            if (left) {
                if (right)
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_RIGHT);
                else
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
            }
            left = false;
        }, e -> e.getKeyCode() == KeyEvent.VK_A, InputHandlingOrder.MOVEMENT_LEFT, false);

        handler.addInput(InputType.KEY_RELEASED, e -> {
            if (right) {
                if (left)
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_LEFT);
                else
                    renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
            }
            right = false;
        }, e -> e.getKeyCode() == KeyEvent.VK_D, InputHandlingOrder.MOVEMENT_RIGHT, false);
    }



    public float jumpTimer = 0;

    @Override
    public void processMovement(float deltaTime) {
        if (jumpTimer > 0)
            jumpTimer = Math.max(0, jumpTimer - deltaTime);

        if (space && jumpTimer == 0 &&  constraints.is(Direction.DOWN)) {
            isLongJump = true;
            jumpTimer = 0.2f;
            applyImpulse(new ObjPos(0, 15));
            renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_JUMP);
        }
        if (velocity.y < 0)
            isLongJump = false;
        if (isLongJump)
            velocity.applyAcceleration(DEFAULT_GRAVITY.copy().divide(-2), deltaTime);

        ObjPos movement = new ObjPos();
        if (left && !right)
            movement.addX(-1);
        if (!left && right)
            movement.addX(1);
        if (movement.length() > 0) {
            movement.normalise().multiply(20);
            applyAcceleration(movement, deltaTime);
        }
        super.processMovement(deltaTime);
    }
}
