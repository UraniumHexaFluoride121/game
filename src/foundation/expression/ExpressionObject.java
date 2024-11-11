package foundation.expression;

import java.util.function.Function;

public class ExpressionObject<T, R> {
    public final Class<R> returnType;
    public final Function<T, R> f;

    public ExpressionObject(Class<R> returnType, Function<T, R> f) {
        this.returnType = returnType;
        this.f = f;
    }
}
