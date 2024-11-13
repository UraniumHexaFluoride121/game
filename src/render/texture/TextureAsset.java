package render.texture;

import foundation.Main;
import loader.*;
import render.Renderable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class TextureAsset implements Renderable {
    public ResourceLocation resource;
    public BufferedImage image;
    public AffineTransform transform;

    private TextureAsset(ResourceLocation resource, BufferedImage image, AffineTransform transform) {
        this.resource = resource;
        this.image = image;
        transform.scale(1, -1);
        transform.translate(0, -image.getHeight());
        this.transform = transform;
    }

    @Override
    public void render(Graphics2D g) {
        //We scale to convert blocks to texture pixels
        g.scale(1 / 16f, 1 / 16f);
        g.drawImage(image, transform, Main.window);
        g.scale(16, 16);
    }

    public static TextureAsset getTextureAsset(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        ResourceLocation imageResource = new ResourceLocation(obj.get("path", JsonType.STRING_JSON_TYPE));

        BufferedImage image = AssetManager.getImage(imageResource);
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
            if (transformObject.containsName("angle")) {
                float xPivot = image.getWidth() / 2f;
                float yPivot = image.getHeight() / 2f;
                transform.translate(xPivot, yPivot);
                transform.rotate(Math.toRadians(transformObject.get("angle", JsonType.FLOAT_JSON_TYPE)));
                transform.translate(-xPivot, -yPivot);
            }
        }
        return new TextureAsset(resource, image, transform);
    }

    public static ArrayList<TextureAsset> getMultiTextureAsset(ResourceLocation resource) {
        int index = resource.relativePath.lastIndexOf("#");
        ResourceLocation multiTextureResource = new ResourceLocation(resource.relativePath.substring(0, index));
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(multiTextureResource));
        JsonArray paths = obj.get("paths", JsonType.JSON_ARRAY_TYPE);
        ArrayList<TextureAsset> assets = new ArrayList<>();
        paths.forEach(path -> {
            BufferedImage image = AssetManager.getImage(new ResourceLocation(path));
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
                if (transformObject.containsName("angle")) {
                    float xPivot = image.getWidth() / 2f;
                    float yPivot = image.getHeight() / 2f;
                    transform.translate(xPivot, yPivot);
                    transform.rotate(Math.toRadians(transformObject.get("angle", JsonType.FLOAT_JSON_TYPE)));
                    transform.translate(-xPivot, -yPivot);
                }
            }
            String imageFile = path.substring(path.lastIndexOf("/") + 1);
            assets.add(new TextureAsset(
                    new ResourceLocation(multiTextureResource.relativePath + "#" + imageFile),
                    image,
                    transform
            ));
        }, JsonType.STRING_JSON_TYPE);
        return assets;
    }
}
