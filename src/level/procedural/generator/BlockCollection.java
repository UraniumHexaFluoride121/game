package level.procedural.generator;

import foundation.math.ObjPos;
import physics.StaticHitBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class BlockCollection {
    public HashSet<ObjPos> blockPositions = new HashSet<>();
    public StaticHitBox bound;

    public void addBlock(ObjPos pos) {
        blockPositions.add(pos.copy());
    }

    public void addBlock(int x, int y) {
        blockPositions.add(new ObjPos(x, y));
    }

    public void generateBlocks(String blockName, ProceduralGenerator gen) {
        blockPositions.forEach(pos -> gen.addBlock(blockName, pos.copy()));
    }

    public void generateBlocks(String blockName, ObjPos origin, ProceduralGenerator gen) {
        blockPositions.forEach(pos -> gen.addBlock(blockName, pos.copy().add(origin)));
    }

    public BlockCollection setBound(StaticHitBox bound) {
        this.bound = bound;
        return this;
    }

    public BlockCollection calculateBound() {
        if (blockPositions.isEmpty())
            return setBound(new StaticHitBox(0, 0, 0, 0));
        float minX = 0, maxX = 0, minY = 0, maxY = 0;
        boolean init = false;
        for (ObjPos pos : blockPositions) {
            if (init) {
                minX = Math.min(minX, pos.x);
                maxX = Math.max(maxX, pos.x);
                minY = Math.min(minY, pos.y);
                maxY = Math.max(maxY, pos.y);
            } else {
                init = true;
                minX = pos.x;
                maxX = pos.x;
                minY = pos.y;
                maxY = pos.y;
            }
        }
        return setBound(new StaticHitBox(maxY + 1, minY, minX, maxX + 1));
    }

    public int height() {
        return (int) (bound.getTop() - bound.getBottom());
    }

    public int width() {
        return (int) (bound.getRight() - bound.getLeft());
    }

    public int height(float percentage) {
        return (int) (height() * percentage);
    }

    public int width(float percentage) {
        return (int) (width() * percentage);
    }

    public int right() {
        return (int) (bound.getRight() - 1);
    }

    public int left() {
        return (int) (-bound.getLeft());
    }

    public int up() {
        return (int) (bound.getTop() - 1);
    }

    public int down() {
        return (int) (-bound.getBottom());
    }

    public int right(float percentage) {
        return (int) (right() * percentage);
    }

    public int left(float percentage) {
        return (int) (left() * percentage);
    }

    public StaticHitBox getBound(ObjPos origin) {
        return bound.copy().offset(origin);
    }

    public void forEachBlockPos(Consumer<ObjPos> action) {
        blockPositions.forEach(action);
    }

    public HashMap<Integer, Integer> getBlockHeights() {
        return getBlockHeights(0, 0);
    }

    public HashMap<Integer, Integer> getBlockHeights(ObjPos pos) {
        return getBlockHeights((int) pos.x, pos.y);
    }

    public HashMap<Integer, Integer> getBlockHeights(float x, float y) {
        return getBlockHeights((int) x, (int) y);
    }

    public HashMap<Integer, Integer> getBlockHeights(int x, int y) {
        HashMap<Integer, Integer> blockHeights = new HashMap<>();
        forEachBlockPos(pos -> {
            int blockX = ((int) pos.x) + x;
            int blockY = ((int) pos.y) + y;
            if (!blockHeights.containsKey(blockX))
                blockHeights.put(blockX, blockY);
            else
                blockHeights.put(blockX, Math.max(blockHeights.get(blockX), blockY));
        });
        return blockHeights;
    }

    public BlockCollection join(BlockCollection other, int xOffset, int yOffset) {
        BlockCollection c = new BlockCollection().setBound(bound.copy().expandToFit(other.bound.copy().offset(xOffset, yOffset)));
        forEachBlockPos(c::addBlock);
        other.forEachBlockPos(pos -> c.addBlock(pos.copy().add(xOffset, yOffset)));
        return c;
    }
}