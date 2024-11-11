package foundation.expression;

public class ExpressionValue<T> {
    public final String name;
    public final ExpressionObject<T, ?> value;

    public ExpressionValue(String name, ExpressionObject<T, ?> value) {
        this.name = name;
        this.value = value;
    }
}
