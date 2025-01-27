package render.texture.ct;

import foundation.expression.Expression;
import foundation.expression.ExpressionFunction;
import foundation.expression.ExpressionObject;
import foundation.expression.ExpressionValue;
import foundation.math.ObjPos;
import level.ObjectLayer;
import level.objects.BlockLike;

import java.util.HashSet;

public class CTExpression extends Expression<CTExpressionData> {
    public static final CTExpression parser = new CTExpression();

    protected CTExpression() {
        super();
        addValue(new ExpressionValue<>("ur", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(1, 1))));
        addValue(new ExpressionValue<>("ul", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(-1, 1))));
        addValue(new ExpressionValue<>("dr", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(1, -1))));
        addValue(new ExpressionValue<>("dl", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(-1, -1))));
        addValue(new ExpressionValue<>("u", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(0, 1))));
        addValue(new ExpressionValue<>("d", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(0, -1))));
        addValue(new ExpressionValue<>("l", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(-1, 0))));
        addValue(new ExpressionValue<>("r", new ExpressionObject<>(ObjPos.class, o -> o.b.getPos().copy().add(1, 0))));
        addValue(new ExpressionValue<>("this", new ExpressionObject<>(BlockLike.class, o -> o.b)));

        addFunction(new ExpressionFunction<>("intProperty", args -> {
            if (args.size() != 1)
                throw new IllegalArgumentException("Incorrectly formatted expression, the \"intProperty\" function requires exactly one argument");
            return new ExpressionObject<>(Integer.class, o -> {
                String name = getArg(0, args, o, String.class);
                return o.b.getProperty(Integer.class, name);
            });
        }));
        addFunction(new ExpressionFunction<>("booleanProperty", args -> {
            if (args.size() != 1)
                throw new IllegalArgumentException("Incorrectly formatted expression, the \"booleanProperty\" function requires exactly one argument");
            return new ExpressionObject<>(Boolean.class, o -> {
                String name = getArg(0, args, o, String.class);
                return o.b.getProperty(Boolean.class, name);
            });
        }));
        addFunction(new ExpressionFunction<>("stringProperty", args -> {
            if (args.size() != 1)
                throw new IllegalArgumentException("Incorrectly formatted expression, the \"stringProperty\" function requires exactly one argument");
            return new ExpressionObject<>(String.class, o -> {
                String name = getArg(0, args, o, String.class);
                return o.b.getProperty(String.class, name);
            });
        }));
        //Get the block at the specified position on the specified layer. Returns block name
        addFunction(new ExpressionFunction<>("block", args -> {
            if (args.size() == 2)
                return new ExpressionObject<>(BlockLike.class, o -> {
                    ObjPos pos = getArg(0, args, o, ObjPos.class);
                    return o.l.getBlock(ObjectLayer.getObjectLayer(getArg(1, args, o, String.class)), (int) pos.x, (int) pos.y);
                });
            if (args.size() == 1)
                return new ExpressionObject<>(BlockLike.class, o -> {
                    ObjPos pos = getArg(0, args, o, ObjPos.class);
                    return o.l.getBlock(o.b.getLayer(), (int) pos.x, (int) pos.y);
                });
            throw new IllegalArgumentException("Incorrectly formatted expression, the \"block\" function requires two arguments");
        }));
        //Does any block exist at the specified position, on any layer, or on specific layers if specified? Returns boolean
        addFunction(new ExpressionFunction<>("existsBlock", args -> {
            if (args.size() == 1)
                return new ExpressionObject<>(Boolean.class, o -> {
                    ObjPos pos = getArg(0, args, o, ObjPos.class);
                    if (o.l.outOfBounds(((int) pos.x), ((int) pos.y)))
                        return true;
                    for (ObjectLayer layer : ObjectLayer.values()) {
                        if (o.l.getBlock(layer, (int) pos.x, (int) pos.y) != null)
                            return true;
                    }
                    return false;
                });
            else return new ExpressionObject<>(Boolean.class, o -> {
                ObjPos pos = getArg(0, args, o, ObjPos.class);
                if (o.l.outOfBounds(((int) pos.x), ((int) pos.y)))
                    return true;
                HashSet<ObjectLayer> layers = new HashSet<>();
                for (int i = 1; i < args.size(); i++) {
                    layers.add(ObjectLayer.getObjectLayer(getArg(i, args, o, String.class)));
                }
                for (ObjectLayer layer : layers) {
                    if (!layer.addToStatic)
                        throw new IllegalArgumentException("Incorrectly formatted expression, function \"existsBlock\" was provided a non-static ObjectLayer as an argument");
                    if (o.l.getBlock(layer, (int) pos.x, (int) pos.y) != null)
                        return true;
                }
                return false;
            });
        }));
        //Same as existsBlock, but ignores non-collision blocks
        addFunction(new ExpressionFunction<>("existsBlockWithCollision", args -> {
            if (args.size() == 1)
                return new ExpressionObject<>(Boolean.class, o -> {
                    ObjPos pos = getArg(0, args, o, ObjPos.class);
                    if (o.l.outOfBounds(((int) pos.x), ((int) pos.y)))
                        return true;
                    for (ObjectLayer layer : ObjectLayer.values()) {
                        BlockLike block = o.l.getBlock(layer, (int) pos.x, (int) pos.y);
                        if (block != null && block.hasCollision())
                            return true;
                    }
                    return false;
                });
            else return new ExpressionObject<>(Boolean.class, o -> {
                ObjPos pos = getArg(0, args, o, ObjPos.class);
                if (o.l.outOfBounds(((int) pos.x), ((int) pos.y)))
                    return true;
                HashSet<ObjectLayer> layers = new HashSet<>();
                for (int i = 1; i < args.size(); i++) {
                    layers.add(ObjectLayer.getObjectLayer(getArg(i, args, o, String.class)));
                }
                for (ObjectLayer layer : layers) {
                    if (!layer.addToStatic)
                        throw new IllegalArgumentException("Incorrectly formatted expression, function \"existsBlockWithCollision\" was provided a non-static ObjectLayer as an argument");
                    BlockLike block = o.l.getBlock(layer, (int) pos.x, (int) pos.y);
                    if (block != null && block.hasCollision())
                        return true;
                }
                return false;
            });
        }));
    }
}
