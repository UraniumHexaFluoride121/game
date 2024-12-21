package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

public record PacketWriter(PacketType type, Consumer<DataOutputStream> writer) {
    public static <T extends Enum<T>> void writeEnum(T value, DataOutputStream writer) {
        try {
            writer.writeInt(value.ordinal());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
