package network;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Consumer;

public record PacketReceiver(PacketType type, Consumer<DataInputStream> reader) {
    public static <T extends Enum<T>> T readEnum(Class<T> clazz, DataInputStream reader) {
        try {
            return clazz.getEnumConstants()[reader.readInt()];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
