package level.procedural;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.BezierCurve3;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import loader.JsonObject;
import loader.JsonType;

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
    }), storeString("woodBlock")
            .andThen(storeString("leafBlock")),
            true
    ),

    ISLAND_TEST("island_test", () -> new ProceduralGenerator((gen, lm, type) -> {
        int sizeLeft = gen.randomInt(3, 6);
        int sizeRight = gen.randomInt(3, 6);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y, pos -> "stone_grey");
        sizeLeft -= gen.randomInt(1, 2);
        sizeRight -= gen.randomInt(1, 2);
        gen.lineOfBlocks(lm.pos.x - sizeLeft, lm.pos.x + sizeRight, lm.pos.y - 1, pos -> "stone_grey");
        if (lm.pos.y < MainPanel.level.getRegionTop())
            gen.addMarker("platform", new ObjPos(lm.pos.x + gen.randomInt(5, 7) * (gen.randomBoolean(0.5f) ? 1 : -1), lm.pos.y + gen.randomInt(6, 9)));
    }), storeNothing(), false);

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
