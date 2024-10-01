package foundation.expression;

import java.util.ArrayList;
import java.util.function.Function;

public class ExpressionFunction<T> {
    public final String name;
    public final Function<ArrayList<Function<T, Object>>, Function<T, Object>> definition;

    public ExpressionFunction(String name, Function<ArrayList<Function<T, Object>>, Function<T, Object>> definition) {
        this.name = name;
        this.definition = definition;
    }
}
