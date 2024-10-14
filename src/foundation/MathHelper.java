package foundation;

public abstract class MathHelper {
    public static float lerp(float a, float b, float t) {
        return (b - a) * t + a;
    }

    public static float normalise(float min, float max, float v) {
        return (v - min) / (max - min);
    }

    public static float clamp(float min, float max, float value) {
        return Math.min(max, Math.max(min, value));
    }
}
