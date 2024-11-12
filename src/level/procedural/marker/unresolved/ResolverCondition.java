package level.procedural.marker.unresolved;

import foundation.MainPanel;
import foundation.expression.*;
import foundation.math.MathHelper;
import foundation.math.ObjPos;
import level.RandomType;
import level.procedural.generator.BoundType;
import level.procedural.generator.GeneratorValidation;
import level.procedural.marker.LayoutMarker;
import physics.StaticHitBox;

import java.util.function.Function;

public class ResolverCondition extends Expression<ResolverConditionData> {
    public static final ResolverCondition parser = new ResolverCondition();

    protected ResolverCondition() {
        super();
        addValue(new ExpressionValue<>("this", new ExpressionObject<>(LayoutMarker.class, o -> o.marker)));
        addValue(new ExpressionValue<>("region", new ExpressionObject<>(String.class, o -> MainPanel.level.getRegion(o.marker.pos).toString())));

        addFunction(new ExpressionFunction<>("posOffset", args -> {
            if (!args.get(0).returnType.equals(Number.class))
                throw new RuntimeException(argExceptionMessage(0, args, Number.class));
            if (!args.get(1).returnType.equals(Number.class))
                throw new RuntimeException(argExceptionMessage(1, args, Number.class));

            if (args.get(0) instanceof ExpressionObjectStatic<ResolverConditionData, ?> static0) {
                float x = ((Number) static0.value).floatValue();
                if (args.get(0) instanceof ExpressionObjectStatic<ResolverConditionData, ?> static1) {
                    float y = ((Number) static1.value).floatValue();
                    return new ExpressionObject<>(ObjPos.class, o -> {
                        return o.marker.pos.copy().add(x, y);
                    });
                }
                return new ExpressionObject<>(ObjPos.class, o -> {
                    float y = getArg(1, args, o, Number.class).floatValue();
                    return o.marker.pos.copy().add(x, y);
                });
            } else {
                if (args.get(0) instanceof ExpressionObjectStatic<ResolverConditionData, ?> static1) {
                    float y = ((Number) static1.value).floatValue();
                    return new ExpressionObject<>(ObjPos.class, o -> {
                        float x = getArg(0, args, o, Number.class).floatValue();
                        return o.marker.pos.copy().add(x, y);
                    });
                }
                return new ExpressionObject<>(ObjPos.class, o -> {
                    float x = getArg(0, args, o, Number.class).floatValue(), y = getArg(1, args, o, Number.class).floatValue();
                    return o.marker.pos.copy().add(x, y);
                });
            }
        }));
        addFunction(new ExpressionFunction<>("testCollision", args -> {
            if (!args.get(2).returnType.equals(String.class))
                throw new RuntimeException(argExceptionMessage(2, args, String.class));
            if (args.get(2) instanceof ExpressionObjectStatic<ResolverConditionData, ?> static2) {
                BoundType type = BoundType.getBoundType((String) static2.value);
                return new ExpressionObject<>(Boolean.class, o -> {
                    ObjPos from = getArg(0, args, o, ObjPos.class), to = getArg(1, args, o, ObjPos.class);
                    StaticHitBox box = new StaticHitBox(from, to);
                    return GeneratorValidation.validate(o.marker, o.marker.genType, LayoutMarker.isNotColliding(box, type));
                });
            } else {
                return new ExpressionObject<>(Boolean.class, o -> {
                    ObjPos from = getArg(0, args, o, ObjPos.class), to = getArg(1, args, o, ObjPos.class);
                    BoundType type = BoundType.getBoundType(getArg(2, args, o, String.class));
                    StaticHitBox box = new StaticHitBox(from, to);
                    return GeneratorValidation.validate(o.marker, o.marker.genType, LayoutMarker.isNotColliding(box, type));
                });
            }
        }));
        addFunction(new ExpressionFunction<>("rand", args -> {
            if (args.get(0).returnType.equals(Number.class)) {
                if (args.get(0) instanceof ExpressionObjectStatic<ResolverConditionData, ?> static0) {
                    float probability = ((Number) static0.value).floatValue();
                    return new ExpressionObject<>(Boolean.class, o ->
                            MathHelper.randBoolean(probability, o.l.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL))
                    );
                } else {
                    Function<ResolverConditionData, ?> f = args.get(0).f;
                    return new ExpressionObject<>(Boolean.class, o ->
                            MathHelper.randBoolean(((Number) f.apply(o)).floatValue(), o.l.randomHandler.getDoubleSupplier(RandomType.PROCEDURAL))
                    );
                }
            }
            throw new RuntimeException(argExceptionMessage(0, args, Number.class));
        }
        ));
    }
}
