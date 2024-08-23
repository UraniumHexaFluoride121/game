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

    public JsonLoader(ResourceLocation resource) {
        this.resource = resource;
        InputStream stream = JsonLoader.class.getResourceAsStream(resource.getPath(ASSETS_PATH));
        if (stream == null)
            throw new RuntimeException("JsonLoader could not find the file with resource location " +
                    resource.relativePath + " using asset path " + ASSETS_PATH);
        dataStream = new DataInputStream(stream);
    }

    public JsonLoader beginObject() {
        try {
            if (readNextChar() != '{')
                throw new RuntimeException("Missing '{' on row " + currentRow + " in file " + resource.relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected '{'");
        }
        return this;
    }

    public JsonLoader endObject() {
        try {
            char c = readNextChar();
            if (c != '}')
                throw new RuntimeException("Missing '}' on row " + currentRow + " in file " + resource.relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected '}'");
        }
        return this;
    }
    
    public JsonLoader beginArray() {
        try {
            if (readNextChar() != '[')
                throw new RuntimeException("Missing '[' on row " + currentRow + " in file " + resource.relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file "+ resource.relativePath + ", expected '['");
        }
        return this;
    }

    public JsonLoader endArray() {
        try {
            char c = readNextChar();
            if (c != ']')
                throw new RuntimeException("Missing ']' on row " + currentRow + " in file " + resource.relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected ']'");
        }
        return this;
    }

    public String readName() {
        String s = readStringValue();
        try {
            if (readNextChar() != ':')
                throw new RuntimeException("Missing ':' on row " + currentRow + " in file " + resource.relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected a name");
        }
        return s;
    }

    public JsonLoader skipName() {
        readName();
        return this;
    }

    public int readIntValue() {
        try {
            boolean negative = false;
            StringBuilder value = new StringBuilder();
            byte buffer = dataStream.readByte();
            while (buffer == ' ' || buffer == '\n' || buffer == ',') {
                if (buffer == '\n')
                    currentRow++;
                buffer = dataStream.readByte();
            }

            while (buffer != ' ' && buffer != '\n' && buffer != 13 && buffer != ',') {
                if (buffer == '-')
                    negative = true;
                else
                    value.append(Character.getNumericValue(buffer));
                buffer = dataStream.readByte();
            }
            if (buffer == '\n')
                currentRow++;
            return negative ? -Integer.parseInt(value.toString()) : Integer.parseInt(value.toString());
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected an integer value");
        }
    }

    public float readFloatValue() {
        try {
            boolean negative = false;
            StringBuilder value = new StringBuilder();
            byte buffer = dataStream.readByte();
            while (buffer == ' ' || buffer == '\n' || buffer == ',') {
                if (buffer == '\n')
                    currentRow++;
                buffer = dataStream.readByte();
            }

            while (buffer != ' ' && buffer != '\n' && buffer != 13 && buffer != ',') {
                switch (buffer) {
                    case '.': {
                        value.append('.');
                        break;
                    }
                    case '-': {
                        negative = true;
                        break;
                    }
                    default: {
                        value.append(Character.getNumericValue(buffer));
                        break;
                    }
                }
                buffer = dataStream.readByte();
            }
            return negative ? -Float.parseFloat(value.toString()) : Float.parseFloat(value.toString());
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected a float value");
        }
    }

    public String readStringValue() {
        try {
            byte buffer;
            StringBuilder s = new StringBuilder();
            char c = readNextChar();
            if (c != '"')
                throw new RuntimeException("Missing \" on row " + currentRow + " in file " + resource.relativePath);
            buffer = dataStream.readByte();
            while (buffer != '"') {
                s.append((char) buffer);
                buffer = dataStream.readByte();
            }
            return s.toString();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected file termination in file " + resource.relativePath + ", expected a string");
        }
    }

    public void close() {
        try {
            dataStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private char readNextChar() throws IOException {
        char c;
        do {
            byte b = dataStream.readByte();
            c = (char) b;
            if (c == '\n')
                currentRow++;
        } while (c == 10 || c == 13 || c == ' ' || c == ',');
        return c;
    }
}
