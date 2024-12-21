package network;

import java.io.DataOutputStream;

public interface Writable<T> {
    void write(DataOutputStream writer);
}
