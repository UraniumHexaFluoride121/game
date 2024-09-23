package render;

public enum RenderOrder {
    //Higher up on this list means lower Z-order
    SOLID_COLOUR_BACKGROUND("solidColourBackground"),
    BACKGROUND("background"),
    PLAYER("player"),
    BLOCK("block");

    public final String s;

    RenderOrder(String s) {
        this.s = s;
    }

    public static RenderOrder getRenderOrder(String s) {
        for (RenderOrder order : RenderOrder.values()) {
            if (order.s.equals(s))
                return order;
        }
        throw new IllegalArgumentException("Unknown render order: " + s);
    }

    @Override
    public String toString() {
        return s;
    }
}
