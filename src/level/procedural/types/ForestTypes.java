package level.procedural.types;

import foundation.Main;
import foundation.math.*;
import level.procedural.Layout;
import level.procedural.collections.BlockCollection;
import level.procedural.collections.StackRandomData;
import level.procedural.generator.BoundType;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;
import physics.StaticHitBox;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

import static level.procedural.generator.GeneratorType.*;

public abstract class ForestTypes {
    //Describes the distance at which blocks generate from the curve for a given point T, if the curve is of length L
    public final static BinaryOperator<Float> FOREST_BRANCH_CURVE_SIZE = (l, t) -> (1 - t) * l / 20 + 0.7f + (1 - l / 20); //(length, point) -> distance

    public static final GeneratorType FOREST_BRANCH = new GeneratorType("forest_branch", level -> new ProceduralGenerator(level, (gen, lm, type) -> {
        BezierCurve3 curve;
        float length;
        float t = gen.randomFloat(0.4f, 0.6f);
        boolean isLeftSide = lm.pos.x < Main.BLOCKS_X / 2f;
        if (isLeftSide) {
            length = lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathUtil.lerp(0, lm.pos.x, t);
            curve = new BezierCurve3(
                    0, lm.pos.y + firstOffset,
                    centerPointX, MathUtil.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x + 3, lm.pos.y,
                    0.5f
            );
        } else {
            length = Main.BLOCKS_X - 1 - lm.pos.x;
            float firstOffset = gen.randomFloat(-length / 5, length / 5);
            float centerPointX = MathUtil.lerp(Main.BLOCKS_X - 1, lm.pos.x, t);
            curve = new BezierCurve3(
                    Main.BLOCKS_X - 1, lm.pos.y + firstOffset,
                    centerPointX, MathUtil.lerp(lm.pos.y + firstOffset, lm.pos.y, t) + gen.randomFloat(-length / 4, length / 4),
                    lm.pos.x - 3, lm.pos.y,
                    0.5f
            );
        }
        gen.addData("curve", curve);
        StaticHitBox mainBound = curve.getBox().copy().expand(FOREST_BRANCH_CURVE_SIZE.apply(length, 0f));
        lm.addBound(mainBound, BoundType.COLLISION);
        lm.addBound(mainBound, BoundType.BLOCKS);
        lm.addBound(mainBound.copy().expand(1, 3, 0, 0), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(2, 0), BoundType.OBSTRUCTION);
        lm.addBound(mainBound.copy().expand(3, 5), BoundType.OVERCROWDING);
    }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
            .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)), (gen, lm, type) -> {
        BezierCurve3 curve = gen.getData("curve", BezierCurve3.class);
        float length;
        boolean isLeftSide = lm.pos.x < Main.BLOCKS_X / 2f;
        BlockCollection blocks;
        if (isLeftSide) {
            length = lm.pos.x;
            blocks = curve.forEachBlockNearCurve(2, (point, dist) -> dist < FOREST_BRANCH_CURVE_SIZE.apply(length, point));
        } else {
            length = Main.BLOCKS_X - 1 - lm.pos.x;
            blocks = curve.forEachBlockNearCurve(2, (point, dist) -> dist < FOREST_BRANCH_CURVE_SIZE.apply(length, point));
        }
        blocks.generateBlocks(type.getString(0), gen);
        if (lm.pos.x < Main.BLOCKS_X / 2f) {
            BlockCollection branchesTop = blocks.getTopLayer().offset(new ObjPos(0, 1)).spaceLine(6, 12, gen);
            branchesTop.generateBlocks(type.getString(2), gen);
        }
        gen.addData("blocks", blocks);
    }, (gen, lm, type) -> {
        BlockCollection blocks = gen.getData("blocks", BlockCollection.class);
        String blockName = type.getString(0);
        BlockCollection.generateJumpValidation(blocks.getBlockHeights(), gen, lm, AssetManager.blockHitBoxes.get(blockName), AssetManager.blockFriction.get(blockName));
    }, (gen, lm, type) -> {
        if (lm.pos.y < lm.level.getRegionTop()) {
            BezierCurve3 curve = gen.getData("curve", BezierCurve3.class).setLevel(lm.level);
            AtomicReference<ObjPos> pos = new AtomicReference<>();
            gen.addMarker(type.getString(1), gen.randomPosAbove(() -> {
                pos.set(curve.sampleCurve(gen.randomFloat(0, 1)));
                return pos.get();
            }, 0.3f, 1.3f, 6, 12, 2.5f, 20));
            if (Layout.DEBUG_RENDER && Layout.DEBUG_RENDER_BEZIER_CURVES)
                gen.addMarker("debug", pos.get());
        }
    }),
            storeString("woodBlock")
                    .andThen(storeString("generateNextPlatformAs"))
                    .andThen(storeString("branchTop")), true
    );
    public static final GeneratorType FOREST_ISLAND_CLUSTER = PresetTypes.islandCluster("forest_island_cluster", 9, 4, 10,
            new WeightedRandom<Integer>()
                    .add(1, 1)
                    .add(2, 3)
                    .add(3, 7)
                    .add(4, 10)
                    .add(5, 3),
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("block"))
                    .andThen(storeString("generateNextPlatformAs")),
            genCollection("blocks", allBlocks(0)),
            GeneratorLMFunction.generateAbove(0, 1)
                    .setTopOffsetFromCollection("blocks", 0.5f)
                    .setMinLength(8)
                    .setMaxLength(23).finalise(),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(0, s -> s.lastSize() <= 1 ? 0 : s.lastValue() == 0 ? 2f : 8f)
                    .add(1, s -> 8f)
                    .add(2, s -> s.lastValue() > 1 ? 5f : 1)
    );
    public static final GeneratorType FOREST_ISLAND_CLUSTER_LARGE = PresetTypes.islandCluster("forest_island_cluster_large", 12, 5, 10,
            new WeightedRandom<Integer>()
                    .add(4, 3)
                    .add(5, 7)
                    .add(6, 6)
                    .add(7, 4),
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("topLayerBlock"))
                    .andThen(storeString("mainBlock"))
                    .andThen(storeString("generateNextPlatformAs"))
                    .andThen(storeString("grassBlock")),
            genForEachIslandCluster("blocks", topLayersUnchanged(3, 1, 0)
                    .andThen(topLayers(0, 1, 0.5f))
                    .andThen(allBlocks(1))),
            GeneratorLMFunction.generateAbove(0, 2)
                    .setTopOffsetFromCollection("blocks", 0.5f)
                    .setMinLength(10)
                    .setMaxLength(25).finalise(),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(0, s -> s.lastSize() <= 1 ? 0 : s.lastValue() == 0 ? 2f : 8f)
                    .add(1, s -> 7f)
                    .add(2, s -> 2f)
    );
    public static final GeneratorType FOREST_ISLAND_DEFAULT = PresetTypes.defaultIsland("forest_island_default", 3, 6,
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("topLayerBlock"))
                    .andThen(storeString("mainBlock"))
                    .andThen(storeString("generateNextPlatformAs"))
                    .andThen(storeString("generateExtraPlatformAs"))
                    .andThen(storeString("grassBlock")),
            genCollection("blocks", topLayersUnchanged(4, 1, 0)
                    .andThen(topLayers(0, 2, 0.3f))
                    .andThen(allBlocks(1))),
            GeneratorLMFunction.generateAbove(0, 2).finalise()
                    .andThen(GeneratorLMFunction.generateAround(0, 3, 15, 0.3f).finalise()),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(0, s -> s.lastSize() <= 1 ? 0 : s.lastValue() == 0 ? 2f : 12)
                    .add(1, s -> 15f)
                    .add(2, s -> 15f)
                    .add(3, s -> s.lastSize() > 8 ? 2f : 0),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(0, s -> s.lastSize() <= 1 ? 0 : s.lastValue() == 0 ? 1f : 4)
                    .add(1, s -> 6f)
                    .add(2, s -> 10f)
                    .add(3, s -> 15f)
                    .add(4, s -> s.lastSize() > 8 ? 5f : 2)
    );
    public static final GeneratorType FOREST_ISLAND_DEFAULT_SMALL = PresetTypes.defaultIsland("forest_island_default_small", 1, 2,
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("topLayerBlock"))
                    .andThen(storeString("mainBlock"))
                    .andThen(storeString("generateNextPlatformAs"))
                    .andThen(storeString("grassBlock")),
            genCollection("blocks", topLayersUnchanged(3, 1, 0)
                    .andThen(topLayers(0, 1, 0.5f))
                    .andThen(allBlocks(1))),
            GeneratorLMFunction.generateAbove(0, 2).finalise(),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(0, s -> s.lastSize() <= 1 ? 1f : s.lastValue() == 0 ? 1f : 8f)
                    .add(1, s -> 8f), null
    );
    public static final GeneratorType FOREST_ISLAND_DEFAULT_SMALL_EXTRA = PresetTypes.defaultIsland("forest_island_default_small_extra", 1, 2,
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("topLayerBlock"))
                    .andThen(storeString("mainBlock"))
                    .andThen(storeString("grassBlock")),
            genCollection("blocks", topLayersUnchanged(2, 1, 0)
                    .andThen(topLayers(0, 1, 0.5f))
                    .andThen(allBlocks(1))),
            GeneratorLMFunction.generateNothing(), new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(0, s -> s.lastSize() <= 1 ? 1f : s.lastValue() == 0 ? 2f : 8f)
                    .add(1, s -> 10f), null
    );
    public static final GeneratorType FOREST_ISLAND_VERTICAL = PresetTypes.verticalIsland("forest_island_vertical", 2, 4, 6,
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("block"))
                    .andThen(storeString("generateNextPlatformAs"))
                    .andThen(storeString("generateExtraPlatformAs")),
            genCollection("blocks", topLayers(0, 2, 0.3f)
                    .andThen(allBlocks(0))),
            GeneratorLMFunction.generateAbove(0, 1).finalise()
                    .andThen(GeneratorLMFunction.generateAround(0, 2, 15, 0.3f).finalise()),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(1, s -> 10f)
                    .add(2, s -> s.layer() == 0 ? 7f : 12f)
                    .add(3, s -> s.layer() == 0 ? 5f : 8f)
                    .add(4, s -> s.lastSize() > 8 && s.layer() != 1 ? 2f : 0),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(-3, s -> 1f)
                    .add(-2, s -> 4f)
                    .add(-1, s -> 4f)
                    .add(0, s -> 8f)
                    .add(1, s -> 4f)
                    .add(2, s -> 4f)
                    .add(3, s -> 1f)
    );
    public static final GeneratorType FOREST_ISLAND_VERTICAL_LARGE = PresetTypes.verticalIsland("forest_island_vertical_large", 1, 2, 8, 15,
            storeInt("forceAwayFromBorderProximity")
                    .andThen(storeString("topLayerBlock"))
                    .andThen(storeString("mainBlock"))
                    .andThen(storeString("generateNextPlatformAs")),
            genCollection("blocks", topLayers(0, 1, 0.3f)
                    .andThen(allBlocks(1))),
            GeneratorLMFunction.generateAbove(0, 2)
                    .setTopOffsetFromCollection("blocks", 0.5f).finalise(),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(2, s -> 5f)
                    .add(3, s -> s.layer() == 0 ? 5f : 10f)
                    .add(4, s -> s.layer() == 0 ? 1f : 15f)
                    .add(5, s -> s.layer() == 0 ? 0 : 12f)
                    .add(6, s -> s.lastSize() > 8 && s.layer() != 1 ? 2f : 0),
            new FunctionalWeightedRandom<Integer, StackRandomData>()
                    .add(-3, s -> 4f)
                    .add(-2, s -> 4f)
                    .add(-1, s -> 4f)
                    .add(1, s -> 4f)
                    .add(2, s -> 4f)
                    .add(3, s -> 4f)
    );
}
