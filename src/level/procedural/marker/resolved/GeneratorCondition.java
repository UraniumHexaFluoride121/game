package level.procedural.marker.resolved;

import foundation.MainPanel;
import foundation.expression.Expression;
import foundation.expression.ExpressionValue;

public class GeneratorCondition extends Expression<GeneratorConditionData> {
    public static final GeneratorCondition parser = new GeneratorCondition();

    protected GeneratorCondition() {
        super();
        addValue(new ExpressionValue<>("this", o -> o.marker));
        addValue(new ExpressionValue<>("region", o -> MainPanel.level.getRegion(o.marker.pos).toString()));
    }
}
