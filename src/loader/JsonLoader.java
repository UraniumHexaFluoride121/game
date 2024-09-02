package loader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public static JsonDataStructure readJsonResource(ResourceLocation r) {
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

    private JsonObject readObject() {
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
                    case 'f', 't' -> {
                        value = readBoolean(c);
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

    private JsonArray readArray() {
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
                    case 'f', 't' -> {
                        value = readBoolean(c);
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
                c = readNextCharRaw();
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
            boolean hasDecimalPlace = false;
            while ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                if (c == '.') {
                    hasDecimalPlace = true;
                    s.append(c);
                } else {
                    s.append(Character.getNumericValue(c));
                }
                c = readNextChar();
            }
            return new NumberResult(hasDecimalPlace ? Float.parseFloat(s.toString()) : Integer.parseInt(s.toString()), c);
        } catch (IOException e) {
            throw new RuntimeException("Reached end of file while reading number at file " + resource.relativePath);
        }
    }

    private boolean readBoolean(char firstChar) {
        try {
            StringBuilder s = new StringBuilder().append(firstChar);
            for (int i = 0; i < (firstChar == 'f' ? 4 : 3); i++) {
                s.append(readNextChar());
            }
            return s.toString().equals("true");
        } catch (IOException e) {
            throw new RuntimeException("Reached end of file while reading boolean at file " + resource.relativePath);
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

    private char readNextCharRaw() throws IOException {
        char c;
        byte b = dataStream.readByte();
        c = (char) b;
        if (c == '\n')
            currentRow++;
        return c;
    }

    private static class NumberResult {
        public Number number;
        public char next;

        public NumberResult(Number number, char next) {
            this.number = number;
            this.next = next;
        }
    }
}
