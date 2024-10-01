package loader;

import foundation.MainPanel;
import foundation.ObjPos;
import level.ObjectLayer;
import level.objects.*;
import physics.CollisionType;
import render.RenderOrder;
import render.Renderable;
import render.event.RenderEvent;
import render.renderables.RenderTexture;
import render.texture.*;
import render.texture.ct.ConnectedTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.function.Function;

//Utility class to store any assets needed for the game. All asset loading requests
//must go through the AssetManager, so that already loaded assets don't get loaded
//more than once each
public abstract class AssetManager {
    //The path to the assets folder, relative to this class file
    private static final String ASSETS_PATH = "../assets/";

    private static final HashMap<ResourceLocation, BufferedImage> textures = new HashMap<>();
    public static final HashMap<String, Function<ObjPos, ? extends BlockLike>> blocks = new HashMap<>();

    public static void createAllLevelSections(ResourceLocation resource) {
        JsonArray sections = ((JsonObject) JsonLoader.readJsonResource(resource))
                .getOrDefault("insertedSections", new JsonArray(), JsonType.JSON_ARRAY_TYPE);
        sections.forEach(section -> {
            createLevelSection(
                    new ResourceLocation(section.get("path", JsonType.STRING_JSON_TYPE)),
                    section.get("heightOffset", JsonType.INTEGER_JSON_TYPE));
        }, JsonType.JSON_OBJECT_TYPE);
    }

    public static void createLevelSection(ResourceLocation resource, int heightOffset) {
        if (heightOffset < 0)
            throw new IllegalArgumentException("Level section " + resource.toString() + " was created with negative height offset");
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray blocksArray = obj.get("blocks", JsonType.JSON_ARRAY_TYPE);
        JsonObject key = obj.get("key", JsonType.JSON_OBJECT_TYPE);
        int size = blocksArray.size();
        blocksArray.forEachI((row, i) -> {
            char[] chars = row.toCharArray();
            for (int j = 0; j < Math.min(30, chars.length); j++) {
                String s = String.valueOf(chars[j]);
                if (!s.equals(" ") && key.containsName(s)) {
                    String name = key.get(s, JsonType.STRING_JSON_TYPE);
                    Function<ObjPos, ? extends BlockLike> blockCreationFunction = blocks.get(name);
                    if (blockCreationFunction == null)
                        throw new RuntimeException("Level section was created with unrecognised block \"" + name + "\"");
                    MainPanel.level.addBlocks(blockCreationFunction.apply(new ObjPos(j, (size - 1) - i + heightOffset)));
                }
            }
        }, JsonType.STRING_JSON_TYPE);
    }

    public static void readBlocks(ResourceLocation resource) {
        JsonObject obj = ((JsonObject) JsonLoader.readJsonResource(resource));
        JsonArray blocksArray = obj.get("blocks", JsonType.JSON_ARRAY_TYPE);
        blocksArray.forEach(blockPath -> {
            JsonObject blockObj = ((JsonObject) JsonLoader.readJsonResource(new ResourceLocation(blockPath)));
            BlockType type = BlockType.getBlockType(blockObj.getOrDefault("type", "staticBlock", JsonType.STRING_JSON_TYPE));
            String blockName = blockObj.get("name", JsonType.STRING_JSON_TYPE);
            if (blockName.equals("player")) {
                type = BlockType.PLAYER;
            } else if (type == BlockType.PLAYER) {
                throw new RuntimeException("Only the block called \"player\" is allowed to have type PLAYER");
            }
            JsonObject hitBox = blockObj.getOrDefault("hitBox", null, JsonType.JSON_OBJECT_TYPE);

            JsonObject texture = blockObj.get("texture", JsonType.JSON_OBJECT_TYPE);

            float hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight;
            if (hitBox != null) {
                hitBoxUp = hitBox.getOrDefault("up", 16f, JsonType.FLOAT_JSON_TYPE) / 16;
                hitBoxDown = hitBox.getOrDefault("down", 0f, JsonType.FLOAT_JSON_TYPE) / 16;
                hitBoxLeft = hitBox.getOrDefault("left", 0f, JsonType.FLOAT_JSON_TYPE) / 16;
                hitBoxRight = hitBox.getOrDefault("right", 16f, JsonType.FLOAT_JSON_TYPE) / 16;
            } else {
                hitBoxUp = 1;
                hitBoxDown = 0;
                hitBoxLeft = 0;
                hitBoxRight = 1;
            }
            if (hitBoxUp + hitBoxDown < 0 || hitBoxRight + hitBoxLeft < 0)
                throw new IllegalArgumentException("HitBox cannot have negative size");

            boolean hasCollision = blockObj.getOrDefault("hasCollision", true, JsonType.BOOLEAN_JSON_TYPE);
            ObjectLayer layer = ObjectLayer.getObjectLayer(texture.getOrDefault("layer", "foreground", JsonType.STRING_JSON_TYPE));

            switch (type) {
                case PLAYER -> blocks.put(blockName, pos -> {
                    Player player = new Player(pos, blockName,
                            blockObj.getOrDefault("mass", 1f, JsonType.FLOAT_JSON_TYPE),
                            hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, MainPanel.getInputHandler());
                    return player.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "player", JsonType.STRING_JSON_TYPE)), player::getPos,
                            deserializeRenderable(texture)));
                });
                case STATIC_BLOCK -> blocks.put(blockName, pos -> {
                    if (layer.addToDynamic)
                        throw new IllegalArgumentException("staticBlocks type " + blockName + " was placed into a dynamic object layer " + layer);
                    StaticBlock staticBlock = new StaticBlock(pos, blockName, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, CollisionType.STATIC, layer, hasCollision);
                    return staticBlock.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), staticBlock::getPos,
                            deserializeRenderable(texture)));
                });
                case MOVABLE_BLOCK -> blocks.put(blockName, pos -> {
                    StaticBlock staticBlock = new StaticBlock(pos, blockName, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, CollisionType.MOVABLE, ObjectLayer.DYNAMIC, hasCollision);
                    return staticBlock.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), staticBlock::getPos,
                            deserializeRenderable(texture)));
                });
                case PHYSICS_BLOCK -> blocks.put(blockName, pos -> {
                    PhysicsBlock physicsBlock = new PhysicsBlock(pos, blockName,
                            blockObj.getOrDefault("mass", 1f, JsonType.FLOAT_JSON_TYPE),
                            hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight);
                    return physicsBlock.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), physicsBlock::getPos,
                            deserializeRenderable(texture)));
                });
            }

        }, JsonType.STRING_JSON_TYPE);

        if (!blocks.containsKey("player"))
            throw new RuntimeException("A player block was not defined");
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

    public static Renderable deserializeRenderable(JsonObject object) {
        String type = object.get("type", JsonType.STRING_JSON_TYPE);
        ResourceLocation path = new ResourceLocation(object.get("path", JsonType.STRING_JSON_TYPE));
        return switch (type) {
            case "TextureAsset" -> TextureAsset.getTextureAsset(path);
            case "AnimatedTexture" -> AnimatedTexture.getAnimatedTexture(path);
            case "LayeredTexture" -> LayeredTexture.getLayeredTexture(path);
            case "EventSwitcherTexture" -> EventSwitcherTexture.getEventSwitcherTexture(path);
            case "RandomTexture" -> RandomTexture.getRandomTexture(path);
            case "ConnectedTexture" -> ConnectedTexture.getConnectedTextures(path);
            default -> throw new IllegalArgumentException("Unknown Renderable type: " + type);
        };
    }

    public static RenderEvent deserializeRenderEvent(String event) {
        if (!RenderEvent.ALL_EVENTS.containsKey(event))
            throw new IllegalArgumentException("Unknown RenderEvent: " + event);
        return RenderEvent.ALL_EVENTS.get(event);
    }
}
