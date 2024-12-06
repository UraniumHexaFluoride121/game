package level.procedural.types;

import foundation.Main;
import foundation.MainPanel;
import foundation.math.BezierCurve3;
import foundation.math.FunctionalWeightedRandom;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.procedural.Layout;
import level.procedural.generator.*;
import level.procedural.marker.LayoutMarker;
import loader.AssetManager;
import physics.StaticHitBox;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ForestTypes {
    public static final GeneratorType FOREST_BRANCH = new GeneratorType("forest_branch", () -> new ProceduralGenerator((gen, lm, type) -> {
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
        lm.addBound(mainBound.copy().expand(3, 1, 0, 0), BoundType.OBSTRUCTION);
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
            blocks = curve.forEachBlockNearCurve(2, (point, dist) -> dist < GeneratorTypeFunctions.FOREST_BRANCH_CURVE_SIZE.apply(length, point));
        } else {
            length = Main.BLOCKS_X - 1 - lm.pos.x;
            blocks = curve.forEachBlockNearCurve(2, (point, dist) -> dist < GeneratorTypeFunctions.FOREST_BRANCH_CURVE_SIZE.apply(length, point));
        }
        blocks.generateBlocks(type.getString(0), gen);
        gen.addData("blocks", blocks);
    }, (gen, lm, type) -> {
        BlockCollection blocks = gen.getData("blocks", BlockCollection.class);
        String blockName = type.getString(0);
        GenUtil.generateJumpValidation(blocks.getBlockHeights(), gen, lm, AssetManager.blockHitBoxes.get(blockName), AssetManager.blockFriction.get(blockName));
    }, (gen, lm, type) -> {
        if (lm.pos.y < MainPanel.level.getRegionTop()) {
            BezierCurve3 curve = gen.getData("curve", BezierCurve3.class);
            AtomicReference<ObjPos> pos = new AtomicReference<>();
            gen.addMarker(type.getString(1), gen.randomPosAbove(() -> {
                pos.set(curve.sampleCurve(gen.randomFloat(0, 1)));
                return pos.get();
            }, 0.3f, 1.3f, 6, 12, 2.5f, 20));
            if (Layout.DEBUG_RENDER && Layout.DEBUG_RENDER_BEZIER_CURVES)
                gen.addMarker("debug", pos.get());
        }
    }),
            GeneratorType.storeString("woodBlock")
                    .andThen(GeneratorType.storeString("generateNextPlatformAs")), true
    );
    public static final GeneratorType FOREST_ISLAND_DEFAULT = PresetTypes.defaultIsland("forest_island_default", 3, 6,
            GeneratorType.storeInt("forceAwayFromBorderProximity")
                    .andThen(GeneratorType.storeString("block"))
                    .andThen(GeneratorType.storeString("generateNextPlatformAs"))
                    .andThen(GeneratorType.storeString("generateExtraPlatformAs")),
            GeneratorType.generateDefault(0, 1)
                    .andThen(GeneratorType.generateAround(0, 2, 15, 0.3f)), new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(0, s -> s.lastValue() == 0 ? 2f : 12)
                    .add(1, s -> 15f)
                    .add(2, s -> 15f)
                    .add(3, s -> s.lastSize() > 8 ? 2f : 0),
            new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(0, s -> s.lastValue() == 0 ? 1f : 4)
                    .add(1, s -> 6f)
                    .add(2, s -> 10f)
                    .add(3, s -> 15f)
                    .add(4, s -> s.lastSize() > 8 ? 5f : 2)
    ).get();
    public static final GeneratorType FOREST_ISLAND_DEFAULT_SMALL = PresetTypes.defaultIsland("forest_island_default_small", 1, 2,
            GeneratorType.storeInt("forceAwayFromBorderProximity")
                    .andThen(GeneratorType.storeString("block"))
                    .andThen(GeneratorType.storeString("generateNextPlatformAs")),
            GeneratorType.generateDefault(0, 1), new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(0, s -> s.lastValue() == 0 ? 1f : 8f)
                    .add(1, s -> 8f), null
    ).get();
    public static final GeneratorType FOREST_ISLAND_DEFAULT_SMALL_EXTRA = PresetTypes.defaultIsland("forest_island_default_small_extra", 1, 2,
            GeneratorType.storeInt("forceAwayFromBorderProximity")
                    .andThen(GeneratorType.storeString("block")),
            GeneratorType.generateNothing(), new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(0, s -> s.lastValue() == 0 ? 2f : 8f)
                    .add(1, s -> 10f), null
    ).get();
    public static final GeneratorType FOREST_ISLAND_VERTICAL = PresetTypes.verticalIsland("forest_island_vertical", 2, 4, 6,
            GeneratorType.storeInt("forceAwayFromBorderProximity")
                    .andThen(GeneratorType.storeString("block"))
                    .andThen(GeneratorType.storeString("generateNextPlatformAs"))
                    .andThen(GeneratorType.storeString("generateExtraPlatformAs")),
            GeneratorType.generateDefault(0, 1)
                    .andThen(GeneratorType.generateAround(0, 2, 15, 0.3f)),
            new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(1, s -> 10f)
                    .add(2, s -> 12f)
                    .add(3, s -> 8f)
                    .add(4, s -> s.lastSize() > 8 ? 2f : 0),
            new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(-3, s -> 1f)
                    .add(-2, s -> 4f)
                    .add(-1, s -> 4f)
                    .add(0, s -> 8f)
                    .add(1, s -> 4f)
                    .add(2, s -> 4f)
                    .add(3, s -> 1f)
    ).get();
    public static final GeneratorType FOREST_ISLAND_VERTICAL_LARGE = PresetTypes.verticalIsland("forest_island_vertical_large", 1, 2, 8, 15,
            GeneratorType.storeInt("forceAwayFromBorderProximity")
                    .andThen(GeneratorType.storeString("block"))
                    .andThen(GeneratorType.storeString("generateNextPlatformAs")),
            GeneratorType.generateDefault(0, 1),
            new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(2, s -> 5f)
                    .add(3, s -> 10f)
                    .add(4, s -> 15f)
                    .add(5, s -> 12f)
                    .add(6, s -> s.lastSize() > 8 ? 2f : 0),
            new FunctionalWeightedRandom<Integer, GenUtil.StackRandomData>()
                    .add(-3, s -> 4f)
                    .add(-2, s -> 4f)
                    .add(-1, s -> 4f)
                    .add(1, s -> 4f)
                    .add(2, s -> 4f)
                    .add(3, s -> 4f)
    ).get();
}
