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
}
