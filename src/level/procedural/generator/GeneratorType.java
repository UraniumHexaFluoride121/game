package level.procedural.generator;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.BezierCurve3;
import foundation.math.MathHelper;
import level.procedural.marker.LayoutMarker;
import loader.JsonObject;
import loader.JsonType;
import physics.StaticHitBox;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public enum GeneratorType {
    FOREST_BRANCH("forest_branch", () -> new ProceduralGenerator((gen, lm, type) -> {
        BezierCurve3 curve;
        float length;
        //Describes the distance at which blocks generate from the curve for a given point T, if the curve is of length L
        BinaryOperator<Float> curveSize = (l, t) -> (1 - t) * l / 20 + 0.7f + (1 - l / 20); //(length, point) -> distance
        float t = gen.randomFloat(0.4f, 0.6f);
        boolean isLeftSide = lm.pos.x < Main.BLOCKS_X / 2f;
        if (isLeftSide) {
            length = lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathHelper.lerp(0, lm.pos.x, t);
            curve = new BezierCurve3(
                    0, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x, lm.pos.y,
                    0.5f
            );
            curve.forEachBlockNearCurve(2f,
                    (point, dist) -> dist < curveSize.apply(length, point),
                    (pos, dist) -> {
                        gen.addBlock(type.getString(0), pos);
                    });
        } else {
            length = Main.BLOCKS_X - 1 - lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathHelper.lerp(Main.BLOCKS_X - 1, lm.pos.x, t);
            curve = new BezierCurve3(
                    Main.BLOCKS_X - 1, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x, lm.pos.y,
                    0.5f
            );
            curve.forEachBlockNearCurve(2,
                    (point, dist) -> dist < curveSize.apply(length, point),
                    (pos, dist) -> {
                        gen.addBlock(type.getString(0), pos);
                    });
        }
        gen.addData("curve", curve);
        StaticHitBox mainBound = curve.getBox().copy().expand(curveSize.apply(length, 0f));
        lm.addBound(mainBound, BoundType.COLLISION);
        lm.addBound(mainBound.copy().expand(3, 4, isLeftSide ? 0 : 5, isLeftSide ? -5 : 0), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(0, 1), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(2, 0), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(3, 5), BoundType.OVERCROWDING);
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            gen.addMarker("platform", gen.randomPosAbove(lm, 0.5f, 1.2f, 5, 9, 2.5f, 20));
        }
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING))),
            storeString("woodBlock")
                    .andThen(storeString("leafBlock")),
            true
    ),

    ISLAND_TEST("island_test", () -> new ProceduralGenerator((gen, lm, type) -> {
        int sizeLeft = gen.randomInt(3, 6);
        int sizeRight = gen.randomInt(3, 6);
        lm.addBound(new StaticHitBox(1f, 1f, sizeLeft, sizeRight + 1, lm.pos), BoundType.COLLISION);
        lm.addBound(new StaticHitBox(3f, 3f, sizeLeft - 2, sizeRight - 1, lm.pos), BoundType.OBSTRUCTION);
        lm.addBound(new StaticHitBox(1f, 1f, sizeLeft + 2, sizeRight + 3, lm.pos), BoundType.OBSTRUCTION);
        lm.addBound(new StaticHitBox(6f, 6f, sizeLeft + 4, sizeRight + 5, lm.pos), BoundType.OVERCROWDING);
        String blockName = type.getString(0);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y, pos -> blockName);
        sizeLeft -= gen.randomInt(1, 2);
        sizeRight -= gen.randomInt(1, 2);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y - 1, pos -> blockName);
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            int borderProximityLimit = type.getInt(0);
            gen.addMarker("platform", gen.randomPosAbove(lm, 0.2f, 1.2f, 5, 9, 2.5f, borderProximityLimit));
        }
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING))),
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("block")),
            true
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
