package loader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class AssetLoader {

    public static Image getImage(String imagePath) {
        try {
            InputStream inputStream = AssetLoader.class.getResourceAsStream(imagePath);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            System.out.println("Error opening image file: " + e.getMessage());
            return null;
        }
    }
}
