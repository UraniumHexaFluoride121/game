package level.procedural.collections;

import foundation.math.FunctionalWeightedRandom;
import foundation.math.MathUtil;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class VerticalStackData extends BlockCollection {
    public final int centerUp, centerDown;
    public ArrayList<Integer>
            leftUp = new ArrayList<>(),
            leftDown = new ArrayList<>(),
            rightUp = new ArrayList<>(),
            rightDown = new ArrayList<>();

    public VerticalStackData(int centerUp, int centerDown, Supplier<Double> random, FunctionalWeightedRandom<Integer, StackRandomData> inward) {
        this.centerUp = centerUp;
        this.centerDown = centerDown;
        generateInward(centerUp, centerDown, rightUp, rightDown, random, inward, null);
        generateInward(centerUp, centerDown, leftUp, leftDown, random, inward, null);
        forEachLayer((u, d, l) -> {
            for (int i = -d; i <= u; i++) {
                addBlock(l, i);
            }
        });
        calculateBound();
    }

    public VerticalStackData(int centerUp, int centerDown, Supplier<Double> random, FunctionalWeightedRandom<Integer, StackRandomData> inward, FunctionalWeightedRandom<Integer, StackRandomData> offset) {
        this.centerUp = centerUp;
        this.centerDown = centerDown;
        generateInward(centerUp, centerDown, rightUp, rightDown, random, inward, offset);
        generateInward(centerUp, centerDown, leftUp, leftDown, random, inward, offset);
        forEachLayer((u, d, l) -> {
            for (int i = -d; i <= u; i++) {
                addBlock(l, i);
            }
        });
        removeDisconnected(0, 0);
        calculateBound();
    }

    private void generateInward(int initialUp, int initialDown, ArrayList<Integer> upLayers, ArrayList<Integer> downLayers, Supplier<Double> random, FunctionalWeightedRandom<Integer, StackRandomData> inward, FunctionalWeightedRandom<Integer, StackRandomData> offset) {
        int prevUp = 0, prevDown = 0, prevOffset = 0, up = initialUp, down = initialDown;
        int layer = 0;
        while (true) {
            int newUp = inward.getValue(random, new StackRandomData(layer, prevUp, up + down + 1));
            int newDown = inward.getValue(random, new StackRandomData(layer, prevDown, up + down + 1));
            int newOffset = layer == 0 ? 0 : offset == null ? 0 : offset.getValue(random, new StackRandomData(layer, prevOffset, up + down + 1));
            newOffset = (int) (MathUtil.min(Math.abs(newOffset), newUp, newDown) * Math.signum(newOffset));
            up += newOffset - newUp;
            down += -newOffset - newDown;
            prevUp = newUp;
            prevDown = newDown;
            prevOffset = newOffset;
            if (up + down + 1 <= 0)
                break;
            upLayers.add(layer, up);
            downLayers.add(layer, down);
            layer++;
        }
    }

    @Override
    public VerticalStackData calculateBound() {
        super.calculateBound();
        return this;
    }

    public void forEachLayer(VerticalStackLayerConsumer action) {
        for (int i = -leftUp.size(); i <= rightUp.size(); i++) {
            int finalI = i;
            forLayer(i, (u, d) -> action.accept(u, d, finalI));
        }
    }

    public void forLayer(int layer, BiConsumer<Integer, Integer> action) {
        if (layer == 0)
            action.accept(centerUp, centerDown);
        else if (layer > 0)
            action.accept(rightUp.get(layer - 1), rightDown.get(layer - 1));
        else
            action.accept(leftUp.get(-layer - 1), leftDown.get(-layer - 1));
    }

    @FunctionalInterface
    public interface VerticalStackLayerConsumer {
        void accept(int up, int down, int layer);
    }
}
