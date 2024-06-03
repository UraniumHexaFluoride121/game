package foundation.tick;

import foundation.Deletable;
import foundation.Main;

public interface Tickable extends Deletable {
    void tick(float deltaTime); //deltaTime in seconds
    TickOrder getTickOrder();

    default void registerTickable() {
        Main.TICK.register(this);
    }

    default void removeTickable() {
        Main.TICK.remove(this);
    }
}
