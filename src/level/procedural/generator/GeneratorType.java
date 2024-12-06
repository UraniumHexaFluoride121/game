package level.procedural.generator;

import foundation.MainPanel;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.types.ForestTypes;
import loader.AssetManager;
import loader.JsonObject;
import loader.JsonType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class GeneratorType {
    public static final HashSet<GeneratorType> values = new HashSet<>();
    public static final GeneratorType[] types = new GeneratorType[]{
            ForestTypes.FOREST_BRANCH,
            ForestTypes.FOREST_ISLAND_DEFAULT,
            ForestTypes.FOREST_ISLAND_DEFAULT_SMALL,
            ForestTypes.FOREST_ISLAND_DEFAULT_SMALL_EXTRA,
            ForestTypes.FOREST_ISLAND_VERTICAL,
            ForestTypes.FOREST_ISLAND_VERTICAL_LARGE
    };

    public final String s;
    public final Supplier<ProceduralGenerator> generator;
    public final BiConsumer<GeneratorType, JsonObject> jsonDataParser;
    public final boolean hasData;

    public final ArrayList<Integer> intValues = new ArrayList<>();
    public final ArrayList<String> stringValues = new ArrayList<>();

    public GeneratorType(String s, Supplier<ProceduralGenerator> generator, BiConsumer<GeneratorType, JsonObject> jsonDataParser, boolean hasData) {
        values.add(this);
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

    public int getInt(int index) {
        return intValues.get(index);
    }

    public String getString(int index) {
        return stringValues.get(index);
    }

    public static GeneratorType getGeneratorType(String s) {
        for (GeneratorType type : GeneratorType.values) {
            if (type.s.equals(s))
                return type;
        }
        throw new IllegalArgumentException("Unknown generator type: " + s);
    }

    public static BiConsumer<GeneratorType, JsonObject> storeNothing() {
        return (type, data) -> {
        };
    }

    public static BiConsumer<GeneratorType, JsonObject> storeInt(String name) {
        return (type, data) -> type.intValues.add(data.get(name, JsonType.INTEGER_JSON_TYPE));
    }

    public static BiConsumer<GeneratorType, JsonObject> storeString(String name) {
        return (type, data) -> type.stringValues.add(data.get(name, JsonType.STRING_JSON_TYPE));
    }

    public static BiConsumer<GeneratorType, JsonObject> storeIntOrDefault(String name, int defaultValue) {
        return (type, data) -> type.intValues.add(data.getOrDefault(name, defaultValue, JsonType.INTEGER_JSON_TYPE));
    }

    public static BiConsumer<GeneratorType, JsonObject> storeStringOrDefault(String name, String defaultValue) {
        return (type, data) -> type.stringValues.add(data.getOrDefault(name, defaultValue, JsonType.STRING_JSON_TYPE));
    }

    public static GeneratorLMFunction generateBlockCollectionValidation(int blockNameIndex, String collectionDataName) {
        return (gen, lm, type) -> {
            String blockName = type.getString(blockNameIndex);
            BlockCollection blocks = gen.getData(collectionDataName, BlockCollection.class);
            GenUtil.generateJumpValidation(blocks.getBlockHeights(lm.pos), gen, lm,
                    AssetManager.blockHitBoxes.get(blockName),
                    AssetManager.blockFriction.get(blockName)
            );
        };
    }

    public static GeneratorFunction generateBlockCollection(int blockNameIndex, String collectionDataName) {
        return (gen, lm, type) -> {
            gen.getData(collectionDataName, BlockCollection.class).generateBlocks(type.getString(blockNameIndex), lm.pos, gen);
        };
    }

    public static GeneratorLMFunction generateDefault(int borderProximityIndex, int platformNameIndex) {
        return generateAbove(borderProximityIndex, platformNameIndex, 0.2f, 1.6f, 6, 15, 2.5f);
    }

    public static GeneratorLMFunction generateAround(int borderProximityIndex, int platformNameIndex, int maxLength, float probability) {
        return (gen, lm, type) -> {
            if (gen.randomBoolean(probability) && lm.pos.y > 30)
                generateAbove(borderProximityIndex, platformNameIndex, -1f, 1.6f, 7, maxLength, 1f).generateMarkers(gen, lm, type);
        };
    }

    public static GeneratorLMFunction generateAbove(int borderProximityIndex, int platformNameIndex, float minAngle, float maxAngle, float minLength, float maxLength, float xLengthMultiplier) {
        return (gen, lm, type) -> {
            if (lm.pos.y < MainPanel.level.getRegionTop()) {
                int borderProximityLimit = type.getInt(borderProximityIndex);
                gen.addMarker(type.getString(platformNameIndex), gen.randomPosAbove(lm, minAngle, maxAngle, minLength, maxLength, xLengthMultiplier, borderProximityLimit));
            }
        };
    }

    public static GeneratorLMFunction generateNothing() {
        return (gen, lm, type) -> {

        };
    }
}
