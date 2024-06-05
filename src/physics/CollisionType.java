package physics;

public enum CollisionType {
    //Determines the performance optimisations used by the collision handler

    //Static objects are expected to be affected by collisions themselves, but
    //dynamic objects can still have collision interactions with them. Their position
    //is not updated, meaning that objects marked as STATIC should never move and therefore
    //have a CollisionBehaviour of IMMOVABLE.
    //Useful for non-moving blocks, for example
    STATIC(false, false),

    //Similar to static objects, they will not collide with other static or movable objects,
    //only with dynamic objects. They do however get position updates, allowing them to be
    //moved.
    //Useful for moving platforms
    MOVABLE(true, false),

    //Dynamic objects get full collision interaction, and collide with all other objects,
    //including other dynamic objects. They also obviously are allowed to move as they receive
    //position updates.
    //Useful for physics objects like the player of pushable blocks
    DYNAMIC(true, true);

    public final boolean requiresPositionUpdates;
    public final boolean interactsDynamically;

    CollisionType(boolean requiresPositionUpdates, boolean interactsDynamically) {
        this.requiresPositionUpdates = requiresPositionUpdates;
        this.interactsDynamically = interactsDynamically;
    }
}
