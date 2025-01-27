package loader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class JsonObject extends JsonDataStructure {
    public HashMap<String, Object> pairs = new HashMap<>();

    public JsonObject() {
        super(false);
    }

    public <T> T get(String name, JsonType<T> type) {
        if (!pairs.containsKey(name))
            throw new RuntimeException("Incorrectly formatted Json, missing expected tag in object with name " + name);
        Object o = pairs.get(name);
        if (o instanceof Number n) {
            switch (type.numberType) {
                case FLOAT -> o = n.floatValue();
                case INTEGER -> o = n.intValue();
            }
        }
        if (!type.clazz.isInstance(o))
            throw new RuntimeException("Incorrectly formatted Json, encountered a type mismatch while loading json. Expected " +
                    "value of type " + type.debugName + ", but instead found a value of type " + o.getClass().getSimpleName());
        return (T) o;
    }

    public <T> T getOrDefault(String name, T defaultValue, JsonType<T> type) {
        if (!pairs.containsKey(name))
            return defaultValue;
        Object o = pairs.get(name);
        if (o instanceof Number n) {
            switch (type.numberType) {
                case FLOAT -> o = n.floatValue();
                case INTEGER -> o = n.intValue();
            }
        }
        if (!type.clazz.isInstance(o))
            throw new RuntimeException("Incorrectly formatted Json, encountered a type mismatch while loading json. Expected " +
                    "value of type " + type.debugName + ", but instead found a value of type " + o.getClass().getSimpleName());
        return (T) o;
    }

    public <T> void forEach(JsonType<T> type, BiConsumer<String, T> action) {
        for (Map.Entry<String, Object> entry : pairs.entrySet()) {
            Object o = entry.getValue();
            if (o instanceof Number n) {
                switch (type.numberType) {
                    case FLOAT -> o = n.floatValue();
                    case INTEGER -> o = n.intValue();
                }
            }
            action.accept(entry.getKey(), ((T) o));
        }
    }

    public boolean containsName(String name) {
        return pairs.containsKey(name);
    }

    @Override
    public String toString() {
        return pairs.toString();
    }
}
