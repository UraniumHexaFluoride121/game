package foundation.expression;

import foundation.Main;
import foundation.math.ObjPos;

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

    public Function<T, ?> parseExpression(String expression) {
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
        Function<T, ?> function = evaluateExpression(tokensToExpression(false, finalTokens)).f;
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

    private ExpressionObject<T, ?> evaluateExpression(ParserExpression exp) {
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
                        newTokens.add(EvaluateOperation.evaluateOperation(op, parsedTokens.get(i)));
                    } else {
                        Object lastToken = newTokens.get(newTokens.size() - 1);
                        newTokens.remove(newTokens.size() - 1);
                        newTokens.add(EvaluateOperation.evaluateOperation(op, lastToken, parsedTokens.get(i)));
                    }
                } else
                    newTokens.add(token);
            }
            parsedTokens = newTokens;
        }
        if (parsedTokens.size() > 1)
            throw new RuntimeException("Unable to parse expression: \n" + exp + "\n" + parsedTokens);
        return ((ExpressionObject<T, ?>) parsedTokens.get(0));
    }

    private ExpressionObject<T, ?> parseExpressionValue(ParserToken<?> value) {
        String s = ((String) value.value);
        try {
            int num = Integer.parseInt(s);
            return new ExpressionObjectStatic<>(Number.class, o -> Integer.parseInt(s), num);
        } catch (NumberFormatException ignored) {

        }
        if (values.containsKey(s))
            return values.get(s).value;
        return switch (s) {
            case "true" -> new ExpressionObjectStatic<>(Boolean.class, o -> true, true);
            case "false" -> new ExpressionObjectStatic<>(Boolean.class, o -> false, false);
            case "null" -> new ExpressionObjectStatic<>(null, o -> null, null);
            default -> new ExpressionObjectStatic<>(String.class, o -> s, s);
        };
    }

    private ExpressionObject<T, ?> parseFunction(String s, ParserExpression argExpression) {
        ArrayList<ExpressionObject<T, ?>> args = new ArrayList<>();
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
            case "pos" -> {
                if (!args.get(0).returnType.equals(Number.class))
                    argExceptionMessage(0, args, Number.class);
                if (!args.get(1).returnType.equals(Number.class))
                    argExceptionMessage(1, args, Number.class);
                Function<T, ?> f0 = args.get(0).f, f1 = args.get(1).f;
                if (args.get(0) instanceof ExpressionObjectStatic<T, ?> static0) {
                    float num0 = ((Number) static0.value).floatValue();
                    if (args.get(1) instanceof ExpressionObjectStatic<T, ?> static1) {
                        float num1 = ((Number) static1.value).floatValue();
                        ObjPos pos = new ObjPos(num0, num1);
                        yield new ExpressionObjectStatic<>(ObjPos.class, o -> pos, pos);
                    }
                    yield new ExpressionObject<>(ObjPos.class, o -> new ObjPos(num0, ((Number) f1.apply(o)).floatValue()));
                } else {
                    if (args.get(1) instanceof ExpressionObjectStatic<T, ?> static1) {
                        float num1 = ((Number) static1.value).floatValue();
                        yield new ExpressionObject<>(ObjPos.class, o -> new ObjPos(((Number) f0.apply(o)).floatValue(), num1));
                    }
                    yield new ExpressionObject<>(ObjPos.class, o -> new ObjPos(((Number) f0.apply(o)).floatValue(), ((Number) f1.apply(o)).floatValue()));
                }
            }
            case "distanceToBorder" -> {
                if (!args.get(0).returnType.equals(ObjPos.class) && !args.get(0).returnType.equals(Number.class))
                    argExceptionMessage(0, args, ObjPos.class);
                if (args.get(0) instanceof ExpressionObjectStatic<T, ?> static0) {
                    float x;
                    if (args.get(0).returnType.equals(ObjPos.class)) {
                        x = ((ObjPos) static0.value).x;
                    } else {
                        x = ((Number) static0.value).floatValue();
                    }
                    float dist = Math.min(x, Main.BLOCKS_X - 1 - x);
                    yield new ExpressionObjectStatic<>(Number.class, o -> dist, dist);
                } else {
                    Function<T, ?> f = args.get(0).f;
                    if (args.get(0).returnType.equals(ObjPos.class)) {
                        yield new ExpressionObject<>(Number.class, o -> {
                            float x = ((ObjPos) f.apply(o)).x;
                            return Math.min(x, Main.BLOCKS_X - 1 - x);
                        });
                    } else {
                        yield new ExpressionObject<>(Number.class, o -> {
                            float x = ((Number) f.apply(o)).floatValue();
                            return Math.min(x, Main.BLOCKS_X - 1 - x);
                        });
                    }
                }
            }
            default ->
                    throw new RuntimeException("Incorrectly formatted expression, unrecognised function name \"" + s + "\"");
        };
    }

    protected <U> U getArg(int index, ArrayList<ExpressionObject<T, ?>> args, T t, Class<U> clazz) {
        if (!clazz.isAssignableFrom(args.get(index).returnType)) {
            Object v = args.get(index).f.apply(t);
            ArrayList<Object> retrievedArgs = new ArrayList<>();
            ArrayList<String> retrievedArgsClass = new ArrayList<>();
            args.forEach(a -> retrievedArgs.add(a.f.apply(t)));
            args.forEach(a -> retrievedArgsClass.add(a.f.apply(t).getClass().getSimpleName()));
            throw new RuntimeException("Argument number " + (index + 1) + " of type " + v.getClass().getSimpleName() + " was of the wrong type in a function for " +
                    this.getClass().getSimpleName() + ". Intended type was: " + clazz.getSimpleName() + ". The values for the arguments provided for this function were: " + retrievedArgs +
                    " of types: " + retrievedArgsClass);
        }
        return (U) args.get(index).f.apply(t);
    }

    protected <U> String argExceptionMessage(int index, ArrayList<ExpressionObject<T, ?>> args, Class<U> clazz) {
        ArrayList<String> retrievedArgsClass = new ArrayList<>();
        args.forEach(a -> retrievedArgsClass.add(a.returnType.getSimpleName()));
        return "Argument number " + (index + 1) + " of type " + args.get(index).getClass().getSimpleName() + " was of the wrong type in a function for " +
                this.getClass().getSimpleName() + ". Intended type was: " + clazz.getSimpleName() + ". All the types of the arguments provided for this function were: " +
                retrievedArgsClass;
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
                OperatorType.GTH,
                OperatorType.GTE,
                OperatorType.LTH,
                OperatorType.LTE
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.AND
        })));
        operatorOrder.add(new HashSet<>(List.of(new OperatorType[]{
                OperatorType.OR
        })));
    }

    public enum OperatorType {
        DOT(o -> false, "."),
        MULTIPLICATION(o -> false, "*"),
        DIVISION(o -> false, "/"),
        PLUS(o -> o instanceof ParserToken<?>, "+"),
        MINUS(o -> o instanceof ParserToken<?>, "-"),
        NOT(o -> true, "not", "!"),
        NOT_EQUAL(o -> false, "!="),
        EQUAL(o -> false, "=", "=="),
        GTH(o -> false, ">"),
        GTE(o -> false, ">="),
        LTH(o -> false, "<"),
        LTE(o -> false, "<="),
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
