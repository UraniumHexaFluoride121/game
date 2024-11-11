package level.procedural.marker.resolved;

import foundation.MainPanel;
import foundation.expression.Expression;
import foundation.expression.ExpressionObject;
import foundation.expression.ExpressionValue;
import level.procedural.marker.LayoutMarker;

public class GeneratorCondition extends Expression<GeneratorConditionData> {
    public static final GeneratorCondition parser = new GeneratorCondition();

    protected GeneratorCondition() {
        super();
        addValue(new ExpressionValue<>("this", new ExpressionObject<>(LayoutMarker.class, o -> o.marker)));
        addValue(new ExpressionValue<>("region", new ExpressionObject<>(String.class, o -> MainPanel.level.getRegion(o.marker.pos).toString())));
    }
}
