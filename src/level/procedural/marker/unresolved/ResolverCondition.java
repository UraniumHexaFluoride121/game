package level.procedural.marker.unresolved;

import foundation.MainPanel;
import foundation.expression.Expression;
import foundation.expression.ExpressionFunction;
import foundation.expression.ExpressionValue;
import foundation.math.ObjPos;
import level.procedural.generator.BoundType;
import level.procedural.generator.GeneratorValidation;
import level.procedural.marker.LayoutMarker;
import physics.StaticHitBox;

public class ResolverCondition extends Expression<ResolverConditionData> {
    public static final ResolverCondition parser = new ResolverCondition();

    protected ResolverCondition() {
        super();
        addValue(new ExpressionValue<>("this", o -> o.marker));
        addValue(new ExpressionValue<>("region", o -> MainPanel.level.getRegion(o.marker.pos).toString()));

        addFunction(new ExpressionFunction<>("posOffset", args -> {
            return o -> {
                int x = getArg(0, args, o, Integer.class), y = getArg(1, args, o, Integer.class);
                return o.marker.pos.copy().add(x, y);
            };
        }));
        addFunction(new ExpressionFunction<>("testCollision", args -> {
            return o -> {
                ObjPos from = getArg(0, args, o, ObjPos.class), to = getArg(1, args, o, ObjPos.class);
                BoundType type = BoundType.getBoundType(getArg(2, args, o, String.class));
                StaticHitBox box = new StaticHitBox(from, to);
                return GeneratorValidation.validate(o.marker, o.marker.genType, LayoutMarker.isNotColliding(box, type));
            };
        }));
    }
}
