package level.procedural.marker;

import foundation.math.ObjPos;
import level.procedural.collections.BlockCollection;
import level.procedural.generator.GeneratorType;
import level.procedural.generator.ProceduralGenerator;

public interface GeneratorLMFunction {
    void generateMarkers(ProceduralGenerator gen, LayoutMarker lm, GeneratorType type);

    static GeneratorLMFunction generateNothing() {
        return (gen, lm, type) -> {

        };
    }

    default GeneratorLMFunction andThen(GeneratorLMFunction other) {
        return ((gen, lm, type) -> {
            generateMarkers(gen, lm, type);
            other.generateMarkers(gen, lm, type);
        });
    }

    static GenerateAboveLMFunctionFactory generateAbove(int borderProximityIndex, int platformNameIndex) {
        return new GenerateAboveLMFunctionFactory(borderProximityIndex, platformNameIndex);
    }

    static GenerateAboveLMFunctionFactory generateAround(int borderProximityIndex, int platformNameIndex, int maxLength, float probability) {
        return new GenerateAboveLMFunctionFactory(borderProximityIndex, platformNameIndex)
                .setMinAngle(-1)
                .setMaxAngle(1.6f)
                .setMinLength(7)
                .setXLengthMultiplier(1)
                .setMinHeight(30)
                .setMaxLength(maxLength)
                .setProbability(probability);
    }

    class GenerateAboveLMFunctionFactory {
        private final int borderProximityIndex;
        private final int platformNameIndex;
        private float minAngle = 0.2f, maxAngle = 1.6f, minLength = 6, maxLength = 15, xLengthMultiplier = 2.5f;
        private ObjPos offset = new ObjPos();
        private String topOffsetCollection = null;
        private float topOffsetMultiplier = 0;
        private float probability = 1;
        private int minHeight = 0;

        public GenerateAboveLMFunctionFactory(int borderProximityIndex, int platformNameIndex) {
            this.borderProximityIndex = borderProximityIndex;
            this.platformNameIndex = platformNameIndex;
        }

        public GeneratorLMFunction finalise() {
            if (topOffsetCollection == null) {
                return (gen, lm, type) -> {
                    if (probability != 1 && !gen.randomBoolean(probability))
                        return;
                    ObjPos origin = offset.copy().add(lm.pos);
                    if (origin.y < lm.level.getRegionTop() && origin.y > minHeight) {
                        int borderProximityLimit = type.getInt(borderProximityIndex);
                        gen.addMarker(type.getString(platformNameIndex), gen.randomPosAbove(origin, minAngle, maxAngle, minLength, maxLength, xLengthMultiplier, borderProximityLimit));
                    }
                };
            } else {
                return (gen, lm, type) -> {
                    if (probability != 1 && gen.randomBoolean(probability))
                        return;
                    BlockCollection blocks = gen.getData(topOffsetCollection, BlockCollection.class);
                    ObjPos origin = offset.copy().add(lm.pos).addY(blocks.bound.up * topOffsetMultiplier).toInt();
                    if (origin.y < lm.level.getRegionTop() && origin.y > minHeight) {
                        int borderProximityLimit = type.getInt(borderProximityIndex);
                        gen.addMarker(type.getString(platformNameIndex), gen.randomPosAbove(origin, minAngle, maxAngle, minLength, maxLength, xLengthMultiplier, borderProximityLimit));
                    }
                };
            }
        }

        public GenerateAboveLMFunctionFactory setMinHeight(int minHeight) {
            this.minHeight = minHeight;
            return this;
        }

        public GenerateAboveLMFunctionFactory setMinAngle(float minAngle) {
            this.minAngle = minAngle;
            return this;
        }

        public GenerateAboveLMFunctionFactory setMaxAngle(float maxAngle) {
            this.maxAngle = maxAngle;
            return this;
        }

        public GenerateAboveLMFunctionFactory setMinLength(float minLength) {
            this.minLength = minLength;
            return this;
        }

        public GenerateAboveLMFunctionFactory setMaxLength(float maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public GenerateAboveLMFunctionFactory setXLengthMultiplier(float xLengthMultiplier) {
            this.xLengthMultiplier = xLengthMultiplier;
            return this;
        }

        public GenerateAboveLMFunctionFactory setOffset(ObjPos offset) {
            this.offset = offset;
            return this;
        }

        public GenerateAboveLMFunctionFactory setProbability(float probability) {
            this.probability = probability;
            return this;
        }

        public GenerateAboveLMFunctionFactory setTopOffsetFromCollection(String collectionDataName, float multiplier) {
            topOffsetCollection = collectionDataName;
            topOffsetMultiplier = multiplier;
            return this;
        }
    }
}
