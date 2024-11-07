package level.procedural;

import foundation.Main;
import foundation.math.BezierCurve3;
import foundation.math.MathHelper;
import loader.JsonObject;
import loader.JsonType;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public enum GeneratorType {
    FOREST_BRANCH("forest_branch", () -> new ProceduralGenerator((gen, lm, type) -> {
        gen.addBlock(type.getString(0), lm.pos);
        //If this isn't done, there is a bias for the second offset being 0
        boolean flipOffsets = gen.randomBoolean(0.5f);
        int extension = gen.randomInt(type.getInt(0), type.getInt(1));
        if (lm.pos.x < Main.BLOCKS_X / 2f) {
            float t = gen.randomFloat(0.4f, 0.6f);
            float firstOffset = gen.randomFloat(-lm.pos.x / 3, lm.pos.x / 3);
            float centerPointX = MathHelper.lerp(0, lm.pos.x, t);
            BezierCurve3 curve = new BezierCurve3(
                    0, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-5, 5),
                    lm.pos.x, lm.pos.y,
                    0.5f
            );
            curve.forEachBlockNearCurve(1.3f, (point, dist) -> dist < (1 - point + 0.5f) * 1, (pos, dist) -> {
                gen.addBlock(type.getString(0), pos);
            });
        } else {
            float t = gen.randomFloat(0.4f, 0.6f);
            float firstOffset = gen.randomFloat(-(Main.BLOCKS_X - 1 - lm.pos.x) / 3, (Main.BLOCKS_X - 1 - lm.pos.x) / 3);
            float centerPointX = MathHelper.lerp(Main.BLOCKS_X - 1, lm.pos.x, t);
            BezierCurve3 curve = new BezierCurve3(
                    Main.BLOCKS_X - 1, lm.pos.y + firstOffset,
                    centerPointX, MathHelper.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-5, 5),
                    lm.pos.x, lm.pos.y,
                    0.5f
            );
            curve.forEachBlockNearCurve(0.8f, (pos, dist) -> {
                gen.addBlock(type.getString(0), pos);
            });
        }
    }), storeString("woodBlock")
            .andThen(storeString("leafBlock"))
            .andThen(storeInt("minExtension"))
            .andThen(storeInt("maxExtension")),
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
