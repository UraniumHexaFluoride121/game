package foundation.tick;

public enum TickOrder {
    //Higher up on this list means it'll get ticked earlier
    PLACEHOLDER,
    COLLISION_PHYSICS,
    PLAYER_INPUT,
    ANIMATIONS_ONLY
}
