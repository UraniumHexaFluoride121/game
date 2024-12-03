package level.procedural.generator;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.BezierCurve3;
import foundation.math.FunctionalWeightedRandom;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.procedural.Layout;
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
        GenUtil.generateJumpValidation(blockHeights, gen, lm, AssetManager.blockHitBoxes.get(blockName), AssetManager.blockFriction.get(blockName));
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            BezierCurve3 curve = gen.getData("curve", BezierCurve3.class);
            AtomicReference<ObjPos> pos = new AtomicReference<>();
            gen.addMarker("platform", gen.randomPosAbove(() -> {
                pos.set(curve.sampleCurve(gen.randomFloat(0, 1)));
                return pos.get();
            }, 0.3f, 1.3f, 6, 12, 2.5f, 20));
            if (Layout.DEBUG_RENDER && Layout.DEBUG_RENDER_BEZIER_CURVES)
                gen.addMarker("debug", pos.get());
        }
    }),
            storeString("woodBlock")
                    .andThen(storeString("leafBlock")), true
    ),

    ISLAND("island", () -> new ProceduralGenerator((gen, lm, type) -> {
        int sizeLeft = gen.randomInt(2, 6);
        int sizeRight = gen.randomInt(2, 6);

        GenUtil.StackData stack = GenUtil.blockStackInward(sizeLeft, sizeRight, gen.random(), new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                .add(0, s -> s.lastValue() == 0 ? 2f : 12)
                .add(1, s -> 15f)
                .add(2, s -> 15f)
                .add(3, s -> s.lastWidth() > 8 ? 2f : 0)
        );
        int stackDown = stack.height() - 1;

        int offset = ((int) ((sizeLeft + sizeRight) * gen.randomFloat(-0.4f, 0.4f)));
        GenUtil.StackData topStack = GenUtil.blockStackInward(Math.min(sizeLeft, (int) (sizeLeft * gen.randomFloat(0.2f, 0.8f)) + offset), Math.min(sizeRight, (int) (sizeRight * gen.randomFloat(0.2f, 0.8f)) - offset), gen.random(), new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                .add(0, s -> s.lastValue() == 0 ? 1f : 4)
                .add(1, s -> 6f)
                .add(2, s -> 10f)
                .add(3, s -> 15f)
                .add(4, s -> s.lastWidth() > 8 ? 5f : 2)
        );
        int stackUp = topStack.height();
        lm.addBound(new StaticHitBox(stackUp + 1, stackDown, sizeLeft, sizeRight + 1, lm.pos), BoundType.BLOCKS);
        lm.addBound(new StaticHitBox(stackUp + 1, stackDown, sizeLeft, sizeRight + 1, lm.pos), BoundType.COLLISION);
        lm.addBound(new StaticHitBox(stackUp + 3, stackDown + 2, stack.getLeftPercent(.5f), stack.getRightPercent(0.5f) + 1, lm.pos), BoundType.OBSTRUCTION);
        lm.addBound(new StaticHitBox(1, stack.getHeightPercent(.5f) - 1, sizeLeft + 1, sizeRight + 2, lm.pos), BoundType.OBSTRUCTION);
        lm.addBound(new StaticHitBox(stackUp + 7, stackDown + 7, sizeLeft + 3, sizeRight + 4, lm.pos), BoundType.OVERCROWDING);
        gen.addData("sizeLeft", sizeLeft);
        gen.addData("sizeRight", sizeRight);
        gen.addData("stack", stack);
        gen.addData("topStack", topStack);
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)), (gen, lm, type) -> {
        GenUtil.StackData stack = gen.getData("stack", GenUtil.StackData.class);
        GenUtil.StackData topStack = gen.getData("topStack", GenUtil.StackData.class);
        String blockName = type.getString(0);
        stack.forEach((l, r, i) -> gen.lineOfBlocks(lm.pos.x - l, lm.pos.x + r, lm.pos.y - i, pos -> blockName));
        topStack.forEach((l, r, i) -> gen.lineOfBlocks(lm.pos.x - l, lm.pos.x + r, lm.pos.y + i + 1, pos -> blockName));
    }, (gen, lm, type) -> {
        String blockName = type.getString(0);
        GenUtil.StackData stack = gen.getData("stack", GenUtil.StackData.class);
        GenUtil.StackData topStack = gen.getData("topStack", GenUtil.StackData.class);
        GenUtil.generateJumpValidation(
                GenUtil.maxBlockHeights(
                        topStack.getBlockHeights(lm.pos.x, lm.pos.y + 1, false),
                        stack.getBlockHeights(lm.pos, true)), gen, lm,
                AssetManager.blockHitBoxes.get(blockName),
                AssetManager.blockFriction.get(blockName)
        );
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            int borderProximityLimit = type.getInt(0);
            gen.addMarker("platform", gen.randomPosAbove(lm, 0.2f, 1.6f, 6, 15, 2.5f, borderProximityLimit));
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
