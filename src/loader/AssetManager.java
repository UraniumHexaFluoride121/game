package loader;

import foundation.Main;
import foundation.math.MathUtil;
import foundation.math.ObjPos;
import foundation.math.RandomType;
import level.Level;
import level.ObjectLayer;
import level.objects.*;
import level.procedural.RegionType;
import level.procedural.generator.GeneratorType;
import level.procedural.marker.LMType;
import level.procedural.marker.LayoutMarker;
import physics.CollisionType;
import physics.StaticHitBox;
import render.RenderOrder;
import render.TickedRenderable;
import render.renderables.RenderTexture;
import render.texture.*;
import render.texture.ct.ConnectedTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

//Utility class to store any assets needed for the game. All asset loading requests
//must go through the AssetManager, so that already loaded assets don't get loaded
//more than once each
public abstract class AssetManager {
    //The path to the assets folder, relative to this class file
    private static final String ASSETS_PATH = "../assets/";

    private static final HashMap<ResourceLocation, BufferedImage> textures = new HashMap<>();
    public static final HashMap<String, BiFunction<ObjPos, Level, ? extends BlockLike>> blocks = new HashMap<>();
    public static final HashMap<String, StaticHitBox> blockHitBoxes = new HashMap<>();
    public static final HashMap<String, Float> blockFriction = new HashMap<>(), blockBounciness = new HashMap<>();

    public static final HashMap<Character, GlyphData> glyphs = new HashMap<>();
    public static final HashMap<String, GlyphData> specialGlyphs = new HashMap<>();

    public static BlockLike createBlock(String s, ObjPos pos, Level level) {
        return blocks.get(s).apply(pos, level);
    }

    public static void readGlyphs(ResourceLocation resource) {
        JsonObject dataElements = (JsonObject) JsonLoader.readJsonResource(new ResourceLocation(((JsonObject) JsonLoader.readJsonResource(resource))
                .get("glyphs", JsonType.STRING_JSON_TYPE)));
        dataElements.forEach(JsonType.JSON_OBJECT_TYPE, (k, v) -> {
            GlyphData data = new GlyphData(v.get("width", JsonType.INTEGER_JSON_TYPE), deserializeRenderable(v).apply(null));
            if (k.length() == 1) {
                glyphs.put(k.charAt(0), data);
            } else {
                specialGlyphs.put(k, data);
            }
        });
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
        JsonObject generationPaths = ((JsonObject) JsonLoader.readJsonResource(resource))
                .get("generationData", JsonType.JSON_OBJECT_TYPE);
        for (GeneratorType type : GeneratorType.values) {
            if (!type.hasData())
                return;
            JsonObject data = ((JsonObject) JsonLoader.readJsonResource(new ResourceLocation(generationPaths.get(type.s, JsonType.STRING_JSON_TYPE))));
            type.parseDataFromJson(data);
        }
    }

    public static void readLayout(ResourceLocation resource, Level l) {
        JsonArray regions = ((JsonObject) JsonLoader.readJsonResource(resource))
                .get("regions", JsonType.JSON_ARRAY_TYPE);
        regions.forEach(r -> {
            int min = r.get("min", JsonType.INTEGER_JSON_TYPE), max = r.get("max", JsonType.INTEGER_JSON_TYPE);
            l.addRegion(
                    r.get("name", JsonType.STRING_JSON_TYPE),
                    l.getRegionTop() + MathUtil.randIntBetween(min, max, l.randomHandler.getDoubleSupplier(RandomType.REGIONS))
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

    public static void createAllLevelSections(ResourceLocation resource, Level level) {
        JsonArray sections = ((JsonObject) JsonLoader.readJsonResource(resource))
                .getOrDefault("insertedSections", new JsonArray(), JsonType.JSON_ARRAY_TYPE);
        sections.forEach(section -> {
            createLevelSection(
                    new ResourceLocation(section.get("path", JsonType.STRING_JSON_TYPE)),
                    section.get("heightOffset", JsonType.INTEGER_JSON_TYPE), level);
        }, JsonType.JSON_OBJECT_TYPE);
    }

    public static void createLevelSection(ResourceLocation resource, int heightOffset, Level level) {
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
                for (int j = 0; j < Math.min(Main.BLOCKS_X, chars.length); j++) {
                    String s = String.valueOf(chars[j]);
                    if (!s.equals(" ")) {
                        int yOffset = (size - 1) - i + heightOffset;

                        if (key != null && key.containsName(s)) {
                            String name = key.get(s, JsonType.STRING_JSON_TYPE);
                            BiFunction<ObjPos, Level, ? extends BlockLike> blockCreationFunction = blocks.get(name);
                            if (blockCreationFunction == null)
                                throw new RuntimeException("Level section was created with unrecognised block \"" + name + "\"");
                            BlockLike block = blockCreationFunction.apply(new ObjPos(j, yOffset), level);
                            level.addBlocks(true, true, block);
                            if (block instanceof Player p)
                                level.cameraPlayer = p;
                        }
                        if (markerKey != null && markerKey.containsName(s)) {
                            JsonObject markerObj = markerKey.get(s, JsonType.JSON_OBJECT_TYPE);
                            int up = markerObj.getOrDefault("up", 0, JsonType.INTEGER_JSON_TYPE);
                            int down = markerObj.getOrDefault("down", 0, JsonType.INTEGER_JSON_TYPE);
                            int left = markerObj.getOrDefault("left", 0, JsonType.INTEGER_JSON_TYPE);
                            int right = markerObj.getOrDefault("right", 0, JsonType.INTEGER_JSON_TYPE);

                            int x = MathUtil.clampInt(0, Main.BLOCKS_X - 1, MathUtil.randIntBetween(j - left, j + right,
                                    level.randomHandler.getRandom(RandomType.PROCEDURAL)::nextDouble));
                            int y = MathUtil.clampInt(0, Main.BLOCKS_X - 1, MathUtil.randIntBetween(yOffset - down, yOffset + up,
                                    level.randomHandler.getRandom(RandomType.PROCEDURAL)::nextDouble));

                            LayoutMarker marker = new LayoutMarker(
                                    markerObj.get("type", JsonType.STRING_JSON_TYPE),
                                    new ObjPos(x, y), level
                            );
                            level.layout.addMarker(marker);
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

            blockFriction.put(blockName, friction);
            blockBounciness.put(blockName, bounciness);
            blockHitBoxes.put(blockName, new StaticHitBox(hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight));
            switch (type) {
                case PLAYER -> {
                    Function<Level, ? extends TickedRenderable> textureSupplier = deserializeRenderable(texture);
                    blocks.put(blockName, (pos, level) -> {
                        Player player = new Player(pos, blockName,
                                blockObj.getOrDefault("mass", 1f, JsonType.FLOAT_JSON_TYPE),
                                hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, level.inputHandler, level);
                        player.setFriction(friction);
                        player.setBounciness(bounciness);
                        return player.init(new RenderTexture(
                                RenderOrder.getRenderOrder(texture.getOrDefault("order", "player", JsonType.STRING_JSON_TYPE)), player::getPos,
                                textureSupplier.apply(level)));
                    });
                }
                case STATIC_BLOCK -> {
                    if (layer.addToDynamic)
                        throw new IllegalArgumentException("staticBlocks type " + blockName + " was placed into a dynamic object layer " + layer);
                    Function<Level, ? extends TickedRenderable> textureSupplier = deserializeRenderable(texture);
                    blocks.put(blockName, (pos, level) -> {
                        StaticBlock staticBlock = new StaticBlock(pos, blockName, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, CollisionType.STATIC, layer, hasCollision, level);
                        staticBlock.setFriction(friction);
                        staticBlock.setBounciness(bounciness);
                        return staticBlock.init(new RenderTexture(
                                RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), staticBlock::getPos,
                                textureSupplier.apply(level)));
                    });
                }
                case MOVABLE_BLOCK -> {
                    Function<Level, ? extends TickedRenderable> textureSupplier = deserializeRenderable(texture);
                    blocks.put(blockName, (pos, level) -> {
                        StaticBlock staticBlock = new StaticBlock(pos, blockName, hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, CollisionType.MOVABLE, ObjectLayer.DYNAMIC, hasCollision, level);
                        staticBlock.setFriction(friction);
                        staticBlock.setBounciness(bounciness);
                        return staticBlock.init(new RenderTexture(
                                RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), staticBlock::getPos,
                                textureSupplier.apply(level)));
                    });
                }
                case PHYSICS_BLOCK -> {
                    Function<Level, ? extends TickedRenderable> textureSupplier = deserializeRenderable(texture);
                    blocks.put(blockName, (pos, level) -> {
                        PhysicsBlock physicsBlock = new PhysicsBlock(pos, blockName,
                                blockObj.getOrDefault("mass", 1f, JsonType.FLOAT_JSON_TYPE),
                                hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, level);
                        physicsBlock.setFriction(friction);
                        physicsBlock.setBounciness(bounciness);
                        return physicsBlock.init(new RenderTexture(
                                RenderOrder.getRenderOrder(texture.getOrDefault("order", "block", JsonType.STRING_JSON_TYPE)), physicsBlock::getPos,
                                textureSupplier.apply(level)));
                    });
                }
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
            if (image == null)
                throw new RuntimeException("Image was null with path: " + resource.relativePath);
            textures.put(resource, image);
            return image;
        } catch (IOException e) {
            throw new RuntimeException("Error opening image file with path " + resource.relativePath + " : " + e.getMessage());
        }
    }

    private static final HashMap<ResourceLocation, TextureAsset> textureAssets = new HashMap<>();
    private static final HashMap<ResourceLocation, Function<Level, AnimatedTexture>> animatedTextures = new HashMap<>();
    private static final HashMap<ResourceLocation, Function<Level, LayeredTexture>> layeredTextures = new HashMap<>();
    private static final HashMap<ResourceLocation, Function<Level, EventSwitcherTexture>> eventSwitcherTextures = new HashMap<>();
    private static final HashMap<ResourceLocation, Function<Level, RandomTexture>> randomTextures = new HashMap<>();
    private static final HashMap<ResourceLocation, Function<Level, ConnectedTexture>> connectedTextures = new HashMap<>();

    public static Function<Level, ? extends TickedRenderable> deserializeRenderable(JsonObject object) {
        String type = object.get("type", JsonType.STRING_JSON_TYPE);
        ResourceLocation resource = new ResourceLocation(object.get("path", JsonType.STRING_JSON_TYPE));
        return switch (type) {
            case "TextureAsset" -> {
                if (textureAssets.containsKey(resource)) {
                    TextureAsset asset = textureAssets.get(resource);
                    yield level -> asset;
                }
                if (resource.relativePath.contains("#")) {
                    ArrayList<TextureAsset> assets = TextureAsset.getMultiTextureAsset(resource);
                    assets.forEach(a -> textureAssets.put(a.resource, a));
                    TextureAsset asset = textureAssets.get(resource);
                    yield level -> asset;
                } else {
                    TextureAsset asset = TextureAsset.getTextureAsset(resource);
                    textureAssets.put(resource, asset);
                    yield level -> asset;
                }
            }
            case "AnimatedTexture" -> {
                if (animatedTextures.containsKey(resource)) {
                    yield animatedTextures.get(resource);
                }
                Function<Level, AnimatedTexture> t = AnimatedTexture.getAnimatedTexture(resource);
                animatedTextures.put(resource, t);
                yield t;
            }
            case "LayeredTexture" -> {
                if (layeredTextures.containsKey(resource)) {
                    yield layeredTextures.get(resource);
                }
                Function<Level, LayeredTexture> t = LayeredTexture.getLayeredTexture(resource);
                layeredTextures.put(resource, t);
                yield t;
            }
            case "EventSwitcherTexture" -> {
                if (eventSwitcherTextures.containsKey(resource)) {
                    yield eventSwitcherTextures.get(resource);
                }
                Function<Level, EventSwitcherTexture> t = EventSwitcherTexture.getEventSwitcherTexture(resource);
                eventSwitcherTextures.put(resource, t);
                yield t;
            }
            case "RandomTexture" -> {
                if (randomTextures.containsKey(resource)) {
                    yield randomTextures.get(resource);
                }
                Function<Level, RandomTexture> t = RandomTexture.getRandomTexture(resource);
                randomTextures.put(resource, t);
                yield t;
            }
            case "ConnectedTexture" -> {
                if (connectedTextures.containsKey(resource)) {
                    yield connectedTextures.get(resource);
                }
                Function<Level, ConnectedTexture> t = ConnectedTexture.getConnectedTexture(resource);
                connectedTextures.put(resource, t);
                yield t;
            }
            default -> throw new IllegalArgumentException("Unknown Renderable type: " + type);
        };
    }
}
