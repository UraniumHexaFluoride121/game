package loader;

//Utility class to store a type parameter, representing the data type being read from json
public class JsonType<T> {
    //Number type is used to know which number type we need to convert to
    public final NumberType numberType;
    public static final JsonType<Boolean> BOOLEAN_JSON_TYPE = new JsonType<>(NumberType.NONE);
    public static final JsonType<Integer> INTEGER_JSON_TYPE = new JsonType<>(NumberType.INTEGER);
    public static final JsonType<Float> FLOAT_JSON_TYPE = new JsonType<>(NumberType.FLOAT);
    public static final JsonType<String> STRING_JSON_TYPE = new JsonType<>(NumberType.NONE);
    public static final JsonType<JsonObject> JSON_OBJECT_TYPE = new JsonType<>(NumberType.NONE);
    public static final JsonType<JsonArray> JSON_ARRAY_TYPE = new JsonType<>(NumberType.NONE);

    public JsonType(NumberType numberType) {
        this.numberType = numberType;
    }

    public enum NumberType {
        NONE, INTEGER, FLOAT
    }
}
