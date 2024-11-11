package foundation.expression;

import foundation.math.ObjPos;
import level.objects.BlockLike;
import level.procedural.marker.LayoutMarker;

import java.util.function.Function;

public abstract class EvaluateOperation {
    static <T> ExpressionObject<T, ?> evaluateOperation(Expression.OperatorType op, Object... objArgs) {
        ExpressionObject<T, ?>[] args = new ExpressionObject[objArgs.length];
        for (int i = 0; i < objArgs.length; i++) {
            args[i] = ((ExpressionObject<T, ?>) objArgs[i]);
        }
        final boolean bothNumbers = args[0].returnType.equals(Number.class) && args[1].returnType.equals(Number.class);
        final boolean bothBooleans = args[0].returnType.equals(Boolean.class) && args[1].returnType.equals(Boolean.class);
        return switch (op) {
            case DOT -> {
                if (args[0].returnType.equals(Number.class) && args[0] instanceof ExpressionObjectStatic<T, ?> static0 &&
                        args[1].returnType.equals(Number.class) && args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                    float num = Float.parseFloat(((Number) static0.value).intValue() + "." + ((Number) static1.value).intValue());
                    yield new ExpressionObjectStatic<>(Number.class, o -> num, num);
                }
                if (args[1].returnType.equals(String.class) && args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                    String field = (String) static1.value;
                    if (args[0].returnType.equals(BlockLike.class)) {
                        final Function<T, ?> f = args[0].f;
                        yield switch (field) {
                            case "name" -> new ExpressionObject<>(String.class, o -> {
                                BlockLike b = ((BlockLike) f.apply(o));
                                return b == null ? null : b.name;
                            });
                            case "layer" -> new ExpressionObject<>(String.class, o -> {
                                BlockLike b = ((BlockLike) f.apply(o));
                                return b == null ? null : b.getLayer().s;
                            });
                            case "pos" -> new ExpressionObject<>(ObjPos.class, o -> {
                                BlockLike b = ((BlockLike) f.apply(o));
                                return b == null ? null : b.pos.copy();
                            });
                            case "hasCollision" -> new ExpressionObject<>(Boolean.class, o -> {
                                BlockLike b = ((BlockLike) f.apply(o));
                                return b == null ? null : b.hasCollision();
                            });
                            default ->
                                    throw new RuntimeException("Incorrectly formatted expression, tried to access non-existent field \"" + field + "\" from BlockLike");
                        };
                    }
                    if (args[0].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            ObjPos pos = (ObjPos) static0.value;
                            yield switch (field) {
                                case "x" -> new ExpressionObjectStatic<>(Number.class, o -> pos.x, pos.x);
                                case "y" -> new ExpressionObjectStatic<>(Number.class, o -> pos.y, pos.y);
                                default ->
                                        throw new RuntimeException("Incorrectly formatted expression, tried to access non-existent field \"" + field + "\" from BlockLike");
                            };
                        }
                        final Function<T, ?> f = args[0].f;
                        yield switch (field) {
                            case "x" -> new ExpressionObject<>(Number.class, o -> ((ObjPos) f.apply(o)).x);
                            case "y" -> new ExpressionObject<>(Number.class, o -> ((ObjPos) f.apply(o)).y);
                            default ->
                                    throw new RuntimeException("Incorrectly formatted expression, tried to access non-existent field \"" + field + "\" from BlockLike");
                        };
                    }
                    if (args[0].returnType.equals(LayoutMarker.class)) {
                        final Function<T, ?> f = args[0].f;
                        yield switch (field) {
                            case "pos" ->
                                    new ExpressionObject<>(ObjPos.class, o -> ((LayoutMarker) f.apply(o)).pos.copy());
                            default ->
                                    throw new RuntimeException("Incorrectly formatted expression, tried to access non-existent field \"" + field + "\" from LayoutMarker");
                        };
                    }
                }
                throw new RuntimeException("Expression error: tried to access a field from an object that doesn't have fields: " + args[0].returnType.getSimpleName());
            }

            case MULTIPLICATION -> {
                final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                if (args[0].returnType.equals(Number.class)) {
                    if (args[1].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                float num = ((Number) static0.value).floatValue() * ((Number) static1.value).floatValue();
                                yield new ExpressionObjectStatic<>(Number.class, o -> num, num);
                            } else {
                                float num0 = ((Number) static0.value).floatValue();
                                yield new ExpressionObject<>(Number.class, o -> num0 * ((Number) f1.apply(o)).floatValue());
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            float num1 = ((Number) static1.value).floatValue();
                            yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() * num1);
                        }
                        yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() * ((Number) f1.apply(o)).floatValue());
                    }
                    if (args[1].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static1.value).multiply(((Number) static0.value).floatValue());
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                float num0 = ((Number) static0.value).floatValue();
                                yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).multiply(num0));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            ObjPos pos1 = ((ObjPos) static1.value);
                            yield new ExpressionObject<>(ObjPos.class, o -> pos1.multiply(((Number) f0.apply(o)).floatValue()));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).multiply(((Number) f0.apply(o)).floatValue()));
                    }
                }
                if (args[0].returnType.equals(ObjPos.class)) {
                    if (args[1].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static0.value).multiply(((Number) static1.value).floatValue());
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                ObjPos pos0 = ((ObjPos) static0.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos0.multiply(((Number) f1.apply(o)).floatValue()));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            float num1 = ((Number) static1.value).floatValue();
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).multiply(num1));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).multiply(((Number) f1.apply(o)).floatValue()));
                    }
                    if (args[1].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static0.value).multiply(((ObjPos) static1.value));
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                ObjPos pos0 = ((ObjPos) static0.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos0.multiply(((ObjPos) f1.apply(o))));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            ObjPos pos1 = ((ObjPos) static1.value);
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).multiply(pos1));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).multiply(((ObjPos) f1.apply(o))));
                    }
                }
                throw new RuntimeException("Tried to apply MULTIPLICATION operator on incompatible objects, \"" + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName() + "\"");
            }
            case DIVISION -> {
                final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                if (args[0].returnType.equals(Number.class)) {
                    if (args[1].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                float num = ((Number) static0.value).floatValue() / ((Number) static1.value).floatValue();
                                yield new ExpressionObjectStatic<>(Number.class, o -> num, num);
                            } else {
                                float num0 = ((Number) static0.value).floatValue();
                                yield new ExpressionObject<>(Number.class, o -> num0 / ((Number) f1.apply(o)).floatValue());
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            float num1 = ((Number) static1.value).floatValue();
                            yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() / num1);
                        }
                        yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() / ((Number) f1.apply(o)).floatValue());
                    }
                    if (args[1].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static1.value).divide(((Number) static0.value).floatValue());
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                float num0 = ((Number) static0.value).floatValue();
                                yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).divide(num0));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            ObjPos pos1 = ((ObjPos) static1.value);
                            yield new ExpressionObject<>(ObjPos.class, o -> pos1.divide(((Number) f0.apply(o)).floatValue()));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).divide(((Number) f0.apply(o)).floatValue()));
                    }
                }
                if (args[0].returnType.equals(ObjPos.class)) {
                    if (args[1].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static0.value).divide(((Number) static1.value).floatValue());
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                ObjPos pos0 = ((ObjPos) static0.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos0.divide(((Number) f1.apply(o)).floatValue()));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            float num1 = ((Number) static1.value).floatValue();
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).divide(num1));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).divide(((Number) f1.apply(o)).floatValue()));
                    }
                    if (args[1].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static0.value).divide(((ObjPos) static1.value));
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                ObjPos pos0 = ((ObjPos) static0.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos0.divide(((ObjPos) f1.apply(o))));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            ObjPos pos1 = ((ObjPos) static1.value);
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).divide(pos1));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).divide(((ObjPos) f1.apply(o))));
                    }
                }
                throw new RuntimeException("Tried to apply DIVISION operator on incompatible objects, \"" + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName() + "\"");
            }
            case PLUS -> {
                final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                if (args[0].returnType.equals(Number.class)) {
                    if (args[1].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                float num = ((Number) static0.value).floatValue() + ((Number) static1.value).floatValue();
                                yield new ExpressionObjectStatic<>(Number.class, o -> num, num);
                            } else {
                                float num0 = ((Number) static0.value).floatValue();
                                yield new ExpressionObject<>(Number.class, o -> num0 + ((Number) f1.apply(o)).floatValue());
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            float num1 = ((Number) static1.value).floatValue();
                            yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() + num1);
                        }
                        yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() + ((Number) f1.apply(o)).floatValue());
                    }
                    if (args[1].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static1.value).add(((Number) static0.value).floatValue());
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                float num0 = ((Number) static0.value).floatValue();
                                yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).add(num0));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            ObjPos pos1 = ((ObjPos) static1.value);
                            yield new ExpressionObject<>(ObjPos.class, o -> pos1.add(((Number) f0.apply(o)).floatValue()));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).add(((Number) f0.apply(o)).floatValue()));
                    }
                }
                if (args[0].returnType.equals(ObjPos.class)) {
                    if (args[1].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static0.value).add(((Number) static1.value).floatValue());
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                ObjPos pos0 = ((ObjPos) static0.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos0.add(((Number) f1.apply(o)).floatValue()));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            float num1 = ((Number) static1.value).floatValue();
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).add(num1));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).add(((Number) f1.apply(o)).floatValue()));
                    }
                    if (args[1].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos = ((ObjPos) static0.value).add(((ObjPos) static1.value));
                                yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                            } else {
                                ObjPos pos0 = ((ObjPos) static0.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos0.add(((ObjPos) f1.apply(o))));
                            }
                        }
                        if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                            ObjPos pos1 = ((ObjPos) static1.value);
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).add(pos1));
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).add(((ObjPos) f1.apply(o))));
                    }
                }
                throw new RuntimeException("Tried to apply ADD operator on incompatible objects, \"" + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName() + "\"");
            }
            case MINUS -> {
                final Function<T, ?> f0 = args[0].f;
                if (args.length == 2) {
                    final Function<T, ?> f1 = args[1].f;
                    if (args[0].returnType.equals(Number.class)) {
                        if (args[1].returnType.equals(Number.class)) {
                            if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                                if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                    float num = ((Number) static0.value).floatValue() - ((Number) static1.value).floatValue();
                                    yield new ExpressionObjectStatic<>(Number.class, o -> num, num);
                                } else {
                                    float num0 = ((Number) static0.value).floatValue();
                                    yield new ExpressionObject<>(Number.class, o -> num0 - ((Number) f1.apply(o)).floatValue());
                                }
                            }
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                float num1 = ((Number) static1.value).floatValue();
                                yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() - num1);
                            }
                            yield new ExpressionObject<>(Number.class, o -> ((Number) f0.apply(o)).floatValue() - ((Number) f1.apply(o)).floatValue());
                        }
                        if (args[1].returnType.equals(ObjPos.class)) {
                            if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                                if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                    ObjPos pos = ((ObjPos) static1.value).subtract(((Number) static0.value).floatValue());
                                    yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                                } else {
                                    float num0 = ((Number) static0.value).floatValue();
                                    yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).subtract(num0));
                                }
                            }
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos1 = ((ObjPos) static1.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> pos1.subtract(((Number) f0.apply(o)).floatValue()));
                            }
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f1.apply(o)).subtract(((Number) f0.apply(o)).floatValue()));
                        }
                    }
                    if (args[0].returnType.equals(ObjPos.class)) {
                        if (args[1].returnType.equals(Number.class)) {
                            if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                                if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                    ObjPos pos = ((ObjPos) static0.value).subtract(((Number) static1.value).floatValue());
                                    yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                                } else {
                                    ObjPos pos0 = ((ObjPos) static0.value);
                                    yield new ExpressionObject<>(ObjPos.class, o -> pos0.subtract(((Number) f1.apply(o)).floatValue()));
                                }
                            }
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                float num1 = ((Number) static1.value).floatValue();
                                yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).subtract(num1));
                            }
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).subtract(((Number) f1.apply(o)).floatValue()));
                        }
                        if (args[1].returnType.equals(ObjPos.class)) {
                            if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                                if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                    ObjPos pos = ((ObjPos) static0.value).subtract(((ObjPos) static1.value));
                                    yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                                } else {
                                    ObjPos pos0 = ((ObjPos) static0.value);
                                    yield new ExpressionObject<>(ObjPos.class, o -> pos0.subtract(((ObjPos) f1.apply(o))));
                                }
                            }
                            if (args[1] instanceof ExpressionObjectStatic<T, ?> static1) {
                                ObjPos pos1 = ((ObjPos) static1.value);
                                yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).subtract(pos1));
                            }
                            yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).subtract(((ObjPos) f1.apply(o))));
                        }
                    }
                    throw new RuntimeException("Tried to apply MINUS operator on incompatible objects, \"" + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName() + "\"");
                } else {
                    if (args[0].returnType.equals(Number.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            float num = -((Number) static0.value).floatValue();
                            yield new ExpressionObjectStatic<>(Number.class, o -> num, num);
                        }
                        yield new ExpressionObject<>(Number.class, o -> -((Number) f0.apply(o)).floatValue());
                    }
                    if (args[0].returnType.equals(ObjPos.class)) {
                        if (args[0] instanceof ExpressionObjectStatic<T, ?> static0) {
                            ObjPos pos = ((ObjPos) static0.value).multiply(-1);
                            yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                        }
                        yield new ExpressionObject<>(ObjPos.class, o -> ((ObjPos) f0.apply(o)).multiply(-1));
                    }
                    throw new RuntimeException("Tried to apply MINUS operator on incompatible object, \"" + args[0].returnType.getSimpleName());
                }
            }
            case GTH -> {
                if (bothNumbers) {
                    final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                    yield new ExpressionObject<>(Boolean.class, o -> ((Number) f0.apply(o)).floatValue() > ((Number) f1.apply(o)).floatValue());
                }
                throw new RuntimeException("Tried to apply GREATER THAN operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName());
            }
            case GTE -> {
                if (bothNumbers) {
                    final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                    yield new ExpressionObject<>(Boolean.class, o -> ((Number) f0.apply(o)).floatValue() >= ((Number) f1.apply(o)).floatValue());
                }
                throw new RuntimeException("Tried to apply GREATER THAN OR EQUAL operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName());
            }
            case LTH -> {
                if (bothNumbers) {
                    final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                    yield new ExpressionObject<>(Boolean.class, o -> ((Number) f0.apply(o)).floatValue() < ((Number) f1.apply(o)).floatValue());
                }
                throw new RuntimeException("Tried to apply LESS THAN operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName());
            }
            case LTE -> {
                if (bothNumbers) {
                    final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                    yield new ExpressionObject<>(Boolean.class, o -> ((Number) f0.apply(o)).floatValue() <= ((Number) f1.apply(o)).floatValue());
                }
                throw new RuntimeException("Tried to apply LESS THAN OR EQUAL operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName());
            }

            case NOT -> {
                if (args[0].returnType.equals(Boolean.class)) {
                    final Function<T, ?> f0 = args[0].f;
                    yield new ExpressionObject<>(Boolean.class, o -> !((Boolean) f0.apply(o)));
                }
                throw new RuntimeException("Tried to apply NOT operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName());
            }
            case NOT_EQUAL -> {
                final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                yield new ExpressionObject<>(Boolean.class, o -> !f0.apply(o).equals(f1.apply(o)));
            }
            case EQUAL -> {
                final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                yield new ExpressionObject<>(Boolean.class, o -> f0.apply(o).equals(f1.apply(o)));
            }
            case AND -> {
                if (bothBooleans) {
                    final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                    yield new ExpressionObject<>(Boolean.class, o -> ((Boolean) f0.apply(o)) && ((Boolean) f1.apply(o)));
                }
                throw new RuntimeException("Tried to apply AND operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName());
            }
            case OR -> {
                if (bothBooleans) {
                    final Function<T, ?> f0 = args[0].f, f1 = args[1].f;
                    yield new ExpressionObject<>(Boolean.class, o -> ((Boolean) f0.apply(o)) || ((Boolean) f1.apply(o)));
                }
                throw new RuntimeException("Tried to apply OR operator on incompatible objects\nArguments were: " + args[0].returnType.getSimpleName() + "\" and \"" + args[1].returnType.getSimpleName());
            }
        };
    }
}