package level.objects;

import foundation.Direction;
import foundation.MainPanel;
import foundation.input.InputEvent;
import foundation.input.InputHandler;
import foundation.input.InputType;
import foundation.math.ObjPos;
import level.Level;
import network.NetworkState;
import network.PacketType;
import network.PacketWriter;
import render.event.RenderEvent;

import java.awt.event.KeyEvent;

public class Player extends PhysicsBlock {
    private boolean space, left, right;
    private boolean isLongJump = false;

    public Player(ObjPos pos, String name, float mass, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight, Level level) {
        super(pos, name, mass, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, level);
    }

    public Player addInput(InputHandler handler) {
        //jump
        handler.addInput(InputType.KEY_PRESSED, e -> {
            if (MainPanel.networkState == NetworkState.CLIENT)
                MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, w -> {
                    PacketWriter.writeEnum(InputEvent.MOVEMENT_UP, w);
                    InputType.KEY_PRESSED.write(w);
                }));
            else
                onSpacePressed();
        }, e -> e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP, InputEvent.MOVEMENT_UP, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            if (MainPanel.networkState == NetworkState.CLIENT)
                MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, w -> {
                    PacketWriter.writeEnum(InputEvent.MOVEMENT_UP, w);
                    InputType.KEY_RELEASED.write(w);
                }));
            else
                onSpaceReleased();
        }, e -> e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP, InputEvent.MOVEMENT_UP, false);

        //left
        handler.addInput(InputType.KEY_PRESSED, e -> {
            if (MainPanel.networkState == NetworkState.CLIENT)
                MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, w -> {
                    PacketWriter.writeEnum(InputEvent.MOVEMENT_LEFT, w);
                    InputType.KEY_PRESSED.write(w);
                }));
            else
                onLeftPressed();
        }, e -> e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT, InputEvent.MOVEMENT_LEFT, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            if (MainPanel.networkState == NetworkState.CLIENT)
                MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, w -> {
                    PacketWriter.writeEnum(InputEvent.MOVEMENT_LEFT, w);
                    InputType.KEY_RELEASED.write(w);
                }));
            else
                onLeftReleased();
        }, e -> e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT, InputEvent.MOVEMENT_LEFT, false);

        //right
        handler.addInput(InputType.KEY_PRESSED, e -> {
            if (MainPanel.networkState == NetworkState.CLIENT)
                MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, w -> {
                    PacketWriter.writeEnum(InputEvent.MOVEMENT_RIGHT, w);
                    InputType.KEY_PRESSED.write(w);
                }));
            else
                onRightPressed();
        }, e -> e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT, InputEvent.MOVEMENT_RIGHT, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            if (MainPanel.networkState == NetworkState.CLIENT)
                MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, w -> {
                    PacketWriter.writeEnum(InputEvent.MOVEMENT_RIGHT, w);
                    InputType.KEY_RELEASED.write(w);
                }));
            else
                onRightReleased();
        }, e -> e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT, InputEvent.MOVEMENT_RIGHT, false);
        return this;
    }

    public void handleInput(InputEvent event, InputType type) {
        switch (event) {
            case MOVEMENT_UP -> {
                if (type == InputType.KEY_PRESSED)
                    onSpacePressed();
                else
                    onSpaceReleased();
            }
            case MOVEMENT_LEFT -> {
                if (type == InputType.KEY_PRESSED)
                    onLeftPressed();
                else
                    onLeftReleased();
            }
            case MOVEMENT_RIGHT -> {
                if (type == InputType.KEY_PRESSED)
                    onRightPressed();
                else
                    onRightReleased();
            }
        }
    }

    private void onSpacePressed() {
        space = true;
    }

    private void onSpaceReleased() {
        space = false;
        isLongJump = false;
    }

    private void onLeftPressed() {
        if (!left) {
            if (right)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_LEFT);
        }
        left = true;
    }

    private void onLeftReleased() {
        if (left) {
            if (right)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_RIGHT);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
        }
        left = false;
    }

    private void onRightPressed() {
        if (!right) {
            if (left)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_RIGHT);
        }
        right = true;
    }

    private void onRightReleased() {
        if (right) {
            if (left)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_LEFT);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
        }
        right = false;
    }


    public static final float MOVEMENT_ACCELERATION = 30, JUMP_IMPULSE = 22.7f;

    public float jumpTimer = 0;

    @Override
    public void processMovement(float deltaTime) {
        if (jumpTimer > 0)
            jumpTimer = Math.max(0, jumpTimer - deltaTime);

        if (space && jumpTimer == 0 && constraints.is(Direction.DOWN)) {
            isLongJump = true;
            jumpTimer = 0.2f;
            applyImpulse(new ObjPos(0, JUMP_IMPULSE));
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
            movement.setLength(MOVEMENT_ACCELERATION);
            applyAcceleration(movement, deltaTime);
        }
        super.processMovement(deltaTime);
    }
}
