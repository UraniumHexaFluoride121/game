package render;

import foundation.Main;
import loader.ResourceLocation;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class TextureAsset implements Renderable {
    public ResourceLocation resource;
    public BufferedImage image;
    public AffineTransform transform;

    public TextureAsset(ResourceLocation resource, BufferedImage image, AffineTransform transform) {
        this.resource = resource;
        this.image = image;
        transform.scale(1, -1);
        transform.translate(0, -image.getHeight());
        this.transform = transform;
    }

    @Override
    public void render(Graphics2D g) {
        //We scale to convert blocks to texture pixels
        g.scale(1/16f, 1/16f);
        g.drawImage(image, transform, Main.window);
        g.scale(16, 16);
    }
}
