package level.objects;

import foundation.Direction;
import foundation.MainPanel;
import foundation.input.InputEvent;
import foundation.input.InputHandler;
import foundation.input.InputType;
import foundation.math.ObjPos;
import level.Level;
import network.PacketType;
import network.PacketWriter;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;

import java.awt.event.KeyEvent;
import java.awt.image.RescaleOp;
import java.io.IOException;

public class Player extends PhysicsBlock {
    private static final RescaleOp[] TEXTURE_COLOURS = new RescaleOp[]{
            new RescaleOp(new float[]{0.7f, 1.1f, 1.1f}, new float[]{0, 0, 0}, null), //cyan
            new RescaleOp(new float[]{0.8f, 1.1f, 0.8f}, new float[]{0, 0, 0}, null), //green
            new RescaleOp(new float[]{1.1f, 0.8f, 0.8f}, new float[]{0, 0, 0}, null), //red
            new RescaleOp(new float[]{0.8f, 0.8f, 1.1f}, new float[]{0, 0, 0}, null), //blue
            new RescaleOp(new float[]{1.1f, 1.1f, 0.7f}, new float[]{0, 0, 0}, null), //yellow
            new RescaleOp(new float[]{1.1f, 0.7f, 1.1f}, new float[]{0, 0, 0}, null), //pink
    };

    public RescaleOp colour;

    public boolean space, left, right;
    public long timeSpace = 0, timeLeft = 0, timeRight = 0;
    private boolean isLongJump = false;

    private boolean hasInput = false;

    public Player(ObjPos pos, String name, float mass, float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight, Level level) {
        super(pos, name, mass, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, level);
    }

    public Player addInput(InputHandler handler) {
        if (hasInput)
            return this;
        hasInput = true;
        //jump
        handler.addInput(InputType.KEY_PRESSED, e -> {
            handleInput(InputEvent.MOVEMENT_UP, InputType.KEY_PRESSED);
        }, e -> e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP, InputEvent.MOVEMENT_UP, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            handleInput(InputEvent.MOVEMENT_UP, InputType.KEY_RELEASED);
        }, e -> e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP, InputEvent.MOVEMENT_UP, false);

        //left
        handler.addInput(InputType.KEY_PRESSED, e -> {
            handleInput(InputEvent.MOVEMENT_LEFT, InputType.KEY_PRESSED);
        }, e -> e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT, InputEvent.MOVEMENT_LEFT, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            handleInput(InputEvent.MOVEMENT_LEFT, InputType.KEY_RELEASED);
        }, e -> e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT, InputEvent.MOVEMENT_LEFT, false);

        //right
        handler.addInput(InputType.KEY_PRESSED, e -> {
            handleInput(InputEvent.MOVEMENT_RIGHT, InputType.KEY_PRESSED);
        }, e -> e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT, InputEvent.MOVEMENT_RIGHT, false);
        handler.addInput(InputType.KEY_RELEASED, e -> {
            handleInput(InputEvent.MOVEMENT_RIGHT, InputType.KEY_RELEASED);
        }, e -> e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT, InputEvent.MOVEMENT_RIGHT, false);
        return this;
    }

    public static void sendClientInput(InputType type, KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_SPACE, KeyEvent.VK_W, KeyEvent.VK_UP -> sendMovementPacket(type, InputEvent.MOVEMENT_UP);
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> sendMovementPacket(type, InputEvent.MOVEMENT_LEFT);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> sendMovementPacket(type, InputEvent.MOVEMENT_RIGHT);
        }
    }

    public void sendMovementPacketUpdate(InputEvent event) {
        sendMovementPacket(switch (event) {
            case MOVEMENT_UP -> space;
            case MOVEMENT_LEFT -> left;
            case MOVEMENT_RIGHT -> right;
            default -> throw new RuntimeException();
        } ? InputType.KEY_PRESSED : InputType.KEY_RELEASED, event);
    }

    public static void sendMovementPacket(InputType type, InputEvent event) {
        MainPanel.sendClientPacket(new PacketWriter(PacketType.PLAYER_MOVEMENT, false, w -> {
            try {
                w.writeLong(System.currentTimeMillis());
                PacketWriter.writeEnum(event, w);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            type.write(w);
        }));
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
        timeSpace = System.currentTimeMillis();
        space = true;
    }

    private void onSpaceReleased() {
        timeSpace = System.currentTimeMillis();
        space = false;
    }

    private void onLeftPressed() {
        timeLeft = System.currentTimeMillis();
        if (!left) {
            if (right)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_LEFT);
        }
        left = true;
    }

    private void onLeftReleased() {
        timeLeft = System.currentTimeMillis();
        if (left) {
            if (right)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_RIGHT);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
        }
        left = false;
    }

    private void onRightPressed() {
        timeRight = System.currentTimeMillis();
        if (!right) {
            if (left)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_RIGHT);
        }
        right = true;
    }

    private void onRightReleased() {
        timeRight = System.currentTimeMillis();
        if (right) {
            if (left)
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_LEFT);
            else
                renderElement.onEvent(RenderEvent.ON_PLAYER_INPUT_STANDING_STILL);
        }
        right = false;
    }

    public synchronized void updateColour(int id) {
        colour = TEXTURE_COLOURS[id % TEXTURE_COLOURS.length];
        renderElement.onEvent(new RenderBlockUpdate(RenderEvent.PLAYER_COLOUR_UPDATE, this));
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
        if (!space)
            isLongJump = false;
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
