package foundation.tick;

import foundation.Deletable;
import foundation.Main;

//Used for Tickables when they are intended to be registered directly to the Tick object
public interface RegisteredTickable extends OrderedTickable, Deletable {
    default void registerTickable() {
        Main.TICK.register(this);
    }

    default void removeTickable() {
        Main.TICK.remove(this);
    }
}
