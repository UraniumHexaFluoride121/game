package foundation.tick;

//Used for tickables that need to be able to be sorted by TickOrder
public interface OrderedTickable extends Tickable {
    TickOrder getTickOrder();
}
