package level.procedural.collections;

import foundation.math.FunctionalWeightedRandom;
import foundation.math.MathUtil;
import foundation.math.ObjPos;
import foundation.math.WeightedRandom;
import physics.StaticHitBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class IslandCluster extends BlockCollection {
    public final HashSet<StaticHitBox> islandBounds = new HashSet<>();
    public final HashSet<StaticHitBox> expandedIslandBounds = new HashSet<>();
    private final HashMap<HorizontalStackData, ObjPos> islands = new HashMap<>();
    public final int yOffset;

    public IslandCluster(int maxDistance, int minIslands, int maxIslands, Supplier<Double> random, WeightedRandom<Integer> widthSupplier, FunctionalWeightedRandom<Integer, StackRandomData> weights) {
        yOffset = maxDistance * 2 / 3;
        int islandCount = MathUtil.randIntBetween(minIslands, maxIslands, random);
        setBound(null);
        for (int i = 0; i < islandCount; i++) {
            for (int j = 0; j < 6; j++) {
                float angle = MathUtil.randFloatBetween(0, ((float) Math.PI) * 2, random);
                int length = MathUtil.randIntBetween(0, maxDistance, random);
                int width = widthSupplier.getValue(random) - 1;
                ObjPos center = new ObjPos(length).rotate(angle).toInt();
                ObjPos islandPos = center.copy().add(0, yOffset);
                HorizontalStackData island = HorizontalStackData.blockStackInward(width / 2, width / 2 + width % 2, true, random, weights);
                StaticHitBox bound = island.getBound(islandPos);
                float expansion = (width + 1) * (width + 1) / 30f + 0.1f;
                StaticHitBox expandedBound = bound.copy().expand(expansion, expansion);
                boolean colliding = false;
                for (StaticHitBox islandBound : expandedIslandBounds) {
                    if (expandedBound.isColliding(islandBound)) {
                        colliding = true;
                        break;
                    }
                }
                if (colliding)
                    continue;
                islands.put(island, islandPos);
                islandBounds.add(bound);
                expandedIslandBounds.add(expandedBound);
                join(island, islandPos);
                break;
            }
        }
    }

    public void forEachIsland(BiConsumer<? super HorizontalStackData, ObjPos> action) {
        islands.forEach(action);
    }
}
