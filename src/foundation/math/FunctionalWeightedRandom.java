package foundation.math;

import foundation.Deletable;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionalWeightedRandom<T, U> implements Deletable {
    private final ArrayList<T> values = new ArrayList<>();
    private final ArrayList<Function<U, Float>> weights = new ArrayList<>();

    public FunctionalWeightedRandom<T, U> add(T value, Function<U, Float> weight) {
        values.add(value);
        weights.add(weight);
        return this;
    }

    public T getValue(Supplier<Double> random, U input) {
        if (values.isEmpty())
            return null;
        float[] calculatedWeights = new float[weights.size()];
        float sum = 0;
        for (int i = 0; i < weights.size(); i++) {
            float calculated = Math.max(0, weights.get(i).apply(input));
            sum += calculated;
            calculatedWeights[i] = calculated;
        }
        float chosen = MathHelper.randFloatBetween(0, sum, random);

        float total = 0;
        for (int i = 0; i < weights.size(); i++) {
            if (total + calculatedWeights[i] > chosen)
                return values.get(i);
            total += calculatedWeights[i];
        }
        return null;
    }

    @Override
    public void delete() {
        values.clear();
        weights.clear();
    }
}
