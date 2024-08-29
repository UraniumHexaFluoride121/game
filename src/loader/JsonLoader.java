package loader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

//This Json parser should always work as long as the file is properly formatted, incorrectly
//formatted Json will result in unpredictable behaviour
public class JsonLoader {
    //The path to the assets folder, relative to this class file
    private static final String ASSETS_PATH = "../assets/";

    private final DataInputStream dataStream;
    private final ResourceLocation resource;
    private int currentRow = 0;

    private JsonLoader(ResourceLocation resource) {
        this.resource = resource;
        InputStream stream = JsonLoader.class.getResourceAsStream(resource.getPath(ASSETS_PATH));
        if (stream == null)
            throw new RuntimeException("JsonLoader could not find the file with resource location " +
                    resource.relativePath + " using asset path " + ASSETS_PATH);
        dataStream = new DataInputStream(stream);
    }

    public static JsonType readJsonResource(ResourceLocation r) {
        JsonLoader loader = new JsonLoader(r);
        try {
            char c = loader.readNextChar();
            if (c == '{')
                return loader.readObject();
            if (c == '[')
                return loader.readArray();
            throw new RuntimeException("Json file " + loader.resource.relativePath + " must start with '{' or '['");
        } catch (IOException e) {
            throw new RuntimeException("Empty file with asset path " + loader.resource.relativePath);
        }
    }

    public JsonObject readObject() {
        JsonObject o = new JsonObject();
        try {
            char c = readNextChar();
            while (true) {
                if (c == '}')
                    break;
                if (c != '"')
                    throw new RuntimeException("Missed a '\"' on row " + currentRow + " at file " + resource.relativePath);
                String name = readString();
                c = readNextChar();
                if (c != ':')
                    throw new RuntimeException("Missed a ':' on row " + currentRow + " at file " + resource.relativePath);

                c = readNextChar();

                Object value;
                switch (c) {
                    case '"' -> {
                        value = readString();
                        c = readNextChar();
                    }
                    case '{' -> {
                        value = readObject();
                        c = readNextChar();
                    }
                    case '[' -> {
                        value = readArray();
                        c = readNextChar();
                    }
                    default -> {
                        NumberResult r = readNumber(c);
                        value = r.number;
                        c = r.next;
                    }
                }
                o.pairs.put(name, value);
                if (c == ',')
                    c = readNextChar();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return o;
    }

    public JsonArray readArray() {
        JsonArray array = new JsonArray();
        try {
            char c = readNextChar();
            while (true) {
                if (c == ']')
                    break;


                Object value;
                switch (c) {
                    case '"' -> {
                        value = readString();
                        c = readNextChar();
                    }
                    case '{' -> {
                        value = readObject();
                        c = readNextChar();
                    }
                    case '[' -> {
                        value = readArray();
                        c = readNextChar();
                    }
                    default -> {
                        NumberResult r = readNumber(c);
                        value = r.number;
                        c = r.next;
                    }
                }
                array.items.add(value);
                if (c == ',')
                    c = readNextChar();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return array;
    }

    private String readString() {
        StringBuilder builder = new StringBuilder();
        try {
            char c;
            while (true) {
                c = readNextChar();
                if (c == '"')
                    break;
                builder.append(c);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Reached end of file while reading string at file " + resource.relativePath);
        }
    }

    private NumberResult readNumber(char firstChar) {
        try {
            StringBuilder s = new StringBuilder().append(firstChar);
            char c = readNextChar();
            boolean isFloat = false;
            while ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                if (c == '.') {
                    isFloat = true;
                    s.append(c);
                } else {
                    s.append(Character.getNumericValue(c));
                }
                c = readNextChar();
            }
            return new NumberResult(isFloat ? Float.parseFloat(s.toString()) : Integer.parseInt(s.toString()), c);
        } catch (IOException e) {
            throw new RuntimeException("Reached end of file while reading number at file " + resource.relativePath);
        }
    }

    private static class NumberResult {
        public Number number;
        public char next;

        public NumberResult(Number number, char next) {
            this.number = number;
            this.next = next;
        }
    }

    public static sealed class JsonType {
        public boolean isList;

        public JsonType(boolean isList) {
            this.isList = isList;
        }
    }

    public static final class JsonObject extends JsonType {
        public HashMap<String, Object> pairs = new HashMap<>();

        public JsonObject() {
            super(false);
        }

        @Override
        public String toString() {
            return pairs.toString();
        }
    }

    public static final class JsonArray extends JsonType {
        public ArrayList<Object> items = new ArrayList<>();

        public JsonArray() {
            super(true);
        }

        @Override
        public String toString() {
            return items.toString();
        }
    }

    private char readNextChar() throws IOException {
        char c;
        do {
            byte b = dataStream.readByte();
            c = (char) b;
            if (c == '\n')
                currentRow++;
        } while (c == 10 || c == 13 || c == ' ');
        return c;
    }
}
