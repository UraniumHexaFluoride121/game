package foundation.expression;

import java.util.function.Function;

public class ExpressionValue<T> {
    public final String name;
    public final Function<T, Object> value;

    public ExpressionValue(String name, Function<T, Object> value) {
        this.name = name;
        this.value = value;
    }
}
