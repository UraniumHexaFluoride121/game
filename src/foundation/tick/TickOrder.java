package foundation.tick;

public enum TickOrder {
    //Higher up on this list means it'll get ticked earlier
    PLACEHOLDER,
    PLAYER_INPUT,
    BLOCK_MOVEMENT,
    COLLISION_CHECK,
    ANIMATIONS_ONLY
}
