package loader;

//Utility class to store a type parameter, representing the data type being read from json
public class JsonType<T> {
    //Number type is used to know which number type we need to convert to
    public final NumberType numberType;
    public final Class<T> clazz;
    public final String debugName;
    public static final JsonType<Boolean> BOOLEAN_JSON_TYPE = new JsonType<>(NumberType.NONE, Boolean.class, "Boolean");
    public static final JsonType<Integer> INTEGER_JSON_TYPE = new JsonType<>(NumberType.INTEGER, Integer.class, "Integer");
    public static final JsonType<Float> FLOAT_JSON_TYPE = new JsonType<>(NumberType.FLOAT, Float.class, "Float");
    public static final JsonType<String> STRING_JSON_TYPE = new JsonType<>(NumberType.NONE, String.class, "String");
    public static final JsonType<JsonObject> JSON_OBJECT_TYPE = new JsonType<>(NumberType.NONE, JsonObject.class, "JsonObject");
    public static final JsonType<JsonArray> JSON_ARRAY_TYPE = new JsonType<>(NumberType.NONE, JsonArray.class, "JsonArray");

    public JsonType(NumberType numberType, Class<T> clazz, String debugName) {
        this.numberType = numberType;
        this.clazz = clazz;
        this.debugName = debugName;
    }

    public enum NumberType {
        NONE, INTEGER, FLOAT
    }
}
