package level;

public enum ObjectLayer {
    FOREGROUND(false, true, "foreground"),
    BACKGROUND(false, true, "background"),
    DYNAMIC(true, false, "dynamic");

    public final boolean addToDynamic, addToStatic;
    public final String s;

    ObjectLayer(boolean addToDynamic, boolean addToStatic, String s) {
        this.addToDynamic = addToDynamic;
        this.addToStatic = addToStatic;
        this.s = s;
    }

    public static ObjectLayer getObjectLayer(String s) {
        for (ObjectLayer layer : ObjectLayer.values()) {
            if (layer.s.equals(s))
                return layer;
        }
        throw new IllegalArgumentException("Unknown object layer: " + s);
    }
}
