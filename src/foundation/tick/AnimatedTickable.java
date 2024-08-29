package foundation.tick;

//Has extra methods invoked on events that can be used for animation purposes
public interface AnimatedTickable extends Tickable {
    void onSwitchTo();
}
