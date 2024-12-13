package foundation.math;

import java.util.function.Supplier;

public abstract class MathUtil {
    public static float lerp(float a, float b, float t) {
        return (b - a) * t + a;
    }

    public static float normalise(float min, float max, float v) {
        return (v - min) / (max - min);
    }

    public static float map(float fromMin, float fromMax, float toMin, float toMax, float v) {
        return lerp(toMin, toMax, normalise(fromMin, fromMax, v));
    }

    public static float map(float fromMin, float fromMax, float toMin, float toMax, float v, boolean clamp) {
        float lerped = lerp(toMin, toMax, normalise(fromMin, fromMax, v));
        return clamp ? clamp(toMin, toMax, lerped) : lerped;
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

    public static float[] findQuadratic(float root1, float root2, float ext) {
        float mid = (root1 + root2) / 2;
        float d = Math.abs(mid - root1);
        float p = -mid * 2;
        float q = -(d * d) + (p * p / 4);
        float a = ext / (mid * mid + p * mid + q);
        if (Float.isNaN(a))
            return null;
        return new float[] {
                a, p * a, q * a
        };
    }

    public static float[] findQuadratic(float root1, float root2, float px, float py) {
        float mid = (root1 + root2) / 2;
        float d = Math.abs(mid - root1);
        float p = -mid * 2;
        float q = -(d * d) + (p * p / 4);
        float a = py / (px * px + p * px + q);
        if (Float.isNaN(a))
            return null;
        return new float[] {
                a, p * a, q * a
        };
    }

    public static float[] solveQuadratic(float a, float b, float c) {
        float p = b / a;
        float q = c / a;
        float d = p * p / 4 - q;
        if (d < 0)
            return null;
        return new float[]{
                ((float) (-(p / 2) - Math.sqrt(d))),
                ((float) (-(p / 2) + Math.sqrt(d)))
        };
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

    public static String floatToString(float v, int decimals) {
        char[] chars = String.valueOf(v).toCharArray();
        StringBuilder s = new StringBuilder();
        boolean isDecimal = false;
        for (int i = 0; ; i++) {
            if (isDecimal) {
                for (int j = i; j < i + decimals; j++) {
                    if (j < chars.length) {
                        s.append(chars[j]);
                    } else
                        s.append('0');
                }
                break;
            }
            if (i < chars.length) {
                if (chars[i] == '.') {
                    if (decimals == 0)
                        break;
                    isDecimal = true;
                }
                s.append(chars[i]);
            } else {
                break;
            }
        }
        return s.toString();
    }

    public static String floatToTime(float seconds, int decimals) {
        int minutes = (int) (seconds / 60);
        seconds -= minutes * 60;
        int hours = minutes / 60;
        minutes -= hours * 60;
        StringBuilder s = new StringBuilder();
        if (hours != 0) {
            s.append(hours).append(':');
            if (minutes < 10)
                s.append('0');
        }
        if (minutes != 0) {
            s.append(minutes).append(':');
            if (seconds < 10)
                s.append('0');
        }
        s.append(floatToString(seconds, decimals).replace('.', ':'));
        return s.toString();
    }
}
