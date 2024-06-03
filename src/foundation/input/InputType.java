package foundation.input;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class InputType<T extends InputEvent> {
    public static final InputType<KeyEvent> KEY_PRESSED = new InputType<>();
    public static final InputType<KeyEvent> KEY_RELEASED = new InputType<>();

    public static InputType[] values() {
        return new InputType[]{
                KEY_PRESSED,
                KEY_RELEASED
        };
    }
}
