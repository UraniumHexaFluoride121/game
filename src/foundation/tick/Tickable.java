package foundation.tick;

import foundation.Deletable;

public interface Tickable extends Deletable {
    void tick(float deltaTime); //deltaTime in seconds
    TickOrder getTickOrder();
}
