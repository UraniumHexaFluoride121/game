package loader;

import render.AnimatedTexture;
import render.Renderable;
import render.TextureAsset;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

//Utility class to store any assets needed for the game. All asset loading requests
//must go through the AssetManager, so that already loaded assets don't get loaded
//more than once each
public abstract class AssetManager {
    //The path to the assets folder, relative to this class file
    private static final String ASSETS_PATH = "../assets/";

    private static final HashMap<ResourceLocation, TextureAsset> textureAssets = new HashMap<>();
    private static final HashMap<ResourceLocation, BufferedImage> textures = new HashMap<>();

    public static TextureAsset getTextureAsset(ResourceLocation resource) {
        if (textureAssets.containsKey(resource))
            return textureAssets.get(resource);
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        ResourceLocation imageResource = new ResourceLocation(obj.get("path", JsonType.STRING_JSON_TYPE));
        AffineTransform transform = new AffineTransform();
        if (obj.containsName("transform")) {
            JsonObject transformObject = obj.get("transform", JsonType.JSON_OBJECT_TYPE);
            transform.translate(
                    transformObject.getOrDefault("xOffset", 0f, JsonType.FLOAT_JSON_TYPE),
                    transformObject.getOrDefault("yOffset", 0f, JsonType.FLOAT_JSON_TYPE)
            );
            transform.scale(
                    transformObject.getOrDefault("xScale", 1f, JsonType.FLOAT_JSON_TYPE),
                    transformObject.getOrDefault("yScale", 1f, JsonType.FLOAT_JSON_TYPE)
            );
        }
        TextureAsset textureAsset = new TextureAsset(resource, getImage(imageResource), transform);
        textureAssets.put(resource, textureAsset);
        return textureAsset;
    }

    public static AnimatedTexture getAnimatedTexture(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray renderables = obj.get("renderables", JsonType.JSON_ARRAY_TYPE);
        AnimatedTexture texture = new AnimatedTexture(obj.getOrDefault("pickRandomFrame", false, JsonType.BOOLEAN_JSON_TYPE), obj.get("frameDuration", JsonType.FLOAT_JSON_TYPE));
        renderables.forEach(o -> {
            texture.addRenderable(deserializeRenderable(o));
        }, JsonType.JSON_OBJECT_TYPE);
        return texture;
    }

    public static BufferedImage getImage(ResourceLocation resource) {
        if (textures.containsKey(resource))
            return textures.get(resource);
        try {
            InputStream inputStream = AssetManager.class.getResourceAsStream(resource.getPath(ASSETS_PATH));
            if (inputStream == null)
                throw new RuntimeException("Unable to load image with path " + resource.relativePath);
            BufferedImage image = ImageIO.read(inputStream);
            textures.put(resource, image);
            return image;
        } catch (IOException e) {
            throw new RuntimeException("Error opening image file: " + e.getMessage());
        }
    }

    private static Renderable deserializeRenderable(JsonObject object) {
        String type = object.get("type", JsonType.STRING_JSON_TYPE);
        ResourceLocation path = new ResourceLocation(object.get("path", JsonType.STRING_JSON_TYPE));
        return switch (type) {
            case "TextureAsset" -> getTextureAsset(path);
            default -> throw new RuntimeException("Unknown Renderable type: " + type);
        };
    }
}
