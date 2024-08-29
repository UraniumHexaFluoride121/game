package loader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class AssetLoader {
    //The path to the assets folder, relative to this class file
    private static final String ASSETS_PATH = "../assets/";

    public static BufferedImage getImage(ResourceLocation r) {
        try {
            InputStream inputStream = AssetLoader.class.getResourceAsStream(r.getPath(ASSETS_PATH));
            if (inputStream == null)
                throw new RuntimeException("Unable to load image with relative path " + r.relativePath + " and asset path " + ASSETS_PATH);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            System.out.println("Error opening image file: " + e.getMessage());
            return null;
        }
    }
}
