package level.procedural.marker.unresolved;

import foundation.MainPanel;
import foundation.expression.Expression;
import foundation.expression.ExpressionFunction;
import foundation.expression.ExpressionObject;
import foundation.expression.ExpressionValue;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.RandomType;
import level.procedural.generator.BoundType;
import level.procedural.generator.GeneratorValidation;
import level.procedural.marker.LayoutMarker;
import physics.StaticHitBox;

public class ResolverCondition extends Expression<ResolverConditionData> {
    public static final ResolverCondition parser = new ResolverCondition();

    protected ResolverCondition() {
        super();
        addValue(new ExpressionValue<>("this", new ExpressionObject<>(LayoutMarker.class, o -> o.marker)));
        addValue(new ExpressionValue<>("region", new ExpressionObject<>(String.class, o -> MainPanel.level.getRegion(o.marker.pos).toString())));

        addFunction(new ExpressionFunction<>("posOffset", args -> {
            return new ExpressionObject<>(ObjPos.class, o -> {
                float x = getArg(0, args, o, Number.class).floatValue(), y = getArg(1, args, o, Number.class).floatValue();
                return o.marker.pos.copy().add(x, y);
            });
        }));
        addFunction(new ExpressionFunction<>("testCollision", args -> {
            return new ExpressionObject<>(Boolean.class, o -> {
                ObjPos from = getArg(0, args, o, ObjPos.class), to = getArg(1, args, o, ObjPos.class);
                BoundType type = BoundType.getBoundType(getArg(2, args, o, String.class));
                StaticHitBox box = new StaticHitBox(from, to);
                return GeneratorValidation.validate(o.marker, o.marker.genType, LayoutMarker.isNotColliding(box, type));
            });
        }));
        addFunction(new ExpressionFunction<>("rand", args ->
                new ExpressionObject<>(Boolean.class, o ->
                        MathHelper.randBoolean(getArg(0, args, o, Number.class).floatValue(), o.l.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL))
                )
        ));
    }
}
