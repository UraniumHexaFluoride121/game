package level.procedural.generator;

import foundation.math.ObjPos;
import level.Level;
import level.procedural.collections.BlockCollection;
import level.procedural.collections.BlockCollectionAction;
import level.procedural.collections.IslandCluster;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.types.ForestTypes;
import loader.AssetManager;
import loader.JsonObject;
import loader.JsonType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class GeneratorType {
    public static final HashSet<GeneratorType> values = new HashSet<>();
    public static final GeneratorType[] types = new GeneratorType[]{
            ForestTypes.FOREST_BRANCH,
            ForestTypes.FOREST_ISLAND_CLUSTER,
            ForestTypes.FOREST_ISLAND_CLUSTER_LARGE,
            ForestTypes.FOREST_ISLAND_DEFAULT,
            ForestTypes.FOREST_ISLAND_DEFAULT_SMALL,
            ForestTypes.FOREST_ISLAND_DEFAULT_SMALL_EXTRA,
            ForestTypes.FOREST_ISLAND_VERTICAL,
            ForestTypes.FOREST_ISLAND_VERTICAL_LARGE
    };

    public final String s;
    public final Function<Level, ProceduralGenerator> generator;
    public final BiConsumer<GeneratorType, JsonObject> jsonDataParser;
    public final boolean hasData;

    public final ArrayList<Integer> intValues = new ArrayList<>();
    public final ArrayList<String> stringValues = new ArrayList<>();

    public GeneratorType(String s, Function<Level, ProceduralGenerator> generator, BiConsumer<GeneratorType, JsonObject> jsonDataParser, boolean hasData) {
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
            BlockCollection.generateJumpValidation(blocks.getBlockHeights(lm.pos), gen, lm,
                    AssetManager.blockHitBoxes.get(blockName),
                    AssetManager.blockFriction.get(blockName)
            );
        };
    }

    public static GeneratorLMFunction generateIslandClusterValidation(int blockNameIndex, String islandClusterName) {
        return (gen, lm, type) -> {
            String blockName = type.getString(blockNameIndex);
            IslandCluster blocks = gen.getData(islandClusterName, IslandCluster.class);
            blocks.forEachIsland((island, offset) -> BlockCollection.generateJumpValidation(island.getBlockHeights(lm.pos.copy().add(offset)), gen, lm,
                    AssetManager.blockHitBoxes.get(blockName),
                    AssetManager.blockFriction.get(blockName)
            ));
        };
    }

    public static GeneratorFunction genCollection(String collectionDataName, BlockCollectionAction action) {
        return (gen, lm, type) -> {
            action.generate(gen, lm, type, gen.getData(collectionDataName, BlockCollection.class));
        };
    }

    public static GeneratorFunction genForEachIslandCluster(String collectionDataName, BlockCollectionAction action) {
        return (gen, lm, type) -> {
            IslandCluster cluster = gen.getData(collectionDataName, IslandCluster.class);
            cluster.forEachIsland((island, pos) -> offset(pos).andThen(action).generate(gen, lm, type, island));
        };
    }

    public static BlockCollectionAction topLayers(int blockNameIndex, int layers, float extraLayerProbability) {
        return (gen, lm, type, collection) ->
                collection.generateTopLayers(type.getString(blockNameIndex), lm.pos, gen, layers, gen.probability(extraLayerProbability), false);
    }

    public static BlockCollectionAction topLayersUnchanged(int blockNameIndex, int layers, float extraLayerProbability) {
        return (gen, lm, type, collection) ->
                collection.generateTopLayers(type.getString(blockNameIndex), lm.pos, gen, layers, gen.probability(extraLayerProbability), true);
    }

    public static BlockCollectionAction offset(ObjPos pos) {
        return (gen, lm, type, collection) ->
                collection.offset(pos);
    }

    public static BlockCollectionAction allBlocks(int blockNameIndex) {
        return (gen, lm, type, collection) ->
                collection.generateBlocks(type.getString(blockNameIndex), lm.pos, gen);
    }
}
