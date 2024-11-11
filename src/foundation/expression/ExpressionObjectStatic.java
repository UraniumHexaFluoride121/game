package foundation.expression;

import java.util.function.Function;

public class ExpressionObjectStatic<T, R> extends ExpressionObject<T, R> {
    public final R value;

    public ExpressionObjectStatic(Class<R> returnType, Function<T, R> f, R value) {
        super(returnType, f);
        this.value = value;
    }
}
