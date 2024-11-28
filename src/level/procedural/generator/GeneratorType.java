package level.procedural.generator;

import foundation.Direction;
import foundation.Main;
import foundation.MainPanel;
import foundation.math.BezierCurve3;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;
import loader.JsonObject;
import loader.JsonType;
import physics.StaticHitBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public enum GeneratorType {
    FOREST_BRANCH("forest_branch", () -> new ProceduralGenerator((gen, lm, type) -> {
        BezierCurve3 curve;
        float length;
        float t = gen.randomFloat(0.4f, 0.6f);
        boolean isLeftSide = lm.pos.x < Main.BLOCKS_X / 2f;
        if (isLeftSide) {
            length = lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathHelper.lerp(0, lm.pos.x, t);
            curve = new BezierCurve3(
                    0, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x + 3, lm.pos.y,
                    0.5f
            );
        } else {
            length = Main.BLOCKS_X - 1 - lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathHelper.lerp(Main.BLOCKS_X - 1, lm.pos.x, t);
            curve = new BezierCurve3(
                    Main.BLOCKS_X - 1, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x - 3, lm.pos.y,
                    0.5f
            );
        }
        gen.addData("curve", curve);
        StaticHitBox mainBound = curve.getBox().copy().expand(GeneratorTypeFunctions.FOREST_BRANCH_CURVE_SIZE.apply(length, 0f));
        lm.addBound(mainBound, BoundType.COLLISION);
        lm.addBound(mainBound, BoundType.BLOCKS);
        lm.addBound(mainBound.copy().expand(0, 6, isLeftSide ? 0 : 7, isLeftSide ? -7 : 0), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(0, 1), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(2, 0), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(3, 5), BoundType.OVERCROWDING);
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)), (gen, lm, type) -> {
        BezierCurve3 curve = gen.getData("curve", BezierCurve3.class);
        float length;
        boolean isLeftSide = lm.pos.x < Main.BLOCKS_X / 2f;
        HashMap<Integer, Integer> blockHeights = new HashMap<>();
        if (isLeftSide) {
            length = lm.pos.x;
            curve.forEachBlockNearCurve(2f,
                    (point, dist) -> dist < GeneratorTypeFunctions.FOREST_BRANCH_CURVE_SIZE.apply(length, point),
                    (pos, dist) -> {
                        int x = ((int) pos.x);
                        int y = ((int) pos.y);
                        if (!blockHeights.containsKey(x))
                            blockHeights.put(x, y);
                        else
                            blockHeights.put(x, Math.max(blockHeights.get(x), y));
                        gen.addBlock(type.getString(0), pos);
                    });
        } else {
            length = Main.BLOCKS_X - 1 - lm.pos.x;
            curve.forEachBlockNearCurve(2,
                    (point, dist) -> dist < GeneratorTypeFunctions.FOREST_BRANCH_CURVE_SIZE.apply(length, point),
                    (pos, dist) -> {
                        int x = ((int) pos.x);
                        int y = ((int) pos.y);
                        if (!blockHeights.containsKey(x))
                            blockHeights.put(x, y);
                        else
                            blockHeights.put(x, Math.max(blockHeights.get(x), y));
                        gen.addBlock(type.getString(0), pos);
                    });
        }
        gen.addData("blockHeights", blockHeights);
    }, (gen, lm, type) -> {
        HashMap<Integer, Integer> blockHeights = gen.getData("blockHeights", HashMap.class);
        String blockName = type.getString(0);
        StaticHitBox box = AssetManager.blockHitBoxes.get(blockName);
        int lastHeight = -1;
        int from = 0;
        for (int x = 0; x <= Main.BLOCKS_X; x++) {
            int height = blockHeights.getOrDefault(x, -1);
            if (lastHeight == -1 || lastHeight != height) {
                if (lastHeight != -1) {
                    if (lastHeight > height) {
                        gen.addJumpMarker("static_jump", new ObjPos(x - 1 + box.getRight(), lastHeight + box.getTop()));
                    } else {
                        gen.addJumpMarker("static_jump", new ObjPos(x + box.getLeft(), height + box.getTop()));
                    }
                    lm.addBound(new StaticHitBox(lastHeight + 1 + box.getTop(), lastHeight + box.getTop(), from + 0.3f, x - 0.3f), BoundType.JUMP_VALIDATION);
                }
                from = x;
                lastHeight = height;
            }
        }
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            BezierCurve3 curve = gen.getData("curve", BezierCurve3.class);
            AtomicReference<ObjPos> pos = new AtomicReference<>();
            gen.addMarker("platform", gen.randomPosAbove(() -> {
                pos.set(curve.sampleCurve(gen.randomFloat(0, 1)));
                return pos.get();
            }, 0.5f, 1.3f, 5, 10, 2.5f, 20));
            gen.addMarker("debug", pos.get());
        }
    }),
            storeString("woodBlock")
                    .andThen(storeString("leafBlock")), true
    ),

    ISLAND_TEST("island_test", () -> new ProceduralGenerator((gen, lm, type) -> {
        int sizeLeft = gen.randomInt(3, 6);
        int sizeRight = gen.randomInt(3, 6);
        lm.addBound(new StaticHitBox(2, -1, sizeLeft - 0.5f, sizeRight + 0.5f, lm.pos), BoundType.JUMP_VALIDATION);
        lm.addBound(new StaticHitBox(1f, 1f, sizeLeft, sizeRight + 1, lm.pos), BoundType.BLOCKS);
        lm.addBound(new StaticHitBox(1f, 1f, sizeLeft, sizeRight + 1, lm.pos), BoundType.COLLISION);
        lm.addBound(new StaticHitBox(3f, 3f, sizeLeft - 2, sizeRight - 1, lm.pos), BoundType.OBSTRUCTION);
        lm.addBound(new StaticHitBox(1f, 1f, sizeLeft + 2, sizeRight + 3, lm.pos), BoundType.OBSTRUCTION);
        lm.addBound(new StaticHitBox(6f, 6f, sizeLeft + 4, sizeRight + 5, lm.pos), BoundType.OVERCROWDING);
        gen.addData("sizeLeft", sizeLeft);
        gen.addData("sizeRight", sizeRight);
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)), (gen, lm, type) -> {
        int sizeLeft = gen.getData("sizeLeft", Integer.class);
        int sizeRight = gen.getData("sizeRight", Integer.class);
        String blockName = type.getString(0);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y, pos -> blockName);
        sizeLeft -= gen.randomInt(1, 2);
        sizeRight -= gen.randomInt(1, 2);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y - 1, pos -> blockName);
    }, (gen, lm, type) -> {
        int sizeLeft = MathHelper.clampInt(0, ((int) lm.pos.x), gen.getData("sizeLeft", Integer.class));
        int sizeRight = MathHelper.clampInt(0, Main.BLOCKS_X - ((int) lm.pos.x) - 1, gen.getData("sizeRight", Integer.class));
        String blockName = type.getString(0);
        StaticHitBox box = AssetManager.blockHitBoxes.get(blockName);
        float friction = AssetManager.blockFriction.get(blockName);
        float platformSize = sizeLeft + sizeRight + box.right + box.left;
        gen.addJumpMarker("static_jump", lm.pos.copy().add(-sizeLeft - box.left, box.up)).addAcceleration(-platformSize, friction).setApproachDirection(Direction.LEFT);
        gen.addJumpMarker("static_jump", lm.pos.copy().add(sizeRight + box.right, box.up)).addAcceleration(platformSize, friction).setApproachDirection(Direction.RIGHT);
        gen.addJumpMarker("static_jump", lm.pos.copy().add(box.middleX(), box.up));
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            int borderProximityLimit = type.getInt(0);
            gen.addMarker("platform", gen.randomPosAbove(lm, 0.2f, 1.6f, 6, 12, 2.5f, borderProximityLimit));
        }
    }),
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("block")), true
    );

    public final String s;
    public final Supplier<ProceduralGenerator> generator;
    public final BiConsumer<GeneratorType, JsonObject> jsonDataParser;
    public final boolean hasData;

    public final ArrayList<Integer> intValues = new ArrayList<>();
    public final ArrayList<String> stringValues = new ArrayList<>();

    GeneratorType(String s, Supplier<ProceduralGenerator> generator, BiConsumer<GeneratorType, JsonObject> jsonDataParser, boolean hasData) {
        this.s = s;
        this.generator = generator;
        this.jsonDataParser = jsonDataParser;
        this.hasData = hasData;
    }

    public boolean hasData() {
        return hasData;
    }

    public void parseDataFromJson(JsonObject data) {
        jsonDataParser.accept(this, data);
    }

    private int getInt(int index) {
        return intValues.get(index);
    }

    private String getString(int index) {
        return stringValues.get(index);
    }

    public static GeneratorType getGeneratorType(String s) {
        for (GeneratorType type : GeneratorType.values()) {
            if (type.s.equals(s))
                return type;
        }
        throw new IllegalArgumentException("Unknown generator type: " + s);
    }

    private static BiConsumer<GeneratorType, JsonObject> storeNothing() {
        return (type, data) -> {
        };
    }

    private static BiConsumer<GeneratorType, JsonObject> storeInt(String name) {
        return (type, data) -> type.intValues.add(data.get(name, JsonType.INTEGER_JSON_TYPE));
    }

    private static BiConsumer<GeneratorType, JsonObject> storeString(String name) {
        return (type, data) -> type.stringValues.add(data.get(name, JsonType.STRING_JSON_TYPE));
    }

    private static BiConsumer<GeneratorType, JsonObject> storeIntOrDefault(String name, int defaultValue) {
        return (type, data) -> type.intValues.add(data.getOrDefault(name, defaultValue, JsonType.INTEGER_JSON_TYPE));
    }

    private static BiConsumer<GeneratorType, JsonObject> storeStringOrDefault(String name, String defaultValue) {
        return (type, data) -> type.stringValues.add(data.getOrDefault(name, defaultValue, JsonType.STRING_JSON_TYPE));
    }
}
