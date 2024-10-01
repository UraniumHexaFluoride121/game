package render.texture.ct;

import foundation.ObjPos;
import foundation.expression.Expression;
import foundation.expression.ExpressionFunction;
import foundation.expression.ExpressionValue;
import level.ObjectLayer;
import level.objects.BlockLike;

import java.util.HashSet;

public class CTExpression extends Expression<CTExpressionData> {
    public static final CTExpression parser = new CTExpression();

    protected CTExpression() {
        super();
        addValue(new ExpressionValue<>("ur", o -> o.b.getPos().copy().add(1, 1)));
        addValue(new ExpressionValue<>("ul", o -> o.b.getPos().copy().add(-1, 1)));
        addValue(new ExpressionValue<>("dr", o -> o.b.getPos().copy().add(1, -1)));
        addValue(new ExpressionValue<>("dl", o -> o.b.getPos().copy().add(-1, -1)));
        addValue(new ExpressionValue<>("u", o -> o.b.getPos().copy().add(0, 1)));
        addValue(new ExpressionValue<>("d", o -> o.b.getPos().copy().add(0, -1)));
        addValue(new ExpressionValue<>("l", o -> o.b.getPos().copy().add(-1, 0)));
        addValue(new ExpressionValue<>("r", o -> o.b.getPos().copy().add(1, 0)));
        addValue(new ExpressionValue<>("this", o -> o.b));

        //Get the block at the specified position on the specified layer. Returns block name
        addFunction(new ExpressionFunction<>("block", args -> {
            if (args.size() == 2)
                return o -> {
                    ObjPos pos = ((ObjPos) args.get(0).apply(o));
                    return o.l.getBlock(ObjectLayer.getObjectLayer((String) args.get(1).apply(o)), (int) pos.x, (int) pos.y);
                };
            if (args.size() == 1)
                return o -> {
                    ObjPos pos = ((ObjPos) args.get(0).apply(o));
                    return o.l.getBlock(o.b.getLayer(), (int) pos.x, (int) pos.y);
                };
            throw new IllegalArgumentException("Incorrectly formatted expression, the \"block\" function requires two arguments");
        }));
        //Does any block exist at the specified position, on any layer, or on specific layers if specified? Returns boolean
        addFunction(new ExpressionFunction<>("existsBlock", args -> {
            if (args.size() == 1)
                return o -> {
                    ObjPos pos = ((ObjPos) args.get(0).apply(o));
                    for (ObjectLayer layer : ObjectLayer.values()) {
                        if (o.l.getBlock(layer, (int) pos.x, (int) pos.y) != null)
                            return true;
                    }
                    return false;
                };
            else return o -> {
                ObjPos pos = ((ObjPos) args.get(0).apply(o));
                HashSet<ObjectLayer> layers = new HashSet<>();
                for (int i = 1; i < args.size(); i++) {
                    layers.add(ObjectLayer.getObjectLayer((String) args.get(i).apply(o)));
                }
                for (ObjectLayer layer : layers) {
                    if (!layer.addToStatic)
                        throw new IllegalArgumentException("Incorrectly formatted expression, function \"existsBlock\" was provided a non-static ObjectLayer as an argument");
                    if (o.l.getBlock(layer, (int) pos.x, (int) pos.y) != null)
                        return true;
                }
                return false;
            };
        }));
        //Same as existsBlock, but ignores non-collision blocks
        addFunction(new ExpressionFunction<>("existsBlockWithCollision", args -> {
            if (args.size() == 1)
                return o -> {
                    ObjPos pos = ((ObjPos) args.get(0).apply(o));
                    for (ObjectLayer layer : ObjectLayer.values()) {
                        BlockLike block = o.l.getBlock(layer, (int) pos.x, (int) pos.y);
                        if (block != null && block.hasCollision())
                            return true;
                    }
                    return false;
                };
            else return o -> {
                ObjPos pos = ((ObjPos) args.get(0).apply(o));
                HashSet<ObjectLayer> layers = new HashSet<>();
                for (int i = 1; i < args.size(); i++) {
                    layers.add(ObjectLayer.getObjectLayer((String) args.get(i).apply(o)));
                }
                for (ObjectLayer layer : layers) {
                    if (!layer.addToStatic)
                        throw new IllegalArgumentException("Incorrectly formatted expression, function \"existsBlockWithCollision\" was provided a non-static ObjectLayer as an argument");
                    BlockLike block = o.l.getBlock(layer, (int) pos.x, (int) pos.y);
                    if (block != null && block.hasCollision())
                        return true;
                }
                return false;
            };
        }));
    }
}
