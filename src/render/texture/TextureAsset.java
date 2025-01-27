package render.texture;

import foundation.Main;
import loader.*;
import render.TickedRenderable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.function.Function;

public class TextureAsset implements TickedRenderable {
    public ResourceLocation resource;
    public BufferedImage image;
    public AffineTransform transform;

    private TextureAsset(ResourceLocation resource, BufferedImage image, AffineTransform transform, boolean flip) {
        this.resource = resource;
        this.image = image;
        if (flip) {
            transform.scale(1, -1);
            transform.translate(0, -image.getHeight());
        }
        this.transform = transform;
    }

    public TextureAsset colourModified(RescaleOp op) {
        return new TextureAsset(resource, op.filter(image, op.createCompatibleDestImage(image, image.getColorModel())), transform, false);
    }

    @Override
    public void render(Graphics2D g) {
        //We scale to convert blocks to texture pixels
        g.scale(1 / 16f, 1 / 16f);
        g.drawImage(image, transform, Main.window);
        g.scale(16, 16);
    }

    @Override
    public boolean requiresTick() {
        return false;
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
        if (obj.containsName("colorFactor")) {
            JsonObject colorFactorObj = obj.get("colorFactor", JsonType.JSON_OBJECT_TYPE);
            float rFactor = colorFactorObj.getOrDefault("r", 1f, JsonType.FLOAT_JSON_TYPE);
            float gFactor = colorFactorObj.getOrDefault("g", 1f, JsonType.FLOAT_JSON_TYPE);
            float bFactor = colorFactorObj.getOrDefault("b", 1f, JsonType.FLOAT_JSON_TYPE);
            float aFactor = colorFactorObj.getOrDefault("a", 1f, JsonType.FLOAT_JSON_TYPE);
            Function<Graphics2D, RescaleOp> op4 = g -> new RescaleOp(new float[]{rFactor, gFactor, bFactor, aFactor}, new float[]{0, 0, 0, 0}, g.getRenderingHints());
            if (image.getData().getNumDataElements() == 4) {
                op4.apply(image.createGraphics()).filter(image, image);
            } else
                System.out.println("[WARNING] Formatting of image with path \"" + imageResource.relativePath + "\" does not support color modification");
        }
        return new TextureAsset(resource, image, transform, true);
    }

    public static ArrayList<TextureAsset> getMultiTextureAsset(ResourceLocation resource) {
        int index = resource.relativePath.lastIndexOf("#");
        ResourceLocation multiTextureResource = new ResourceLocation(resource.relativePath.substring(0, index));
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(multiTextureResource));
        JsonArray paths = obj.get("paths", JsonType.JSON_ARRAY_TYPE);
        ArrayList<TextureAsset> assets = new ArrayList<>();
        Function<Graphics2D, RescaleOp> op4;
        if (obj.containsName("colorFactor")) {
            JsonObject colorFactorObj = obj.get("colorFactor", JsonType.JSON_OBJECT_TYPE);
            float rFactor = colorFactorObj.getOrDefault("r", 1f, JsonType.FLOAT_JSON_TYPE);
            float gFactor = colorFactorObj.getOrDefault("g", 1f, JsonType.FLOAT_JSON_TYPE);
            float bFactor = colorFactorObj.getOrDefault("b", 1f, JsonType.FLOAT_JSON_TYPE);
            float aFactor = colorFactorObj.getOrDefault("a", 1f, JsonType.FLOAT_JSON_TYPE);
            op4 = g -> new RescaleOp(new float[]{rFactor, gFactor, bFactor, aFactor}, new float[]{0, 0, 0, 0}, g.getRenderingHints());
        } else {
            op4 = null;
        }
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
            if (op4 != null) {
                if (image.getData().getNumDataElements() == 4)
                    op4.apply(image.createGraphics()).filter(image, image);
                else
                    System.out.println("[WARNING] Formatting of image with path \"" + path + "\" does not support color modification");
            }
            assets.add(new TextureAsset(
                    new ResourceLocation(multiTextureResource.relativePath + "#" + imageFile),
                    image,
                    transform,
                    true));
        }, JsonType.STRING_JSON_TYPE);
        return assets;
    }

    @Override
    public String toString() {
        return resource.toString();
    }
}
