package foundation.expression;

import java.util.ArrayList;
import java.util.function.Function;

public class ExpressionFunction<T> {
    public final String name;
    public final Function<ArrayList<ExpressionObject<T, ?>>, ExpressionObject<T, ?>> definition;

    public ExpressionFunction(String name, Function<ArrayList<ExpressionObject<T, ?>>, ExpressionObject<T, ?>> definition) {
        this.name = name;
        this.definition = definition;
    }
}
