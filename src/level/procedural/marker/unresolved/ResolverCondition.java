package level.procedural.marker.unresolved;

import foundation.MainPanel;
import foundation.expression.Expression;
import foundation.expression.ExpressionValue;

public class ResolverCondition extends Expression<ResolverConditionData> {
    public static final ResolverCondition parser = new ResolverCondition();

    protected ResolverCondition() {
        super();
        addValue(new ExpressionValue<>("this", o -> o.marker));
        addValue(new ExpressionValue<>("region", o -> MainPanel.level.getRegion(o.marker.pos).toString()));
    }
}
