package level.procedural;

import foundation.Main;

import java.util.function.Supplier;

public enum GeneratorType {
    FOREST_BRANCH("forest_branch", () -> new ProceduralGenerator((gen, lm) -> {
        gen.addBlock("static_example", lm.pos);
        int firstOffset = gen.randomInt(-1, 1);
        int secondOffset = firstOffset == 0 ? gen.randomInt(-1, 1) : gen.randomInt(0, -firstOffset);
        //If this isn't done, there is a bias for the second offset being 0
        boolean flipOffsets = gen.randomBoolean(0.5f);
        if (lm.pos.x < Main.BLOCKS_X / 2f) {
            gen.lineOfBlocks(
                    0,
                    (int) lm.pos.x,
                    (int) lm.pos.y + (flipOffsets ? secondOffset : firstOffset),
                    pos -> "static_example"
            );
            gen.lineOfBlocks(
                    (int) lm.pos.x,
                    (int) lm.pos.x + gen.randomInt(3, 5),
                    (int) lm.pos.y + (flipOffsets ? firstOffset : secondOffset),
                    pos -> "static_example"
            );
        }
    }));

    public final String s;
    public final Supplier<ProceduralGenerator> generator;

    GeneratorType(String s, Supplier<ProceduralGenerator> generator) {
        this.s = s;
        this.generator = generator;
    }

    public static GeneratorType getGeneratorType(String s) {
        for (GeneratorType type : GeneratorType.values()) {
            if (type.s.equals(s))
                return type;
        }
        throw new IllegalArgumentException("Unknown generator type: " + s);
    }
}
