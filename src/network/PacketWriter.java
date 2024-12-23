package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

public record PacketWriter(PacketType type, boolean udp, Consumer<DataOutputStream> writer) {
    public static <T extends Enum<T>> void writeEnum(T value, DataOutputStream writer) throws IOException {
        writer.writeInt(value.ordinal());
    }
}
