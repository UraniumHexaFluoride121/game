package level.procedural.types;

import foundation.math.FunctionalWeightedRandom;
import level.procedural.generator.*;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import loader.JsonObject;
import physics.StaticHitBox;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static level.procedural.generator.GeneratorType.*;

public abstract class PresetTypes {
    public static Supplier<GeneratorType> verticalIsland(String name, int upwardOffset, int minSize, int maxSize, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorLMFunction generation, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weights, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> offset) {
        return verticalIsland(name, 2, upwardOffset, minSize, maxSize, jsonParser, generation, weights, offset);
    }

    public static Supplier<GeneratorType> verticalIsland(String name, int obstructionWidth, int upwardOffset, int minSize, int maxSize, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorLMFunction generation, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weights, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> offset) {
        return () -> new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            int sizeUp = gen.randomInt(minSize, maxSize) + upwardOffset;
            int sizeDown = gen.randomInt(minSize, maxSize) - upwardOffset;
            GenUtil.VerticalStackData verticalStack = new GenUtil.VerticalStackData(sizeUp, sizeDown, gen.random(), weights, offset);
            StaticHitBox blocksBound = verticalStack.getBound(lm.pos);
            lm.addBound(blocksBound, BoundType.BLOCKS);
            lm.addBound(blocksBound, BoundType.COLLISION);
            lm.addBound(new StaticHitBox(verticalStack.up() + 3f, verticalStack.down() + 2f, verticalStack.left(.5f), verticalStack.right(.5f) + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(new StaticHitBox(verticalStack.up() * 0.7f, verticalStack.down() * 0.5f, verticalStack.left() + obstructionWidth, verticalStack.right() + obstructionWidth + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(blocksBound.expand(3, 7), BoundType.OVERCROWDING);
            gen.addData("blocks", verticalStack);
        }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
                .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)),
                generateBlockCollection(0, "blocks"),
                generateBlockCollectionValidation(0, "blocks"),
                generation), jsonParser, true
        );
    }

    public static Supplier<GeneratorType> defaultIsland(String name, int minSize, int maxSize, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorLMFunction generation, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weightsLower, FunctionalWeightedRandom<Integer, GenUtil.StackRandomData> weightsUpper) {
        return () -> new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            int sizeLeft = gen.randomInt(minSize, maxSize);
            int sizeRight = gen.randomInt(minSize, maxSize);

            GenUtil.StackData stack = GenUtil.blockStackInward(sizeLeft, sizeRight, true, gen.random(), weightsLower);
            int offset = ((int) ((sizeLeft + sizeRight) * gen.randomFloat(-0.4f, 0.4f)));
            BlockCollection blocks;
            if (weightsUpper != null) {
                GenUtil.StackData topStack = GenUtil.blockStackInward(
                        Math.min(sizeLeft, (int) (sizeLeft * gen.randomFloat(0.2f, 0.8f)) + offset),
                        Math.min(sizeRight, (int) (sizeRight * gen.randomFloat(0.2f, 0.8f)) - offset),
                        false, gen.random(), weightsUpper);
                blocks = stack.join(topStack, 0, 1);
            } else
                blocks = stack;
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
                generation), jsonParser, true
        );
    }
}
