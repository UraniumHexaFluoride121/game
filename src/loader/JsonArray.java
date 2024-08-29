package loader;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class JsonArray extends JsonDataStructure {
    public ArrayList<Object> items = new ArrayList<>();

    public JsonArray() {
        super(true);
    }

    public <T> T get(int index, JsonType<T> type) {
        if (!items.contains(index))
            throw new RuntimeException("Incorrectly formatted Json, missing expected item in array with index " + index);
        return (T) items.get(index);
    }


    public <T> void forEach(Consumer<T> action, JsonType<T> type) {
        items.forEach(o -> action.accept((T) o));
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
