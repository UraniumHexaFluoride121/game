package loader;

public sealed class JsonDataStructure permits JsonArray, JsonObject {
    public boolean isList;

    public JsonDataStructure(boolean isList) {
        this.isList = isList;
    }

}
