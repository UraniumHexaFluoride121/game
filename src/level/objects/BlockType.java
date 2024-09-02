package level.objects;

public enum BlockType {
            PLAYER("player"),
            PHYSICS_BLOCK("physics_block"),
            STATIC_BLOCK("static_block"),
            MOVABLE_BLOCK("movable_block");

    public final String s;

    BlockType(String s) {
        this.s = s;
    }

    public static BlockType getBlockType(String s) {
        for (BlockType type : BlockType.values()) {
            if (type.s.equals(s))
                return type;
        }
        throw new IllegalArgumentException("Unknown block type: " + s);
    }

    @Override
    public String toString() {
        return s;
    }
}
