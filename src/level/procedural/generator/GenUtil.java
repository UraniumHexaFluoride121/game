package level.procedural.generator;

import foundation.Direction;
import foundation.Main;
import foundation.math.FunctionalWeightedRandom;
import foundation.math.ObjPos;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.movement.LMDPlayerMovement;
import loader.AssetManager;
import physics.StaticHitBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public abstract class GenUtil {
    public static StackData blockStackInward(int initialLeft, int initialRight, boolean inverted, Supplier<Double> random, FunctionalWeightedRandom<Integer, StackRandomData> inward) {
        ArrayList<Integer> left = new ArrayList<>(), right = new ArrayList<>();
        left.add(initialLeft);
        right.add(initialRight);
        while (getLastStackSize(left, right) > 0) {
            int lastLeftElement = left.get(left.size() - 1);
            int lastRightElement = right.get(right.size() - 1);

            left.add(lastLeftElement - inward.getValue(random, new StackRandomData(left.size(), left.size() == 1 ? 0 : left.get(right.size() - 2) - lastLeftElement, lastLeftElement + lastRightElement + 1)));
            right.add(lastRightElement - inward.getValue(random, new StackRandomData(right.size(), right.size() == 1 ? 0 : right.get(right.size() - 2) - lastRightElement, lastLeftElement + lastRightElement + 1)));
        }
        left.remove(left.size() - 1);
        right.remove(right.size() - 1);
        return new StackData(left, right, inverted).setBound(inverted ?
                new StaticHitBox(1, left.size() - 1, initialLeft, initialRight + 1, new ObjPos()) :
                new StaticHitBox(left.size(), 0, initialLeft, initialRight + 1, new ObjPos())
        );
    }

    private static int getLastStackSize(ArrayList<Integer> left, ArrayList<Integer> right) {
        return left.get(left.size() - 1) + right.get(right.size() - 1) + 1;
    }

    public record StackRandomData(int layer, int lastValue, int lastWidth) {
    }

    public static class StackData extends BlockCollection {
        public final ArrayList<Integer> left;
        public final ArrayList<Integer> right;
        public final boolean inverted;

        public StackData(ArrayList<Integer> left, ArrayList<Integer> right, boolean inverted) {
            this.left = left;
            this.right = right;
            this.inverted = inverted;
            forEachLayer((l, r, i) -> {
                for (int j = -l; j <= r; j++) {
                    addBlock(j, inverted ? -i : i);
                }
            });
        }

        @Override
        public StackData setBound(StaticHitBox bound) {
            super.setBound(bound);
            return this;
        }

        @Override
        public StackData calculateBound() {
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

    public static void generateJumpValidation(HashMap<Integer, Integer> blockHeights, ProceduralGenerator gen, LayoutMarker lm, StaticHitBox box, float friction) {
        int lastHeight = -1;
        int from = 0;
        boolean lastWasUp = true;
        LMDPlayerMovement lastJump = null;
        StaticHitBox playerBox = AssetManager.blockHitBoxes.get("player");
        float playerWidth = playerBox.left + playerBox.right;
        for (int x = 0; x <= Main.BLOCKS_X; x++) {
            int height = blockHeights.getOrDefault(x, -1);
            if (lastHeight == -1 || lastHeight != height) {
                if (lastHeight != -1) {
                    lm.addBound(new StaticHitBox(lastHeight + 1 + box.getTop(), lastHeight + box.getTop(), from + 0.3f, x - 0.3f), BoundType.JUMP_VALIDATION);
                }
                int finalLastHeight = lastHeight, finalHeight = height;
                boolean finalLastWasUp = lastWasUp;
                if (lastWasUp && lastJump != null) {
                    lastJump.addAcceleration(lastJump.lm.pos.x - x + (lastHeight > height ? 1 - box.getRight() : box.getLeft() + playerWidth), friction, false);
                }
                if (lastHeight > height) {
                    int finalFrom = from;
                    lastJump = gen.addJumpMarker("static_jump", new ObjPos(x - 1 + box.getRight(), lastHeight + box.getTop()), data -> {
                        if (finalHeight == -1)
                            data.setApproachDirection(Direction.RIGHT);
                        data.addAcceleration(data.lm.pos.x - finalFrom + (finalLastWasUp ? box.getLeft() : -playerWidth + 1 - box.getRight()), friction, true);
                    });
                    lastWasUp = false;
                } else {
                    lastJump = gen.addJumpMarker("static_jump", new ObjPos(x - box.getLeft(), height + box.getTop()), data -> {
                        if (finalLastHeight == -1)
                            data.setApproachDirection(Direction.LEFT);
                    });
                    lastWasUp = true;
                }
                from = x;
                lastHeight = height;
            }
        }
    }

    public static HashMap<Integer, Integer> maxBlockHeights(HashMap<Integer, Integer> a, HashMap<Integer, Integer> b) {
        a.forEach((x, y) -> {
            if (b.containsKey(x)) {
                b.put(x, Math.max(a.get(x), b.get(x)));
            } else
                b.put(x, y);
        });
        return b;
    }
}
