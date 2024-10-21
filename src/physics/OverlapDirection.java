package physics;

public enum OverlapDirection {
    POSITIVE(1), NEGATIVE(-1), NONE(0);

    private final float sign;

    OverlapDirection(float sign) {
        this.sign = sign;
    }

    public float getSign() {
        return sign;
    }
}
