package foundation.tick;

public interface Tickable {
    void tick(float deltaTime); //deltaTime in seconds
    TickOrder getTickOrder();
}
