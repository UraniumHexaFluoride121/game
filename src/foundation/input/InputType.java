package foundation.input;

import network.Writable;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InputType<T extends InputEvent> implements Writable<InputType<T>> {
    public static final InputType<KeyEvent> KEY_PRESSED = new InputType<>(0, "KEY_PRESSED");
    public static final InputType<KeyEvent> KEY_RELEASED = new InputType<>(1, "KEY_RELEASED");

    private final int index;
    public final String name;

    public InputType(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public static InputType[] values() {
        return new InputType[]{
                KEY_PRESSED,
                KEY_RELEASED
        };
    }

    @Override
    public void write(DataOutputStream writer) {
        try {
            writer.writeInt(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static InputType read(DataInputStream reader) {
        try {
            return values()[reader.readInt()];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
