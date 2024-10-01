package foundation.expression;

import foundation.ObjPos;
import level.objects.BlockLike;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class Expression<T> {
    public HashMap<String, ExpressionValue<T>> values = new HashMap<>();
    public HashMap<String, ExpressionFunction<T>> functions = new HashMap<>();

    protected Expression() {

    }

    public void addValue(ExpressionValue<T> v) {
        values.put(v.name, v);
    }

    public void addFunction(ExpressionFunction<T> v) {
        functions.put(v.name, v);
    }

    public Function<T, Object> parseExpression(String expression) {
        ArrayList<ParserToken<?>> unidentifiedTokens = getUnidentifiedTokens(expression);
        ArrayList<ParserToken<?>> finalTokens = new ArrayList<>();

        for (int j = 0; j < unidentifiedTokens.size(); j++) {
            ParserToken<?> token = unidentifiedTokens.get(j);
            boolean added = false;

            //Name tokens need to be interpreted
            if (token.type == ParserTokenType.NAME) {
                String name = ((String) token.value);

                //Check if the token is an operator
                for (OperatorType op : OperatorType.values()) {
                    for (String validStringToken : op.validStringTokens) {
                        if (name.equals(validStringToken)) {
                            finalTokens.add(new ParserToken<>(ParserTokenType.OPERATOR, op));
                            added = true;
                        }
                    }
                }
                if (added)
                    continue;

                //Check if the token is a function
                if (j != unidentifiedTokens.size() - 1 &&
                        unidentifiedTokens.get(j + 1).type == ParserTokenType.PARENTHESIS &&
                        ((Character) unidentifiedTokens.get(j + 1).value) == '[') {
                    finalTokens.add(new ParserToken<>(ParserTokenType.FUNCTION, ((String) token.value)));
                    continue;
                }

                //If all fail, we assume it must be a value
                finalTokens.add(new ParserToken<>(ParserTokenType.VALUE, ((String) token.value)));
                continue;
            }
            finalTokens.add(token);
        }
        Function<T, Object> function = evaluateExpression(tokensToExpression(false, finalTokens));
        return function;
    }

    private static ArrayList<ParserToken<?>> getUnidentifiedTokens(String condition) {
        char[] chars = condition.toCharArray();
        int i = 0;

        ArrayList<ParserToken<?>> unnamedTokens = new ArrayList<>();

        while (i < chars.length) {
            while (i < chars.length && chars[i] == ' ') i++;
            boolean continueToken = true;
            StringBuilder token = new StringBuilder();
            while (true) {
                if (!continueToken || i >= chars.length) {
                    if (continueToken && !token.isEmpty())
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                    break;
                }
                switch (chars[i]) {
                    case ',' -> {
                        if (!token.isEmpty())
                            unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.ARGUMENT_SEPARATOR, ','));
                        continueToken = false;
                    }
                    case '(' -> {
                        if (!token.isEmpty())
                            unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.PARENTHESIS, '('));
                        continueToken = false;
                    }
                    case ')' -> {
                        if (!token.isEmpty())
                            unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.PARENTHESIS, ')'));
                        continueToken = false;
                    }
                    case '[' -> {
                        if (!token.isEmpty())
                            unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.PARENTHESIS, '['));
                        continueToken = false;
                    }
                    case ']' -> {
                        if (!token.isEmpty())
                            unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.PARENTHESIS, ']'));
                        continueToken = false;
                    }
                    case ' ' -> {
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        continueToken = false;
                    }
                    case '.' -> {
                        if (!token.isEmpty())
                            unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                        unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, "."));
                        continueToken = false;
                    }
                    default -> {
                        if (!token.isEmpty()) {
                            boolean isInt = false;
                            try {
                                Integer.parseInt(token.toString());
                                isInt = true;
                            } catch (NumberFormatException ignored) {

                            }
                            if (isInt && chars[i] == '-') {
                                unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                                unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, "-"));
                                continueToken = false;
                                break;
                            } else if (nameChar(chars[i - 1]) != nameChar(chars[i])) {
                                unnamedTokens.add(new ParserToken<>(ParserTokenType.NAME, token.toString()));
                                token = new StringBuilder();
                            }
                        }
                        token.append(chars[i]);
                    }
                }
                i++;
            }
        }
        return unnamedTokens;
    }

    private static ParserExpression tokensToExpression(boolean isFunction, ArrayList<ParserToken<?>> tokens) {
        ParserExpression exp = new ParserExpression(isFunction);
        ArrayDeque<Character> parentheses = new ArrayDeque<>();
        ArrayList<ParserToken<?>> innerExpressionTokens = new ArrayList<>();
        for (ParserToken<?> token : tokens) {
            if (token.type == ParserTokenType.PARENTHESIS) {
                char c = ((Character) token.value);
                switch (c) {
                    case '[', '(' -> {
                        parentheses.add(c);
                        //Make sure we remove the parenthesis that starts the inner expression
                        if (parentheses.size() == 1)
                            continue;
                    }
                    case ']', ')' -> {
                        //Get the corresponding opening parenthesis to the closing one, assuming correct formatting
                        Character opening = parentheses.pollLast();
                        if (opening == null)
                            throw new RuntimeException("Incorrectly formatted expression, found a closing parenthesis \"" +
                                    c + "\" with no corresponding opening parenthesis");
                        if ((opening == '[' && c == ']') || (opening == '(' && c == ')')) {
                            if (parentheses.isEmpty()) {
                                exp.addToken(tokensToExpression(c == ']', innerExpressionTokens));
                                innerExpressionTokens.clear();
                                //remove the closing parenthesis that ends the inner expression
                                continue;
                            }
                        } else
                            throw new RuntimeException("Incorrectly formatted expression, found a closing parenthesis \"" +
                                    c + "\" with the wrong corresponding opening parenthesis: \"" + opening + "\"");
                    }
                }
            }
            if (parentheses.isEmpty())
                exp.addToken(token);
            else
                innerExpressionTokens.add(token);
        }
        if (!parentheses.isEmpty())
            throw new RuntimeException("Incorrectly formatted expression, did not include all closing parentheses");
        return exp;
    }

    private Function<T, Object> evaluateExpression(ParserExpression exp) {
        ArrayList<Object> parsedTokens = new ArrayList<>();
        for (int i = 0; i < exp.tokens.size(); i++) {
            Object token = exp.tokens.get(i);
            if (token instanceof ParserToken<?> t) {
                if (t.type == ParserTokenType.VALUE) {
                    parsedTokens.add(parseExpressionValue(t));
                } else if (t.type == ParserTokenType.FUNCTION) {
                    parsedTokens.add(parseFunction(((String) t.value), ((ParserExpression) exp.tokens.get(i + 1))));
                    i++;
                } else
                    parsedTokens.add(t);
            } else {
                parsedTokens.add(evaluateExpression((ParserExpression) token));
            }
        }
        for (HashSet<OperatorType> ops : operatorOrder) {
            ArrayList<Object> newTokens = new ArrayList<>();
            for (int i = 0; i < parsedTokens.size(); i++) {
                Object token = parsedTokens.get(i);
                if (token instanceof ParserToken<?> t && t.type == ParserTokenType.OPERATOR && t.value instanceof OperatorType op && ops.contains(op)) {
                    i++;
                    if (i < 2 || op.isUnary.test(parsedTokens.get(i - 2))) {
                        newTokens.add(evaluateOperation(op, parsedTokens.get(i)));
                    } else {
                        Object lastToken = newTokens.get(newTokens.size() - 1);
                        newTokens.remove(newTokens.size() - 1);
                        newTokens.add(evaluateOperation(op, lastToken, parsedTokens.get(i)));
                    }
                } else
                    newTokens.add(token);
            }
            parsedTokens = newTokens;
        }
        if (parsedTokens.size() > 1)
            throw new RuntimeException("Unable to parse expression: \n" + exp + "\n" + parsedTokens);
        return ((Function<T, Object>) parsedTokens.get(0));
    }

    private Function<T, Object> parseExpressionValue(ParserToken<?> value) {
        String s = ((String) value.value);
        try {
            Integer.parseInt(s);
            return o -> Integer.parseInt(s);
        } catch (NumberFormatException ignored) {

        }
        if (values.containsKey(s))
            return values.get(s).value;
        return switch (s) {
            case "true" -> o -> true;
            case "false" -> o -> false;
            case "null" -> o -> null;
            default -> o -> s;
        };
    }

    private Function<T, Object> evaluateOperation(OperatorType op, Object... args) {
        return switch (op) {
            case DOT -> (o) -> {
                Object obj = ((Function<T, Object>) args[0]).apply(o);
                if (obj == null)
                    return null;
                String field = (String) ((Function<T, Object>) args[1]).apply(o);
                if (obj instanceof BlockLike block) {
                    return switch (field) {
                        case "name" -> block.name;
                        case "layer" -> block.getLayer().s;
                        case "pos" -> block.getPos().copy();
                        case "hasCollision" -> block.hasCollision();
                        default ->
                                throw new RuntimeException("Incorrectly formatted expression, tried to access non-existent field \"" + field + "\" from BlockLike");
                    };
                }
                if (obj instanceof ObjPos pos) {
                    return switch (field) {
                        case "x" -> (int) pos.x;
                        case "y" -> (int) pos.y;
                        default ->
                                throw new RuntimeException("Incorrectly formatted expression, tried to access non-existent field \"" + field + "\" from BlockLike");
                    };
                }
                throw new RuntimeException("Expression error: tried to access a field from an object that doesn't have fields: " + obj);
            };

            case MULTIPLICATION -> o -> {
                Object arg1 = ((Function<T, Object>) args[0]).apply(o);
                Object arg2 = ((Function<T, Object>) args[1]).apply(o);
                if (arg1 instanceof Integer i1) {
                    if (arg2 instanceof Integer i2)
                        return i1 * i2;
                    if (arg2 instanceof ObjPos p2)
                        return p2.copy().multiply(i1);
                }
                if (arg1 instanceof ObjPos p1) {
                    if (arg2 instanceof Integer i2)
                        return p1.copy().multiply(i2);
                    if (arg2 instanceof ObjPos p2)
                        return p1.copy().multiply(p2);
                }
                throw new RuntimeException("Tried to apply MULTIPLICATION operator on incompatible objects, \"" + arg1 + "\" and \"" + arg2 + "\"");
            };
            case DIVISION -> o -> {
                Object arg1 = ((Function<T, Object>) args[0]).apply(o);
                Object arg2 = ((Function<T, Object>) args[1]).apply(o);
                if (arg1 instanceof Integer i1) {
                    if (arg2 instanceof Integer i2)
                        return i1 / i2;
                    if (arg2 instanceof ObjPos p2)
                        return p2.copy().divide(i1).toInt();
                }
                if (arg1 instanceof ObjPos p1) {
                    if (arg2 instanceof Integer i2)
                        return p1.copy().divide(i2).toInt();
                    if (arg2 instanceof ObjPos p2)
                        return p1.copy().divide(p2).toInt();
                }
                throw new RuntimeException("Tried to apply DIVISION operator on incompatible objects, \"" + arg1 + "\" and \"" + arg2 + "\"");
            };
            case PLUS -> {
                if (args.length == 2)
                    yield o -> {
                        Object arg1 = ((Function<T, Object>) args[0]).apply(o);
                        Object arg2 = ((Function<T, Object>) args[1]).apply(o);
                        if (arg1 instanceof Integer)
                            return ((int) arg1) + ((int) arg2);
                        if (arg1 instanceof ObjPos)
                            return ((ObjPos) arg1).copy().add((ObjPos) arg2);
                        throw new RuntimeException("Tried to apply ADD operator on incompatible objects, \"" + arg1 + "\" and \"" + arg2 + "\"");
                    };
                else
                    yield ((Function<T, Object>) args[0]);
            }
            case MINUS -> {
                if (args.length == 2)
                    yield o -> {
                        Object arg1 = ((Function<T, Object>) args[0]).apply(o);
                        Object arg2 = ((Function<T, Object>) args[1]).apply(o);
                        if (arg1 instanceof Integer)
                            return ((int) arg1) - ((int) arg2);
                        if (arg1 instanceof ObjPos)
                            return ((ObjPos) arg1).copy().subtract((ObjPos) arg2);
                        throw new RuntimeException("Tried to apply MINUS operator on incompatible objects, \"" + arg1 + "\" and \"" + arg2 + "\"");
                    };
                else
                    yield o -> {
                        Object arg = ((Function<T, Object>) args[0]).apply(o);
                        if (arg instanceof Integer)
                            return -((int) arg);
                        if (arg instanceof ObjPos)
                            return ((ObjPos) arg).copy().multiply(-1);
                        throw new RuntimeException("Tried to apply MINUS operator on incompatible object, \"" + arg + "\"");
                    };
            }

            case NOT -> o -> !((Boolean) ((Function<T, Object>) args[0]).apply(o));
            case NOT_EQUAL -> o -> !((Function<T, Object>) args[0]).apply(o).equals(
                    ((Function<T, Object>) args[1]).apply(o));
            case EQUAL -> o -> ((Function<T, Object>) args[0]).apply(o).equals(
                    ((Function<T, Object>) args[1]).apply(o));
            case AND -> o -> ((Boolean) ((Function<T, Object>) args[0]).apply(o)) &&
                    ((Boolean) ((Function<T, Object>) args[1]).apply(o));
            case OR -> o -> ((Boolean) ((Function<T, Object>) args[0]).apply(o)) ||
                    ((Boolean) ((Function<T, Object>) args[1]).apply(o));
        };
    }

    private Function<T, Object> parseFunction(String s, ParserExpression argExpression) {
        ArrayList<Function<T, Object>> args = new ArrayList<>();
        ArrayList<Object> tokens = new ArrayList<>();
        for (Object token : argExpression.tokens) {
            if (token instanceof ParserToken<?> t && t.type == ParserTokenType.ARGUMENT_SEPARATOR) {
                args.add(evaluateExpression(new ParserExpression(false).addTokens(tokens)));
                tokens.clear();
            } else {
                tokens.add(token);
            }
        }
        args.add(evaluateExpression(new ParserExpression(false).addTokens(tokens)));
        if (functions.containsKey(s))
            return functions.get(s).definition.apply(args);
        return switch (s) {
            case "pos" -> o -> new ObjPos((int) args.get(0).apply(o), (int) args.get(1).apply(o));
            default ->
                    throw new RuntimeException("Incorrectly formatted expression, unrecognised function name \"" + s + "\"");
        };
    }

    //Verify that a char is allowed to be used in names
    public static boolean nameChar(char c) {
        return Character.isAlphabetic(c) || c == '_' || c == '-' || Character.isDigit(c);
    }

    private static class ParserExpression {
        //Consists of tokens and other expressions
        public final ArrayList<Object> tokens = new ArrayList<>();
        public final boolean isFunction;

        private ParserExpression(boolean isFunction) {
            this.isFunction = isFunction;
        }

        public void addToken(Object token) {
            tokens.add(token);
        }

        public ParserExpression addTokens(ArrayList<Object> objects) {
            for (Object o : objects) {
                addToken(o);
            }
            return this;
        }

        @Override
        public String toString() {
            return "{tokens: " + tokens + "}";
        }
    }

    private record ParserToken<T>(ParserTokenType<T> type, T value) {
        @Override
        public String toString() {
            return "(" + type.s + ", \"" + value.toString() + "\")";
        }
    }

    private record ParserTokenType<T>(String s) {
        public static final ParserTokenType<Character> PARENTHESIS = new ParserTokenType<>("parenthesis");
        public static final ParserTokenType<OperatorType> OPERATOR = new ParserTokenType<>("operator");
        public static final ParserTokenType<String> NAME = new ParserTokenType<>("name");
        public static final ParserTokenType<String> FUNCTION = new ParserTokenType<>("function");
        public static final ParserTokenType<String> VALUE = new ParserTokenType<>("value");
        public static final ParserTokenType<Character> ARGUMENT_SEPARATOR = new ParserTokenType<>("separator");
    }

    private static ArrayList<HashSet<OperatorType>> operatorOrder = new ArrayList<>();

    static {
        /* Operators are processed in the order listed here. Operators in the same
        HashSet are processed with equal priority (from left to right) */
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.DOT
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.MULTIPLICATION,
                OperatorType.DIVISION
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.PLUS,
                OperatorType.MINUS
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.NOT
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.EQUAL,
                OperatorType.NOT_EQUAL
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.AND
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.OR
        })));
    }

    private enum OperatorType {
        DOT(o -> false, "."),
        MULTIPLICATION(o -> false, "*"),
        DIVISION(o -> false, "/"),
        PLUS(o -> o instanceof ParserToken<?>, "+"),
        MINUS(o -> o instanceof ParserToken<?>, "-"),
        NOT(o -> true, "not", "!"),
        NOT_EQUAL(o -> false, "!="),
        EQUAL(o -> false, "=", "=="),
        AND(o -> false, "and", "&", "&&"),
        OR(o -> false, "or", "|", "||");

        //Check if this operator should be treated as a unary operator. Some operators can function as both, and
        //we need to be able to differentiate between the two with this predicate. The object received in the predicate
        //is the token preceding the operator. The token after the operator is always one of the args, unary or not.
        public final Predicate<Object> isUnary;

        public final String[] validStringTokens;

        OperatorType(Predicate<Object> isUnary, String... s) {
            this.isUnary = isUnary;
            validStringTokens = s;
        }
    }
}
