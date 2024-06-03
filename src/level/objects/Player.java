package level.objects;

import foundation.ObjPos;
import foundation.input.InputHandler;
import foundation.input.InputHandlingOrder;
import foundation.input.InputType;

import java.awt.*;
import java.awt.event.KeyEvent;

public class Player extends PhysicsBlock {
    public boolean up, down, left, right;

    public Player(ObjPos pos, Color color, InputHandler handler) {
        super(pos, color);
        handler.addInput(InputType.KEY_PRESSED, e -> up = true, e -> e.getKeyCode() == KeyEvent.VK_W, InputHandlingOrder.MOVEMENT_UP, false);
        handler.addInput(InputType.KEY_PRESSED, e -> down = true, e -> e.getKeyCode() == KeyEvent.VK_S, InputHandlingOrder.MOVEMENT_DOWN, false);
        handler.addInput(InputType.KEY_PRESSED, e -> left = true, e -> e.getKeyCode() == KeyEvent.VK_A, InputHandlingOrder.MOVEMENT_LEFT, false);
        handler.addInput(InputType.KEY_PRESSED, e -> right = true, e -> e.getKeyCode() == KeyEvent.VK_D, InputHandlingOrder.MOVEMENT_RIGHT, false);

        handler.addInput(InputType.KEY_RELEASED, e -> up = false, e -> e.getKeyCode() == KeyEvent.VK_W, InputHandlingOrder.MOVEMENT_UP, false);
        handler.addInput(InputType.KEY_RELEASED, e -> down = false, e -> e.getKeyCode() == KeyEvent.VK_S, InputHandlingOrder.MOVEMENT_DOWN, false);
        handler.addInput(InputType.KEY_RELEASED, e -> left = false, e -> e.getKeyCode() == KeyEvent.VK_A, InputHandlingOrder.MOVEMENT_LEFT, false);
        handler.addInput(InputType.KEY_RELEASED, e -> right = false, e -> e.getKeyCode() == KeyEvent.VK_D, InputHandlingOrder.MOVEMENT_RIGHT, false);
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        ObjPos movement = new ObjPos();

        if (up && !down)
            movement.addY(1);
        if (!up && down)
            movement.addY(-1);
        if (left && !right)
            movement.addX(-1);
        if (!left && right)
            movement.addX(1);
        if (movement.length() > 0) {
            movement.normalise().multiply(20);
            applyAcceleration(movement, deltaTime);
        }
    }
}
