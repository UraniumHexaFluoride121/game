package loader;

import foundation.Main;
import foundation.MainPanel;
import foundation.MathHelper;
import foundation.ObjPos;
import level.ObjectLayer;
import level.RandomType;
import level.objects.*;
import level.procedural.Layout;
import level.procedural.RegionType;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.LMType;
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

    public static BlockLike createBlock(String s, ObjPos pos) {
        return blocks.get(s).apply(pos);
    }

    public static void readLayoutMarkerData(ResourceLocation resource) {
        JsonObject paths = ((JsonObject) JsonLoader.readJsonResource(resource))
                .get("layoutMarkerData", JsonType.JSON_OBJECT_TYPE);
        LMType.values.forEach(v -> {
            if (!v.hasData())
                return;
            JsonObject data = ((JsonObject) JsonLoader.readJsonResource(new ResourceLocation(paths.get(v.s, JsonType.STRING_JSON_TYPE))));
            v.parseDataFromJson(data);
        });
    }

    public static void readLayout(ResourceLocation resource, Layout l) {
        JsonArray regions = ((JsonObject) JsonLoader.readJsonResource(resource))
                .get("regions", JsonType.JSON_ARRAY_TYPE);
        regions.forEach(r -> {
            int min = r.get("min", JsonType.INTEGER_JSON_TYPE), max = r.get("max", JsonType.INTEGER_JSON_TYPE);
            l.addRegion(
                    r.get("name", JsonType.STRING_JSON_TYPE),
                    l.getRegionTop() + MathHelper.randIntBetween(min, max, MainPanel.level.randomHandler.getDoubleSupplier(RandomType.REGIONS))
            );
        }, JsonType.JSON_OBJECT_TYPE);
    }

    public static void readRegions(ResourceLocation resource) {
        JsonArray regions = ((JsonObject) JsonLoader.readJsonResource(resource))
                .get("regionTypes", JsonType.JSON_ARRAY_TYPE);
        regions.forEach(r -> {
            RegionType.add(r.get("name", JsonType.STRING_JSON_TYPE));
        }, JsonType.JSON_OBJECT_TYPE);
    }

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
        JsonArray pages = ((JsonArray) JsonLoader.readJsonResource(resource));
        pages.forEach(obj -> {
            JsonArray blocksArray = obj.get("blocks", JsonType.JSON_ARRAY_TYPE);
            JsonObject key = obj.getOrDefault("key", null, JsonType.JSON_OBJECT_TYPE);
            JsonObject markerKey = obj.getOrDefault("markerKey", null, JsonType.JSON_OBJECT_TYPE);

            if (key == null && markerKey == null)
                throw new RuntimeException("Level section " + resource.toString() + " contains a page with no key or markerKey object");

            int size = blocksArray.size();
            blocksArray.forEachI((row, i) -> {
                char[] chars = row.toCharArray();
                for (int j = 0; j < Math.min(30, chars.length); j++) {
                    String s = String.valueOf(chars[j]);
                    if (!s.equals(" ")) {
                        int yOffset = (size - 1) - i + heightOffset;

                        if (key != null && key.containsName(s)) {
                            String name = key.get(s, JsonType.STRING_JSON_TYPE);
                            Function<ObjPos, ? extends BlockLike> blockCreationFunction = blocks.get(name);
                            if (blockCreationFunction == null)
                                throw new RuntimeException("Level section was created with unrecognised block \"" + name + "\"");
                            MainPanel.level.addBlocks(blockCreationFunction.apply(new ObjPos(j, yOffset)));
                        }
                        if (markerKey != null && markerKey.containsName(s)) {
                            JsonObject markerObj = markerKey.get(s, JsonType.JSON_OBJECT_TYPE);
                            int up = markerObj.getOrDefault("up", 0, JsonType.INTEGER_JSON_TYPE);
                            int down = markerObj.getOrDefault("down", 0, JsonType.INTEGER_JSON_TYPE);
                            int left = markerObj.getOrDefault("left", 0, JsonType.INTEGER_JSON_TYPE);
                            int right = markerObj.getOrDefault("right", 0, JsonType.INTEGER_JSON_TYPE);

                            int x = MathHelper.clampInt(0, Main.BLOCKS_X - 1, MathHelper.randIntBetween(j - left, j + right,
                                    MainPanel.level.randomHandler.getRandom(RandomType.PROCEDURAL)::nextDouble));
                            int y = MathHelper.clampInt(0, Main.BLOCKS_X - 1, MathHelper.randIntBetween(yOffset - down, yOffset + up,
                                    MainPanel.level.randomHandler.getRandom(RandomType.PROCEDURAL)::nextDouble));

                            LayoutMarker marker = new LayoutMarker(
                                    LMType.getLayoutMarker(markerObj.get("type", JsonType.STRING_JSON_TYPE)),
                                    new ObjPos(x, y)
                            );
                            MainPanel.level.layout.addMarker(marker);
                        }
                    }
                }
            }, JsonType.STRING_JSON_TYPE);
        }, JsonType.JSON_OBJECT_TYPE);
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

            float friction = blockObj.getOrDefault("friction", 1f, JsonType.FLOAT_JSON_TYPE);
            float bounciness = blockObj.getOrDefault("bounciness", 0f, JsonType.FLOAT_JSON_TYPE);

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
                    player.setFriction(friction);
                    player.setBounciness(bounciness);
                    return player.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "player", JsonType.STRING_JSON_TYPE)), player::getPos,
                            deserializeRenderable(texture)));
                });
                case STATIC_BLOCK -> blocks.put(blockName, pos -> {
                    if (layer.addToDynamic)
                        throw new IllegalArgumentException("staticBlocks type " + blockName + " was placed into a dynamic object layer " + layer);
                    StaticBlock staticBlock = new StaticBlock(pos, blockName, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, CollisionType.STATIC, layer, hasCollision);
                    staticBlock.setFriction(friction);
                    staticBlock.setBounciness(bounciness);
                    return staticBlock.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), staticBlock::getPos,
                            deserializeRenderable(texture)));
                });
                case MOVABLE_BLOCK -> blocks.put(blockName, pos -> {
                    StaticBlock staticBlock = new StaticBlock(pos, blockName, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, CollisionType.MOVABLE, ObjectLayer.DYNAMIC, hasCollision);
                    staticBlock.setFriction(friction);
                    staticBlock.setBounciness(bounciness);
                    return staticBlock.init(new RenderTexture(
                            RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), staticBlock::getPos,
                            deserializeRenderable(texture)));
                });
                case PHYSICS_BLOCK -> blocks.put(blockName, pos -> {
                    PhysicsBlock physicsBlock = new PhysicsBlock(pos, blockName,
                            blockObj.getOrDefault("mass", 1f, JsonType.FLOAT_JSON_TYPE),
                            hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight);
                    physicsBlock.setFriction(friction);
                    physicsBlock.setBounciness(bounciness);
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
            if (inputStream == null) {
                //When packaged as a jar file, the assets path cannot be accessed the same way
                inputStream = AssetManager.class.getResourceAsStream(resource.getPath("/"));
            }
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
