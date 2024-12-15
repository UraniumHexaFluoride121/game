package level.procedural.collections;

import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.LayoutMarker;

@FunctionalInterface
public interface BlockCollectionAction {
    BlockCollection generate(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type, BlockCollection collection);

    default BlockCollectionAction andThen(BlockCollectionAction other) {
        return (gen, lm, type, collection) ->
                other.generate(gen, lm, type, generate(gen, lm, type, collection));
    }
}
