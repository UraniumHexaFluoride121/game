package render;

public enum RenderOrder {
    //Higher up on this list means lower Z-order
    SOLID_COLOUR_BACKGROUND("solidColourBackground"),
    BACKGROUND("background"),
    GRASS_BACK("grass_back"),
    PLAYER("player"),
    BLOCK("block"),
    GRASS_FRONT("grass_front"),
    UI("ui"),
    DEBUG("debug"); //Debug is not accessible through json

    public final String s;

    RenderOrder(String s) {
        this.s = s;
    }

    public static RenderOrder getRenderOrder(String s) {
        for (RenderOrder order : RenderOrder.values()) {
            if (order.s.equals(s) && !order.s.equals("debug"))
                return order;
        }
        throw new IllegalArgumentException("Unknown render order: " + s);
    }

    @Override
    public String toString() {
        return s;
    }
}
