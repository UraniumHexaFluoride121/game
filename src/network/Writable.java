package network;

import java.io.DataOutputStream;

public interface Writable {
    void write(DataOutputStream writer);
}
