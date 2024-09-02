package loader;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class JsonArray extends JsonDataStructure {
    public ArrayList<Object> items = new ArrayList<>();

    public JsonArray() {
        super(true);
    }

    public <T> T get(int index, JsonType<T> type) {
        if (!items.contains(index))
            throw new RuntimeException("Incorrectly formatted Json, missing expected item in array with index " + index);
        if (items.get(index) instanceof Number n) {
            switch (type.numberType) {
                case FLOAT -> n.floatValue();
                case INTEGER -> n.intValue();
            }
        }
        return (T) items.get(index);
    }

    public <T> void forEach(Consumer<T> action, JsonType<T> type) {
        items.forEach(o -> {
            if (o instanceof Number n) {
                switch (type.numberType) {
                    case FLOAT -> o = n.floatValue();
                    case INTEGER -> o = n.intValue();
                }
            }
            action.accept((T) o);
        });
    }

    public <T> void forEachI(BiConsumer<T, Integer> action, JsonType<T> type) {
        for (int i = 0; i < items.size(); i++) {
            Object o = items.get(i);
            if (o instanceof Number n) {
                switch (type.numberType) {
                    case FLOAT -> o = n.floatValue();
                    case INTEGER -> o = n.intValue();
                }
            }
            action.accept((T) o, i);
        }
    }

    public int size() {
        return items.size();
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
