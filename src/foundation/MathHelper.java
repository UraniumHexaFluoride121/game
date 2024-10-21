package foundation;

import java.util.function.Supplier;

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

    public static int clampInt(int min, int max, int value) {
        return Math.min(max, Math.max(min, value));
    }

    public static int randIntBetween(int min, int max, Supplier<Double> random) {
        int realMin = Math.min(min, max), realMax = Math.max(min, max);
        return ((int) Math.floor(lerp(realMin, realMax + 1, random.get().floatValue())));
    }

    public static boolean randBoolean(float probability, Supplier<Double> random) {
        return random.get() < probability;
    }
}
