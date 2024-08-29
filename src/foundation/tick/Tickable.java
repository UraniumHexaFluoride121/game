package foundation.tick;

//Used for objects that need to be able to receive game ticks
public interface Tickable {
    void tick(float deltaTime); //deltaTime in seconds
}
