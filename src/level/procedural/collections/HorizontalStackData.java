package level.procedural.collections;

import foundation.math.FunctionalWeightedRandom;
import foundation.math.ObjPos;
import physics.StaticHitBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class HorizontalStackData extends BlockCollection {
    public final ArrayList<Integer> left;
    public final ArrayList<Integer> right;
    public final boolean inverted;

    public HorizontalStackData(ArrayList<Integer> left, ArrayList<Integer> right, boolean inverted) {
        this.left = left;
        this.right = right;
        this.inverted = inverted;
        forEachLayer((l, r, i) -> {
            for (int j = -l; j <= r; j++) {
                addBlock(j, inverted ? -i : i);
            }
        });
    }

    public static HorizontalStackData blockStackInward(int initialLeft, int initialRight, boolean inverted, Supplier<Double> random, FunctionalWeightedRandom<Integer, StackRandomData> inward) {
        ArrayList<Integer> left = new ArrayList<>(), right = new ArrayList<>();
        left.add(initialLeft);
        right.add(initialRight);
        while (StackRandomData.getLastStackSize(left, right) > 0) {
            int lastLeftElement = left.get(left.size() - 1);
            int lastRightElement = right.get(right.size() - 1);

            left.add(lastLeftElement - inward.getValue(random, new StackRandomData(left.size(), left.size() == 1 ? 0 : left.get(right.size() - 2) - lastLeftElement, lastLeftElement + lastRightElement + 1)));
            right.add(lastRightElement - inward.getValue(random, new StackRandomData(right.size(), right.size() == 1 ? 0 : right.get(right.size() - 2) - lastRightElement, lastLeftElement + lastRightElement + 1)));
        }
        left.remove(left.size() - 1);
        right.remove(right.size() - 1);
        return new HorizontalStackData(left, right, inverted).setBound(inverted ?
                new StaticHitBox(1, left.size() - 1, initialLeft, initialRight + 1, new ObjPos()) :
                new StaticHitBox(left.size(), 0, initialLeft, initialRight + 1, new ObjPos())
        );
    }

    @Override
    public HorizontalStackData setBound(StaticHitBox bound) {
        super.setBound(bound);
        return this;
    }

    @Override
    public HorizontalStackData calculateBound() {
        super.calculateBound();
        return this;
    }

    public int rightLayer(float percent) {
        return right.get(((int) ((right.size() - 1) * percent)));
    }

    public int leftLayer(float percent) {
        return left.get(((int) ((left.size() - 1) * percent)));
    }

    @Override
    public HashMap<Integer, Integer> getBlockHeights(int x, int y) {
        HashMap<Integer, Integer> blockHeights = new HashMap<>();
        if (inverted) {
            for (int i = -left.get(0); i <= right.get(0); i++) {
                blockHeights.put(i + x, y);
            }
        } else {
            forEachLayer((l, r, layer) -> {
                for (int i = -l; i <= r; i++) {
                    blockHeights.put(i + x, y + layer);
                }
            });
        }
        return blockHeights;
    }

    public void forEachLayer(StackLayerConsumer action) {
        for (int i = 0; i < left.size(); i++) {
            action.accept(left.get(i), right.get(i), i);
        }
    }

    @FunctionalInterface
    public interface StackLayerConsumer {
        void accept(int left, int right, int layer);
    }
}
