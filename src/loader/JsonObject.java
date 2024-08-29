package loader;

import java.util.HashMap;

public final class JsonObject extends JsonDataStructure {
    public HashMap<String, Object> pairs = new HashMap<>();

    public JsonObject() {
        super(false);
    }

    public <T> T get(String name, JsonType<T> type) {
        if (!pairs.containsKey(name))
            throw new RuntimeException("Incorrectly formatted Json, missing expected tag in object with name " + name);
        return (T) pairs.get(name);
    }

    public <T> T getOrDefault(String name, T defaultValue, JsonType<T> type) {
        if (!pairs.containsKey(name))
            return defaultValue;
        return (T) pairs.get(name);
    }

    public boolean containsName(String name) {
        return pairs.containsKey(name);
    }

    @Override
    public String toString() {
        return pairs.toString();
    }
}
