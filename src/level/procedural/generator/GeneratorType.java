package level.procedural.generator;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.BezierCurve3;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.procedural.marker.LayoutMarker;
import loader.JsonObject;
import loader.JsonType;
import physics.StaticHitBox;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public enum GeneratorType {
    FOREST_BRANCH("forest_branch", () -> new ProceduralGenerator((gen, lm, type) -> {
        if (lm.pos.x < Main.BLOCKS_X / 2f) {
            float t = gen.randomFloat(0.4f, 0.6f);
            float length = lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathHelper.lerp(0, lm.pos.x, t);
            BezierCurve3 curve = new BezierCurve3(
                    0, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x, lm.pos.y,
                    0.5f
            );
            gen.addData("curve", curve);
            curve.forEachBlockNearCurve(2f,
                    (point, dist) -> dist < (1 - point) * length / 20 + 0.7f + (1 - length / 20),
                    (pos, dist) -> {
                        gen.addBlock(type.getString(0), pos);
                    });
        } else {
            float t = gen.randomFloat(0.4f, 0.6f);
            float length = Main.BLOCKS_X - 1 - lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathHelper.lerp(Main.BLOCKS_X - 1, lm.pos.x, t);
            BezierCurve3 curve = new BezierCurve3(
                    Main.BLOCKS_X - 1, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x, lm.pos.y,
                    0.5f
            );
            gen.addData("curve", curve);
            curve.forEachBlockNearCurve(2,
                    (point, dist) -> dist < (1 - point) * length / 20 + 0.7f + (1 - length / 20),
                    (pos, dist) -> {
                        gen.addBlock(type.getString(0), pos);
                    });
        }
    }, (gen, lm, type) -> {

    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)),
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
        lm.addBound(new StaticHitBox(6f, 6f, sizeLeft + 4, sizeRight + 5, lm.pos), BoundType.OVER_CROWDING);
        String blockName = type.getString(0);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y, pos -> blockName);
        sizeLeft -= gen.randomInt(1, 2);
        sizeRight -= gen.randomInt(1, 2);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y - 1, pos -> blockName);
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            ObjPos pos;
            int borderProximityLimit = type.getInt(0);
            do {
                float angle = gen.randomFloat(0.2f, 1.35f);
                float length = gen.randomFloat(5, 9);
                boolean isRight = gen.randomBoolean(0.5f);
                if (lm.pos.x > Main.BLOCKS_X - 1 - borderProximityLimit)
                    isRight = false;
                else if (lm.pos.x < borderProximityLimit)
                    isRight = true;
                pos = new ObjPos(lm.pos.x + Math.cos(angle) * length * 2.7f * (isRight ? 1 : -1), lm.pos.y + Math.sin(angle) * length).toInt();
            } while (MainPanel.level.outOfBounds(pos));
            gen.addMarker("platform", pos);
        }
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVER_CROWDING))),
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
