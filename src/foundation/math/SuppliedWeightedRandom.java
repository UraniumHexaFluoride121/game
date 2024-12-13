package foundation.math;

import foundation.Deletable;

import java.util.ArrayList;
import java.util.function.Supplier;

public class SuppliedWeightedRandom<T> implements Deletable {
    private final ArrayList<T> values = new ArrayList<>();
    private final ArrayList<Supplier<Float>> weights = new ArrayList<>();

    public SuppliedWeightedRandom<T> add(T value, Supplier<Float> weight) {
        values.add(value);
        weights.add(weight);
        return this;
    }

    public T getValue(Supplier<Double> random) {
        if (values.isEmpty())
            return null;
        float[] calculatedWeights = new float[weights.size()];
        float sum = 0;
        for (int i = 0; i < weights.size(); i++) {
            float calculated = Math.max(0, weights.get(i).get());
            sum += calculated;
            calculatedWeights[i] = calculated;
        }
        float chosen = MathUtil.randFloatBetween(0, sum, random);

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
