package foundation.math;

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

    public static float randFloatBetween(float min, float max, Supplier<Double> random) {
        return (float) (random.get() * (max - min) + min);
    }

    public static boolean randBoolean(float probability, Supplier<Double> random) {
        return random.get() < probability;
    }

    public static float[] solveCubic(float a, float b, float c, float d) {
        float p = c / a, q = d / a;
        float P = p - (b * b) / (3 * a * a), Q = q - (b * c) / (3 * a * a) + (2 * b * b * b) / (27 * a * a * a);
        float D = (Q * Q) / 4 + (P * P * P) / 27;
        float t = -b / (3 * a);

        if (Math.abs(D) < 0.00001)
            D = 0;
        if (D > 0) {
            return new float[]{
                    ((float) (Math.cbrt(-Q / 2 + Math.sqrt(D)) + Math.cbrt(-Q / 2 - Math.sqrt(D)))) + t
            };
        } else if (D == 0) {
            return new float[]{
                    -((float) Math.cbrt(-Q / 2)) + t,
                    2 * ((float) Math.cbrt(-Q / 2)) + t
            };
        } else {
            float P3 = P / 3;
            float angle = ((float) Math.acos((-Q / 2) / Math.sqrt(-P3 * P3 * P3)) / 3);
            return new float[]{
                    ((float) (2 * Math.sqrt(-P3) * Math.cos(angle))) + t,
                    ((float) (2 * Math.sqrt(-P3) * Math.cos(angle + 2 * Math.PI / 3))) + t,
                    ((float) (2 * Math.sqrt(-P3) * Math.cos(angle + 4 * Math.PI / 3))) + t
            };
        }
    }
}
