package level.procedural.types;

import foundation.MainPanel;
import foundation.math.FunctionalWeightedRandom;
import level.procedural.generator.*;
import level.procedural.marker.LayoutMarker;
import physics.StaticHitBox;

import java.util.function.Supplier;

import static level.procedural.generator.GeneratorType.*;

public abstract class PresetTypes {
    public static Supplier<GeneratorType> verticalIsland(String name, int upwardOffset, int minSize, int maxSize, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weights) {
        return verticalIsland(name, 2, upwardOffset, minSize, maxSize, weights);
    }
    public static Supplier<GeneratorType> verticalIsland(String name, int obstructionWidth, int upwardOffset, int minSize, int maxSize, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weights) {
        return () -> new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            int sizeUp = gen.randomInt(minSize, maxSize) + upwardOffset;
            int sizeDown = gen.randomInt(minSize, maxSize) - upwardOffset;
            GenUtil.VerticalStackData verticalStack = new GenUtil.VerticalStackData(sizeUp, sizeDown, gen.random(), weights);
            StaticHitBox blocksBound = verticalStack.getBound(lm.pos);
            lm.addBound(blocksBound, BoundType.BLOCKS);
            lm.addBound(blocksBound, BoundType.COLLISION);
            lm.addBound(new StaticHitBox(verticalStack.up() + 1.5f, verticalStack.down() + .5f, verticalStack.left(.5f), verticalStack.right(.5f) + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(new StaticHitBox(verticalStack.up() * 0.7f, verticalStack.down() * 0.5f, verticalStack.left() + obstructionWidth, verticalStack.right() + obstructionWidth + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(blocksBound.expand(3, 7), BoundType.OVERCROWDING);
            gen.addData("blocks", verticalStack);
        }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
                .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)),
                generateBlockCollection(0, "blocks"),
                generateBlockCollectionValidation(0, "blocks"),
                (gen, lm, type) -> {
                    if (lm.pos.y < MainPanel.level.getRegionTop()) {
                        int borderProximityLimit = type.getInt(0);
                        gen.addMarker(type.getString(1), gen.randomPosAbove(lm.pos.copy().addY(gen.getData("blocks", BlockCollection.class).up()), 0.2f, 1.6f, 6, 15, 2.5f, borderProximityLimit));
                    }
                }),
                GeneratorType.storeInt("forceAwayFromBorderProximity")
                        .andThen(GeneratorType.storeString("block"))
                        .andThen(GeneratorType.storeString("generateNextPlatformAs")), true
        );
    }

    public static Supplier<GeneratorType> defaultIsland(String name, int minSize, int maxSize, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weightsLower, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weightsUpper) {
        return () -> new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            int sizeLeft = gen.randomInt(minSize, maxSize);
            int sizeRight = gen.randomInt(minSize, maxSize);

            GenUtil.StackData stack = GenUtil.blockStackInward(sizeLeft, sizeRight, true, gen.random(), weightsLower);
            int offset = ((int) ((sizeLeft + sizeRight) * gen.randomFloat(-0.4f, 0.4f)));
            GenUtil.StackData topStack = GenUtil.blockStackInward(
                    Math.min(sizeLeft, (int) (sizeLeft * gen.randomFloat(0.2f, 0.8f)) + offset),
                    Math.min(sizeRight, (int) (sizeRight * gen.randomFloat(0.2f, 0.8f)) - offset),
                    false, gen.random(), weightsUpper);
            BlockCollection blocks = stack.join(topStack, 0, 1);
            StaticHitBox blocksBound = blocks.getBound(lm.pos);
            lm.addBound(blocksBound, BoundType.BLOCKS);
            lm.addBound(blocksBound, BoundType.COLLISION);
            lm.addBound(new StaticHitBox(blocks.up() + 3, blocks.down() + 2, stack.leftLayer(.7f), stack.rightLayer(.7f) + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(new StaticHitBox(1, stack.height(.4f), blocks.left() + 3, blocks.right() + 4, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(blocksBound.expand(3, 7), BoundType.OVERCROWDING);
            gen.addData("blocks", blocks);
        }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
                .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)),
                generateBlockCollection(0, "blocks"),
                generateBlockCollectionValidation(0, "blocks"),
                (gen, lm, type) -> {
                    if (lm.pos.y < MainPanel.level.getRegionTop()) {
                        int borderProximityLimit = type.getInt(0);
                        gen.addMarker(type.getString(1), gen.randomPosAbove(lm, 0.2f, 1.6f, 6, 15, 2.5f, borderProximityLimit));
                    }
                }),
                GeneratorType.storeInt("forceAwayFromBorderProximity")
                        .andThen(GeneratorType.storeString("block"))
                        .andThen(GeneratorType.storeString("generateNextPlatformAs")), true
        );
    }
}
