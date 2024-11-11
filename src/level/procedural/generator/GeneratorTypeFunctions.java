package level.procedural.generator;

import java.util.function.BinaryOperator;

public abstract class GeneratorTypeFunctions {
    //Describes the distance at which blocks generate from the curve for a given point T, if the curve is of length L
    public final static BinaryOperator<Float> FOREST_BRANCH_CURVE_SIZE = (l, t) -> (1 - t) * l / 20 + 0.7f + (1 - l / 20); //(length, point) -> distance
}
