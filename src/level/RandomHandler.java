package level;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Supplier;

public class RandomHandler {
    private final int seed;
    private final Random mainRandom;
    private final HashMap<RandomType, Random> randoms = new HashMap<>();

    public RandomHandler(int seed) {
        this.seed = seed;
        mainRandom = new Random(seed);
        for (RandomType type : RandomType.values()) {
            randoms.put(type, new Random(mainRandom.nextInt()));
        }
    }

    public Random getRandom(RandomType type) {
        return randoms.get(type);
    }

    public Supplier<Double> getDoubleSupplier(RandomType type) {
        return getRandom(type)::nextDouble;
    }

    public Random generateNewRandomSource(RandomType type) {
        return new Random(getRandom(type).nextInt());
    }
}
