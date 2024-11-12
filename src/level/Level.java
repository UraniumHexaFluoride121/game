package level;

import foundation.Deletable;
import foundation.Main;
import foundation.input.InputHandler;
import foundation.input.InputHandlingOrder;
import foundation.input.InputType;
import foundation.math.ObjPos;
import level.objects.BlockLike;
import level.objects.Player;
import level.procedural.Layout;
import level.procedural.RegionType;
import loader.AssetManager;
import physics.CollisionHandler;
import render.event.RenderEvent;
import render.renderables.RenderBackground;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import static foundation.MainPanel.*;

public class Level implements Deletable {
    public final int maximumHeight;
    public final InputHandler inputHandler;
    public final CollisionHandler collisionHandler;
    private static final int SECTION_SIZE = 16;
    public final int seed = 0;
    public final RandomHandler randomHandler;

    //All BlockLikes inserted as static MUST NOT have their positions modified, otherwise
    //they risk not being able to be accessed.
    //Multiple static blocks can be placed at the same position as long as they're on different
    //layers. This does NOT affect the rendering layer or render order, it is just to allow
    //multiple static blocks to be placed on top of each other. It can, however, affect how
    //the renderer does connected textures
    public final HashMap<ObjectLayer, BlockLike[][]> staticBlocks = new HashMap<>();
    //Set of all statics to allow for faster block updates, divided by level section to
    //allow async loading, with the bottom sections first
    public final HashSet<BlockLike>[] allStaticBlocks;

    //A set containing all dynamic blocks. Connected textures do not work for these blocks.
    public final HashSet<BlockLike> dynamicBlocks = new HashSet<>();

    public final Layout layout;

    //We store the order of each region, as well as when it appears in the level. The integer key is
    //the height that the region starts at
    private final TreeMap<Integer, RegionType> regionLayout = new TreeMap<>();

    public Player cameraPlayer = null;
    public final RenderBackground background = new RenderBackground(Color.WHITE);

    public Level() {
        randomHandler = new RandomHandler(seed);
        AssetManager.readLayout(LEVEL_PATH, this);

        int maximumHeight = getRegionTop() + 50;

        for (ObjectLayer layer : ObjectLayer.values()) {
            if (!layer.addToStatic)
                continue;
            staticBlocks.put(layer, new BlockLike[Main.BLOCKS_X][]);
            BlockLike[][] blockLayer = staticBlocks.get(layer);
            for (int i = 0; i < Main.BLOCKS_X; i++) {
                blockLayer[i] = new BlockLike[maximumHeight];
            }
        }
        int sectionCount = ((int) Math.ceil((double) maximumHeight / SECTION_SIZE)) + 2;
        allStaticBlocks = new HashSet[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            allStaticBlocks[i] = new HashSet<>();
        }

        inputHandler = new InputHandler();
        inputHandler.addInput(InputType.KEY_PRESSED, e -> upCamera = true, e -> e.getKeyCode() == KeyEvent.VK_PAGE_UP, InputHandlingOrder.CAMERA_UP, false);
        inputHandler.addInput(InputType.KEY_RELEASED, e -> upCamera = false, e -> e.getKeyCode() == KeyEvent.VK_PAGE_UP, InputHandlingOrder.CAMERA_UP, false);
        inputHandler.addInput(InputType.KEY_PRESSED, e -> downCamera = true, e -> e.getKeyCode() == KeyEvent.VK_PAGE_DOWN, InputHandlingOrder.CAMERA_DOWN, false);
        inputHandler.addInput(InputType.KEY_RELEASED, e -> downCamera = false, e -> e.getKeyCode() == KeyEvent.VK_PAGE_DOWN, InputHandlingOrder.CAMERA_DOWN, false);

        collisionHandler = new CollisionHandler(maximumHeight, SECTION_SIZE, 2);
        this.maximumHeight = maximumHeight;

        layout = new Layout(maximumHeight, SECTION_SIZE, 1);
    }

    public void init() {
        long time = System.currentTimeMillis();
        AssetManager.createAllLevelSections(LEVEL_PATH);
        layout.generateMarkers();
        updateBlocks(RenderEvent.ON_GAME_INIT);
        System.out.println("generation time: " + ((System.currentTimeMillis() - time) / 1000f));
    }

    public BlockLike getBlock(ObjectLayer layer, int x, int y) {
        if (x < 0 || x >= Main.BLOCKS_X || y < 0 || y >= maximumHeight || !layer.addToStatic)
            return null;
        return staticBlocks.get(layer)[x][y];
    }

    public boolean outOfBounds(int x, int y) {
        return x < 0 || x >= Main.BLOCKS_X || y < 0 || y >= maximumHeight;
    }

    public boolean outOfBounds(float x, float y) {
        return x < 0 || x >= Main.BLOCKS_X || y < 0 || y >= maximumHeight;
    }

    public boolean outOfBounds(ObjPos pos) {
        return outOfBounds(pos.x, pos.y);
    }

    public void addBlocks(BlockLike... blockLikes) {
        collisionHandler.register(blockLikes);
        for (BlockLike b : blockLikes) {
            if (b.getLayer().addToStatic) {
                BlockLike block = staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)];
                if (block != null)
                    removeBlocks(block);
                staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = b;
                allStaticBlocks[yPosToSection(b.pos.y)].add(b);
            }
            if (b.getLayer().addToDynamic)
                dynamicBlocks.add(b);
        }
    }

    public void removeBlocks(BlockLike... blockLikes) {
        for (BlockLike b : blockLikes) {
            if (b.getLayer().addToStatic) {
                staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = null;
                allStaticBlocks[yPosToSection(b.pos.y)].remove(b);
            }
            if (b.getLayer().addToDynamic)
                dynamicBlocks.remove(b);
            b.delete();
        }
        collisionHandler.remove(blockLikes);
    }

    //The procedural generator needs to know if it has overwritten a block in case the generation
    //needs to be reverted. This does not apply if the overwritten block is a part of the current
    //generation
    public BlockLike addProceduralBlock(BlockLike b) {
        if (outOfBounds(b.pos))
            return null;
        collisionHandler.register(b);
        BlockLike removed = null;
        if (b.getLayer().addToStatic) {
            BlockLike block = staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)];
            if (block != null) {
                removeBlocks(block);
                removed = block;
            }
            staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = b;
            allStaticBlocks[yPosToSection(b.pos.y)].add(b);
        }
        if (b.getLayer().addToDynamic)
            dynamicBlocks.add(b);
        return removed;
    }

    public void updateBlocks(RenderEvent type) {
        new Thread(() -> {
            long time = System.currentTimeMillis();
            for (HashSet<BlockLike> section : allStaticBlocks) {
                for (BlockLike b : section) {
                    b.renderUpdateBlock(type);
                }
            }
            System.out.println("block update time: " + ((System.currentTimeMillis() - time) / 1000f));
        }).start();
    }

    public void addRegion(String name, int startsAt) {
        regionLayout.put(startsAt, RegionType.getRegionType(name));
    }

    public RegionType getRegion(ObjPos pos) {
        Map.Entry<Integer, RegionType> region = regionLayout.ceilingEntry(((int) pos.y));
        if (region == null)
            return regionLayout.lastEntry().getValue();
        return region.getValue();
    }

    public int getRegionTop() {
        if (regionLayout.isEmpty())
            return 0;
        return regionLayout.lastKey();
    }

    public boolean upCamera = false, downCamera = false;

    public float getCameraOffset() {
        return 14 - (upCamera ? 7 : 0) + (downCamera ? 7 : 0);
    }

    private int yPosToSection(float y) {
        return ((int) (y / SECTION_SIZE));
    }

    @Override
    public void delete() {
        for (ObjectLayer layer : ObjectLayer.values()) {
            if (!layer.addToStatic)
                continue;
            BlockLike[][] blockLayer = staticBlocks.get(layer);
            for (int i = 0; i < Main.BLOCKS_X; i++) {
                for (int j = 0; j < maximumHeight; j++) {
                    BlockLike b = blockLayer[i][j];
                    if (b != null) {
                        b.delete();
                    }
                    blockLayer[i][j] = null;
                }
            }
        }
        dynamicBlocks.clear();
        inputHandler.delete();
        collisionHandler.delete();
    }
}
