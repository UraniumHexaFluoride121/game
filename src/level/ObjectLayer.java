package level;

public enum ObjectLayer {
    MAIN(false, true),
    DYNAMIC(true, false);

    public final boolean addToDynamic, addToStatic;

    ObjectLayer(boolean addToDynamic, boolean addToStatic) {
        this.addToDynamic = addToDynamic;
        this.addToStatic = addToStatic;
    }
}
