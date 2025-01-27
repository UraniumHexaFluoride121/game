package level;

import foundation.Deletable;
import foundation.Main;
import foundation.MainPanel;
import foundation.input.InputEvent;
import foundation.input.InputHandler;
import foundation.input.InputType;
import foundation.math.ObjPos;
import foundation.math.RandomHandler;
import level.objects.BlockLike;
import level.objects.Player;
import level.procedural.Layout;
import level.procedural.RegionType;
import level.procedural.generator.BoundType;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.resolved.LMTResolvedElement;
import loader.AssetManager;
import physics.CollisionHandler;
import physics.StaticHitBox;
import render.GameRenderer;
import render.event.RenderEvent;
import render.renderables.RenderBackground;
import render.ui.elements.UIProgressTracker;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static foundation.MainPanel.*;

public class Level implements Deletable {
    public static int levelIndexCounter = 0;
    public final int levelIndex;
    public int generationAttempts = 0;
    public int generatedMarkers = 0;
    public GameRenderer gameRenderer;
    public final int maximumHeight;
    public final InputHandler inputHandler;
    public final CollisionHandler collisionHandler;
    public static final int SECTION_SIZE = 16;
    public final long seed;
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
    public final Set<BlockLike>[] allStaticBlocks;

    //A set containing all dynamic blocks. Connected textures do not work for these blocks.
    public final HashSet<BlockLike> dynamicBlocks = new HashSet<>();

    public final Layout layout;

    //We store the order of each region, as well as when it appears in the level. The integer key is
    //the height that the region starts at
    private final TreeMap<Integer, RegionType> regionLayout = new TreeMap<>();

    public ObjPos spawnLocation = null;
    public Player cameraPlayer = null;
    public final HashMap<Integer, Player> players = new HashMap<>();
    public final RenderBackground background = new RenderBackground(new Color(185, 231, 241));

    public boolean deleted = false;
    public UIProgressTracker uiProgressTracker;

    public Level(long seed) {
        this(seed, levelIndexCounter++);
    }

    public Level(long seed, int index) {
        levelIndex = index;
        this.seed = seed;
        gameRenderer = new GameRenderer(gameTransform, MainPanel::getCameraTransform, this);
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
        allStaticBlocks = new Set[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            allStaticBlocks[i] = ConcurrentHashMap.newKeySet();
        }
        gameRenderer.createStaticsSet(sectionCount);

        inputHandler = new InputHandler();
        inputHandler.addInput(InputType.KEY_PRESSED, e -> upCamera = true, e -> e.getKeyCode() == KeyEvent.VK_PAGE_UP, InputEvent.CAMERA_UP, false);
        inputHandler.addInput(InputType.KEY_RELEASED, e -> upCamera = false, e -> e.getKeyCode() == KeyEvent.VK_PAGE_UP, InputEvent.CAMERA_UP, false);
        inputHandler.addInput(InputType.KEY_PRESSED, e -> downCamera = true, e -> e.getKeyCode() == KeyEvent.VK_PAGE_DOWN, InputEvent.CAMERA_DOWN, false);
        inputHandler.addInput(InputType.KEY_RELEASED, e -> downCamera = false, e -> e.getKeyCode() == KeyEvent.VK_PAGE_DOWN, InputEvent.CAMERA_DOWN, false);

        collisionHandler = new CollisionHandler(maximumHeight, SECTION_SIZE, 2);
        this.maximumHeight = maximumHeight;

        layout = new Layout(maximumHeight, SECTION_SIZE, 1, this);
    }

    public void finalise() {
        generateFull();
        createUI();
    }

    public AtomicBoolean interruptGeneration = new AtomicBoolean(false);
    public AtomicBoolean doneGenerating = new AtomicBoolean(false);
    public AtomicInteger generateTo = new AtomicInteger(((int) (BLOCK_DIMENSIONS.y + 20)));

    public void init() {
        AssetManager.createAllLevelSections(LEVEL_PATH, this);
        spawnLocation = AssetManager.getSpawnLocation(LEVEL_PATH);
        Thread generationThread = new Thread(() -> {
            long time = System.currentTimeMillis();
            updatePool = Executors.newCachedThreadPool();
            layout.generateMarkers();
            collisionHandler.clearProcedural();
            if (generateTo.get() == -1)
                System.out.println("-------------------[ Level fully generated ]-------------------");
            else
                System.out.println("-----------------[ Level partially generated ]-----------------");

            System.out.println("seed: " + seed);
            System.out.println("generation time: " + ((System.currentTimeMillis() - time) / 1000f));
            System.out.println("average generation attempts: " + ((float) generationAttempts) / generatedMarkers);
            for (int i = layout.markerSections.length - 1; i >= 0; i--) {
                float height = -1;
                for (LayoutMarker lm : layout.markerSections[i]) {
                    if (lm.type instanceof LMTResolvedElement && lm.pos.y > height)
                        height = lm.pos.y;
                }
                if (height != -1) {
                    System.out.println("max generation height: " + height);
                    break;
                }
            }
            System.out.println("generated markers: " + generatedMarkers);
            System.out.println("---------------------------------------------------------------");
            updatePool.shutdown();
            try {
                updatePool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            doneGenerating.set(true);
        });
        generationThread.start();
    }

    public void spawnPlayers(HashSet<Integer> clientIDs) {
        HashSet<Integer> remove = new HashSet<>();
        players.forEach((id, player) -> {
            if (!clientIDs.contains(id))
                remove.add(id);
            clientIDs.remove(id);
        });
        remove.forEach(id -> {
            removeBlocks(true, players.get(id));
            players.remove(id);
        });
        clientIDs.forEach(id -> {
            Player player = (Player) AssetManager.createBlock("player", spawnLocation.copy(), this);
            player.updateColour(id);
            players.put(id, player);
            addBlocks(true, false, player);
        });
        Player activePlayer = players.get(getClientID());
        activePlayer.addInput(inputHandler);
        cameraPlayer = activePlayer;
    }

    public void generateFull() {
        generateTo.set(-1);
    }

    public void createUI() {
        uiProgressTracker = new UIProgressTracker(0, gameRenderer, this);
        gameRenderer.registerUI(uiProgressTracker.startTime());
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

    public synchronized void addBlocks(boolean registerCollision, boolean registerProcedural, BlockLike... blockLikes) {
        if (registerCollision)
            collisionHandler.register(blockLikes);
        if (registerProcedural)
            collisionHandler.registerProcedural(blockLikes);
        for (BlockLike b : blockLikes) {
            if (b.getLayer().addToStatic) {
                BlockLike block = staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)];
                if (block != null)
                    removeBlocks(registerCollision, block);
                staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = b;
                allStaticBlocks[yPosToSection(b.pos.y)].add(b);
            }
            if (b.getLayer().addToDynamic)
                dynamicBlocks.add(b);
        }
    }

    public synchronized void removeBlocks(boolean registerCollision, BlockLike... blockLikes) {
        for (BlockLike b : blockLikes) {
            if (b.getLayer().addToStatic) {
                staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)] = null;
                allStaticBlocks[yPosToSection(b.pos.y)].remove(b);
            }
            if (b.getLayer().addToDynamic)
                dynamicBlocks.remove(b);
            b.delete();
        }
        if (registerCollision)
            collisionHandler.remove(blockLikes);
        collisionHandler.removeProcedural(blockLikes);
    }

    //The procedural generator needs to know if it has overwritten a block in case the generation
    //needs to be reverted. This does not apply if the overwritten block is a part of the current
    //generation
    public synchronized BlockLike addProceduralBlock(boolean registerCollision, boolean registerProcedural, BlockLike b) {
        if (outOfBounds(b.pos))
            return null;
        if (registerCollision)
            collisionHandler.register(b);
        if (registerProcedural)
            collisionHandler.registerProcedural(b);
        BlockLike removed = null;
        if (b.getLayer().addToStatic) {
            BlockLike block = staticBlocks.get(b.getLayer())[((int) b.pos.x)][((int) b.pos.y)];
            if (block != null) {
                removeBlocks(registerCollision, block);
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
            for (Set<BlockLike> section : allStaticBlocks) {
                for (BlockLike b : section) {
                    b.renderUpdateBlock(type);
                }
            }
            System.out.println("block update time: " + ((System.currentTimeMillis() - time) / 1000f));
        }).start();
    }

    public void updateBlocks(RenderEvent type, LayoutMarker lm) {
        updatePool.submit(() -> {
            StaticHitBox updateBounds = lm.boundForBounds(BoundType.BLOCKS).expand(2);
            for (int i = yPosToSection(updateBounds.getBottom()); i <= yPosToSection(updateBounds.getTop()); i++) {
                Set<BlockLike> sectionSet = allStaticBlocks[i];
                for (BlockLike b : sectionSet) {
                    b.renderUpdateBlock(type);
                }
            }
        });
    }

    private ExecutorService updatePool;

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

    public int yPosToSection(float y) {
        return ((int) (y / SECTION_SIZE));
    }

    @Override
    public void delete() {
        deleted = true;
        interruptGeneration.set(true);
        generateTo.set(-2);
        while (!doneGenerating.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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
        dynamicBlocks.forEach(BlockLike::delete);
        dynamicBlocks.clear();
        inputHandler.delete();
        collisionHandler.delete();
        gameRenderer.delete();
    }
}
