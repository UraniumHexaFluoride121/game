package level.procedural.collections;

import foundation.Direction;
import foundation.Main;
import foundation.math.ObjPos;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.jump.JumpSimGroup;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.movement.LMDPlayerMovement;
import loader.AssetManager;
import physics.StaticHitBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockCollection {
    public HashSet<ObjPos> blockPositions = new HashSet<>();
    public StaticHitBox bound;

    public BlockCollection(HashSet<ObjPos> blockPositions) {
        this.blockPositions = blockPositions;
    }

    public BlockCollection() {
    }

    public static void generateJumpValidation(HashMap<Integer, Integer> blockHeights, ProceduralGenerator gen, LayoutMarker lm, StaticHitBox box, float friction) {
        JumpSimGroup group = gen.newJumpSimGroup(lm);
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
                    group.addBound(new StaticHitBox(lastHeight + 1 + box.getTop(), lastHeight + box.getTop(), from + 0.3f, x - 0.3f));
                }
                int finalLastHeight = lastHeight, finalHeight = height;
                boolean finalLastWasUp = lastWasUp;
                if (lastWasUp && lastJump != null) {
                    lastJump.addAcceleration(lastJump.lm.pos.x - x + (lastHeight > height ? 1 - box.getRight() : box.getLeft() + playerWidth), friction, false);
                }
                if (lastHeight > height) {
                    int finalFrom = from;
                    lastJump = gen.addJumpMarker("static_jump", group, new ObjPos(x - 1 + box.getRight(), lastHeight + box.getTop()), data -> {
                        if (finalHeight == -1)
                            data.setApproachDirection(Direction.RIGHT);
                        data.addAcceleration(data.lm.pos.x - finalFrom + (finalLastWasUp ? box.getLeft() : -playerWidth + 1 - box.getRight()), friction, true);
                    });
                    lastWasUp = false;
                } else {
                    lastJump = gen.addJumpMarker("static_jump", group, new ObjPos(x - box.getLeft(), height + box.getTop()), data -> {
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

    public void addBlock(ObjPos pos) {
        blockPositions.add(pos.copy());
    }

    public void addBlock(int x, int y) {
        blockPositions.add(new ObjPos(x, y));
    }

    public BlockCollection generateTopLayers(String blockName, ObjPos origin, ProceduralGenerator gen, int layers, Supplier<Boolean> extraBlockProbability) {
        HashSet<ObjPos> blocks = new HashSet<>(blockPositions);
        for (int i = 0; i < layers + 1; i++) {
            HashMap<Integer, ObjPos> topLayer = new HashMap<>();
            blocks.forEach(pos -> {
                int xPos = (int) pos.x;
                if (!topLayer.containsKey(xPos))
                    topLayer.put(xPos, pos);
                else if (topLayer.get(xPos).y < pos.y)
                    topLayer.put(xPos, pos);
            });
            if (i == layers) {
                HashSet<ObjPos> lastLayer = new HashSet<>();
                for (Map.Entry<Integer, ObjPos> entry : topLayer.entrySet()) {
                    if (extraBlockProbability.get()) {
                        lastLayer.add(entry.getValue());
                    }
                }
                lastLayer.forEach(pos -> gen.addBlock(blockName, pos.copy().add(origin)));
                blocks.removeIf(lastLayer::contains);
                break;
            }
            topLayer.forEach((height, pos) -> gen.addBlock(blockName, pos.copy().add(origin)));
            blocks.removeIf(topLayer::containsValue);
        }
        return new BlockCollection(blocks);
    }

    public BlockCollection generateBlocks(String blockName, ProceduralGenerator gen) {
        blockPositions.forEach(pos -> gen.addBlock(blockName, pos.copy()));
        return null;
    }

    public BlockCollection generateBlocks(String blockName, ObjPos origin, ProceduralGenerator gen) {
        blockPositions.forEach(pos -> gen.addBlock(blockName, pos.copy().add(origin)));
        return null;
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

    public void removeDisconnected(int originX, int originY) {
        HashSet<ObjPos> visited = new HashSet<>();
        dfsConnected(new ObjPos(originX, originY), visited);
        blockPositions = visited;
    }

    private void dfsConnected(ObjPos pos, HashSet<ObjPos> visited) {
        if (blockPositions.contains(pos.toInt()) && !visited.contains(pos.toInt())) {
            visited.add(pos);
            dfsConnected(pos.copy().add(0, 1), visited);
            dfsConnected(pos.copy().add(0, -1), visited);
            dfsConnected(pos.copy().add(1, 0), visited);
            dfsConnected(pos.copy().add(-1, 0), visited);
        }
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

    public BlockCollection joinAndCopy(BlockCollection other, int xOffset, int yOffset) {
        BlockCollection c = new BlockCollection().setBound(bound.copy().expandToFit(other.bound.copy().offset(xOffset, yOffset)));
        forEachBlockPos(c::addBlock);
        other.forEachBlockPos(pos -> c.addBlock(pos.copy().add(xOffset, yOffset)));
        return c;
    }

    public BlockCollection join(BlockCollection other, ObjPos pos) {
        return join(other, pos.xInt(), pos.yInt());
    }

    public BlockCollection join(BlockCollection other, int xOffset, int yOffset) {
        StaticHitBox otherBound = other.bound.copy().offset(xOffset, yOffset);
        if (bound == null)
            bound = otherBound;
        else
            bound.expandToFit(otherBound);
        other.forEachBlockPos(pos -> addBlock(pos.copy().add(xOffset, yOffset)));
        return this;
    }
}