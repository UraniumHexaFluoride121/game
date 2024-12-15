package level.procedural.types;

import foundation.math.FunctionalWeightedRandom;
import foundation.math.WeightedRandom;
import level.procedural.collections.*;
import level.procedural.generator.BoundType;
import level.procedural.generator.GeneratorFunction;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.GeneratorLMFunction;
import level.procedural.marker.LayoutMarker;
import loader.JsonObject;
import physics.StaticHitBox;

import java.util.function.BiConsumer;

import static level.procedural.generator.GeneratorType.*;

public abstract class PresetTypes {
    public static GeneratorType islandCluster(String name, int maxDistance, int minIslands, int maxIslands, WeightedRandom<Integer> width, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorFunction blockGeneration, GeneratorLMFunction lmGeneration, FunctionalWeightedRandom<Integer, StackRandomData> weights) {
        return new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            IslandCluster blocks = new IslandCluster(maxDistance, minIslands, maxIslands, gen.random(), width, weights);
            StaticHitBox blocksBound = blocks.getBound(lm.pos);
            lm.addBound(blocksBound, BoundType.BLOCKS);
            blocks.islandBounds.forEach(b -> lm.addBound(b.copy().offset(lm.pos), BoundType.COLLISION));
            blocks.expandedIslandBounds.forEach(b -> lm.addBound(b.copy().offset(lm.pos), BoundType.OBSTRUCTION));
            lm.addBound(blocksBound.expand(2, 2), BoundType.OVERCROWDING);
            gen.addData("blocks", blocks);
        }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
                .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)),
                blockGeneration,
                generateIslandClusterValidation(0, "blocks"),
                lmGeneration), jsonParser, true
        );
    }

    public static GeneratorType verticalIsland(String name, int upwardOffset, int minSize, int maxSize, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorFunction blockGeneration, GeneratorLMFunction lmGeneration, FunctionalWeightedRandom<Integer, StackRandomData> weights, FunctionalWeightedRandom<Integer, StackRandomData> offset) {
        return verticalIsland(name, 2, upwardOffset, minSize, maxSize, jsonParser, blockGeneration, lmGeneration, weights, offset);
    }

    public static GeneratorType verticalIsland(String name, int obstructionWidth, int upwardOffset, int minSize, int maxSize, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorFunction blockGeneration, GeneratorLMFunction lmGeneration, FunctionalWeightedRandom<Integer, StackRandomData> weights, FunctionalWeightedRandom<Integer, StackRandomData> offset) {
        return new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            int sizeUp = gen.randomInt(minSize, maxSize) + upwardOffset;
            int sizeDown = gen.randomInt(minSize, maxSize) - upwardOffset;
            VerticalStackData verticalStack = new VerticalStackData(sizeUp, sizeDown, gen.random(), weights, offset);
            StaticHitBox blocksBound = verticalStack.getBound(lm.pos);
            lm.addBound(blocksBound, BoundType.BLOCKS);
            lm.addBound(blocksBound, BoundType.COLLISION);
            lm.addBound(new StaticHitBox(verticalStack.up() + 3f, verticalStack.down() + 2f, verticalStack.left(.5f), verticalStack.right(.5f) + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(new StaticHitBox(verticalStack.up() * 0.7f, verticalStack.down() * 0.5f, verticalStack.left() + obstructionWidth, verticalStack.right() + obstructionWidth + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(blocksBound.expand(3, 7), BoundType.OVERCROWDING);
            gen.addData("blocks", verticalStack);
        }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
                .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)),
                blockGeneration,
                generateBlockCollectionValidation(0, "blocks"),
                lmGeneration), jsonParser, true
        );
    }

    public static GeneratorType defaultIsland(String name, int minSize, int maxSize, BiConsumer<GeneratorType, JsonObject> jsonParser, GeneratorFunction blockGeneration, GeneratorLMFunction lmGeneration, FunctionalWeightedRandom<Integer, StackRandomData> weightsLower, FunctionalWeightedRandom<Integer, StackRandomData> weightsUpper) {
        return new GeneratorType(name, () -> new ProceduralGenerator((gen, lm, type) -> {
            int sizeLeft = gen.randomInt(minSize, maxSize);
            int sizeRight = gen.randomInt(minSize, maxSize);

            HorizontalStackData stack = HorizontalStackData.blockStackInward(sizeLeft, sizeRight, true, gen.random(), weightsLower);
            int offset = ((int) ((sizeLeft + sizeRight) * gen.randomFloat(-0.4f, 0.4f)));
            BlockCollection blocks;
            if (weightsUpper != null) {
                HorizontalStackData topStack = HorizontalStackData.blockStackInward(
                        Math.min(sizeLeft, (int) (sizeLeft * gen.randomFloat(0.2f, 0.8f)) + offset),
                        Math.min(sizeRight, (int) (sizeRight * gen.randomFloat(0.2f, 0.8f)) - offset),
                        false, gen.random(), weightsUpper);
                blocks = stack.joinAndCopy(topStack, 0, 1);
            } else
                blocks = stack;
            StaticHitBox blocksBound = blocks.getBound(lm.pos);
            lm.addBound(blocksBound, BoundType.BLOCKS);
            lm.addBound(blocksBound, BoundType.COLLISION);
            lm.addBound(new StaticHitBox(blocks.up() + 3, blocks.down() + 2, stack.leftLayer(.7f), stack.rightLayer(.7f) + 1, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(new StaticHitBox(1, stack.height(.4f), blocks.left() + 2, blocks.right() + 3, lm.pos), BoundType.OBSTRUCTION);
            lm.addBound(blocksBound.expand(3, 7), BoundType.OVERCROWDING);
            gen.addData("blocks", blocks);
        }, LayoutMarker.isNotColliding(BoundType.OBSTRUCTION)
                .and(LayoutMarker.isNotColliding(BoundType.OVERCROWDING)),
                blockGeneration,
                generateBlockCollectionValidation(0, "blocks"),
                lmGeneration), jsonParser, true
        );
    }
}
