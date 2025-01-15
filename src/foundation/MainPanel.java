package foundation;

import foundation.input.InputEvent;
import foundation.input.InputType;
import foundation.math.MathUtil;
import foundation.math.ObjPos;
import foundation.tick.RegisteredTickable;
import foundation.tick.TickOrder;
import level.Level;
import level.objects.Player;
import loader.AssetManager;
import loader.ResourceLocation;
import network.Client;
import network.NetworkState;
import network.PacketWriter;
import network.Server;
import render.ui.GameState;
import render.ui.UIRenderer;
import render.ui.button.ButtonState;
import render.ui.button.ClickableRegister;
import render.ui.elements.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainPanel extends JFrame implements KeyListener, MouseListener, RegisteredTickable {
    //path to the main level file
    public static final ResourceLocation LEVEL_PATH = new ResourceLocation("level.json");
    public static AffineTransform gameTransform = new AffineTransform();

    public static ObjPos DEVICE_WINDOW_SIZE; //the physical screen size, in pixels
    public static ObjPos RENDER_WINDOW_SIZE; //the size of the render box, in pixels
    public static ObjPos BLOCK_DIMENSIONS; //the size of the render box, in blocks

    public static GameState state = GameState.MAIN_MENU;

    public static int currentLevelIndex = 0;

    private static Level mainLevel;
    public static final HashMap<Integer, Level> loadedLevels = new HashMap<>();
    public static final HashMap<Integer, Boolean> finalisedLevels = new HashMap<>();

    private static final UIRenderer mainMenuUI = new UIRenderer();
    private static final ClickableRegister mainMenuClickables = new ClickableRegister();
    private static UISeedSelector seedSelector;
    private static float levelTint = 0.3f;
    private static float mainMenuYOffset = 0f;

    public static Server server;
    public static Client client;

    public static NetworkState networkState = NetworkState.NONE;
    private static UIHostServer serverButton;
    private static UIServerAddressBox serverAddressBox;

    public MainPanel() throws HeadlessException {
        super();
        registerTickable();
    }

    public void init() {
        AssetManager.readRegions(LEVEL_PATH);
        AssetManager.readBlocks(LEVEL_PATH);
        AssetManager.readGlyphs(LEVEL_PATH);
        AssetManager.readUIAssets(LEVEL_PATH);
        AssetManager.readLayoutMarkerData(LEVEL_PATH);

        seedSelector = new UISeedSelector(0, mainMenuUI, mainMenuClickables);

        UIPlayButton playButton = new UIPlayButton(0, mainMenuUI, mainMenuClickables);

        serverButton = new UIHostServer(0, mainMenuUI, mainMenuClickables);

        serverAddressBox = new UIServerAddressBox(0, mainMenuUI, mainMenuClickables);
    }

    public static AtomicBoolean updatePlayers = new AtomicBoolean(false);

    public static void createNewMainLevel(long seed) {
        deleteAllLevels();
        mainLevel = addNewLevel(seed, false);
        setActiveLevel(mainLevel.levelIndex);
        updatePlayers.set(true);
        if (networkState == NetworkState.SERVER) {
            server.clients.forEach((id, c) -> c.setLevel(mainLevel));
        }
        cameraY = 1;
    }

    public static Level addNewLevel(long seed, boolean finalise) {
        Level l = new Level(seed);
        l.init();
        loadedLevels.put(l.levelIndex, l);
        finalisedLevels.put(l.levelIndex, finalise);
        if (finalise) {
            l.finalise();
        }
        return l;
    }

    public static Level addNewLevel(long seed, boolean finalise, int index) {
        Level l = new Level(seed, index);
        l.init();
        loadedLevels.put(l.levelIndex, l);
        finalisedLevels.put(l.levelIndex, finalise);
        if (finalise) {
            l.finalise();
        }
        return l;
    }

    public static Level getLevel(int index) {
        return loadedLevels.get(index);
    }

    public static void setActiveLevel(int index) {
        currentLevelIndex = index;
    }

    public static void deleteAllLevels() {
        loadedLevels.forEach((i, l) -> l.delete());
        loadedLevels.clear();
        finalisedLevels.clear();
        mainLevel = null;
    }

    public static void deleteLevel(int index) {
        if (mainLevel != null && mainLevel.levelIndex == index)
            mainLevel = null;
        if (loadedLevels.containsKey(index))
            loadedLevels.get(index).delete();
        loadedLevels.remove(index);
        finalisedLevels.remove(index);
    }

    public static void startMainLevel() {
        if (networkState == NetworkState.CLIENT) {
            if (levelFullyGenerated(currentLevelIndex))
                state = GameState.PLAYING_LEVEL;
        } else {
            state = GameState.PLAYING_LEVEL;
            if (mainLevel.seed != seedSelector.getSeed())
                createNewMainLevel(seedSelector.getSeed());
            mainLevel.finalise();
            finalisedLevels.put(mainLevel.levelIndex, true);
        }
    }

    public static void startServer() {
        if (networkState != NetworkState.NONE)
            return;
        networkState = NetworkState.SERVER;
        server = new Server();
        serverAddressBox.delete();
        serverAddressBox.delete();
    }

    public static void startClient() {
        new Thread(() -> {
            if (networkState != NetworkState.NONE)
                return;
            UIConnectToClient.setConnectionState(UIClientConnectionState.TRYING_CONNECTION);
            networkState = NetworkState.CLIENT;
            client = new Client(serverAddressBox.getText());
            if (client.failed) {
                client = null;
                networkState = NetworkState.NONE;
                UIConnectToClient.setConnectionState(UIClientConnectionState.CONNECTION_FAILED);
                serverAddressBox.allowClick();
                return;
            }
            serverButton.delete();
            MainPanel.addTask(MainPanel::deleteAllLevels);
            UIConnectToClient.setConnectionState(UIClientConnectionState.CONNECTED);
        }).start();
    }

    public static void sendClientPacket(PacketWriter packet) {
        if (networkState == NetworkState.CLIENT)
            client.queuePacket(packet);
    }

    public static int getClientID() {
        return networkState == NetworkState.CLIENT ? client.clientID : 0;
    }

    public static HashSet<Integer> getClientIDs() {
        HashSet<Integer> ids;
        if (networkState == NetworkState.SERVER) {
            ids = server.getClientIDs();
            ids.add(0);
        } else {
            ids = new HashSet<>();
            ids.add(0);
        }
        return ids;
    }

    public static void handleClientPlayerInput(InputEvent event, InputType type, int clientID, int levelIndex) {
        Player player = getLevel(levelIndex).players.get(clientID);
        if (player != null) {
            player.handleInput(event, type);
        }
    }

    @Override
    public void paintComponents(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (levelTint != 1)
            g2d.drawImage(levelToImage(), new RescaleOp(new float[]{levelTint}, new float[]{0}, g2d.getRenderingHints()), 0, 0);
        else
            drawLevel(g2d);

        if (mainMenuYOffset != BLOCK_DIMENSIONS.y) {
            AffineTransform prev = g2d.getTransform();
            g2d.transform(gameTransform);
            g2d.translate(0, mainMenuYOffset);
            mainMenuUI.render(g2d);
            g2d.setTransform(prev);
        }
    }

    public BufferedImage levelToImage() {
        BufferedImage image = new BufferedImage(DEVICE_WINDOW_SIZE.xInt(), DEVICE_WINDOW_SIZE.yInt(), Image.SCALE_DEFAULT);
        Graphics2D g2d = image.createGraphics();
        drawLevel(g2d);
        return image;
    }

    public void drawLevel(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        //Render background separately so that it's not affected by camera movement
        Level l = getLevel(currentLevelIndex);
        if (levelExists(l)) {
            AffineTransform prev = g2d.getTransform();
            g2d.transform(gameTransform);
            l.background.render(g2d);
            g2d.setTransform(prev);

            l.gameRenderer.render(g2d);
        }
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.LEVEL_UPDATES;
    }

    public static float cameraY = 1;

    public static AffineTransform getCameraTransform() {
        AffineTransform t = new AffineTransform();
        t.translate(0, cameraY);
        return t;
    }

    private static final ArrayList<Runnable> tasks = new ArrayList<>();

    public static void addTask(Runnable r) {
        synchronized (Main.window) {
            MainPanel.tasks.add(r);
        }
    }

    public static float sendLevelPacketTimer = 0;
    private static final float sendLevelPacketInterval = 0.03f;

    @Override
    public void tick(float deltaTime) {
        processQueuedInputs();
        if (updatePlayers.get() && mainLevelExists()) {
            updatePlayers.set(false);
            if (networkState != NetworkState.CLIENT)
                mainLevel.spawnPlayers(getClientIDs());
            if (networkState == NetworkState.SERVER) {
                server.clients.forEach((id, c) -> {
                    if (c.levelIndex == -1 && mainLevelExists())
                        c.setLevel(mainLevel);
                });
            }
        }
        if (state == GameState.MAIN_MENU) {
            seedSelector.tick(deltaTime);
            levelTint = MathUtil.linearTo(levelTint, 0.3f, 3, deltaTime);
            mainMenuYOffset = MathUtil.linearTo(mainMenuYOffset, 0, 30, deltaTime);
        } else {
            levelTint = MathUtil.linearTo(levelTint, 1, 3, deltaTime);
            mainMenuYOffset = MathUtil.linearTo(mainMenuYOffset, BLOCK_DIMENSIONS.y, BLOCK_DIMENSIONS.y * 3, deltaTime);
        }
        Level l = getLevel(currentLevelIndex);
        if (levelExists(l) && l.cameraPlayer != null) {
            float y = Math.min(0, -l.cameraPlayer.pos.y + l.getCameraOffset());
            if (cameraY == 1)
                cameraY = y;
            else {
                cameraY += (y - cameraY) * deltaTime * 2.5f;
                cameraY += Math.min(Math.abs(y - cameraY), 0.3f * deltaTime) * Math.signum(y - cameraY);
            }
        }
        synchronized (this) {
            tasks.forEach(Runnable::run);
            tasks.clear();
        }
        if (networkState == NetworkState.SERVER) {
            sendLevelPacketTimer += deltaTime;
            if (sendLevelPacketTimer > sendLevelPacketInterval) {
                sendLevelPacketTimer = 0;
                server.sendLevelPacket(loadedLevels, finalisedLevels);
                server.sendPhysicsUpdate(loadedLevels);
            }
        }
    }

    public static boolean mainLevelExists() {
        return mainLevel != null && !mainLevel.deleted;
    }

    public static boolean levelExists(Level l) {
        return l != null && !l.deleted;
    }

    public static boolean levelFullyGenerated(int index) {
        return levelExists(getLevel(index)) && finalisedLevels.get(index);
    }

    private final Vector<Runnable> qInputs = new Vector<>();

    private synchronized void processQueuedInputs() {
        qInputs.forEach(Runnable::run);
        qInputs.clear();
    }

    @Override
    public synchronized void keyTyped(KeyEvent e) {

    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        qInputs.add(() -> {
            Level l = getLevel(currentLevelIndex);
            if (state == GameState.PLAYING_LEVEL && levelExists(l)) {
                l.inputHandler.queueInput(InputType.KEY_PRESSED, e);
                if (networkState == NetworkState.CLIENT)
                    Player.sendClientInput(InputType.KEY_PRESSED, e);
            } else if (state == GameState.MAIN_MENU) {
                seedSelector.keyPressed(e);
                serverAddressBox.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER && serverAddressBox.state == ButtonState.INACTIVE)
                    startMainLevel();
            }
        });
    }

    @Override
    public synchronized void keyReleased(KeyEvent e) {
        qInputs.add(() -> {
            Level l = getLevel(currentLevelIndex);
            if (state == GameState.PLAYING_LEVEL && levelExists(l)) {
                l.inputHandler.queueInput(InputType.KEY_RELEASED, e);
                if (networkState == NetworkState.CLIENT)
                    Player.sendClientInput(InputType.KEY_RELEASED, e);
            }
        });
    }

    @Override
    public synchronized void mouseClicked(MouseEvent e) {

    }

    @Override
    public synchronized void mousePressed(MouseEvent e) {
        qInputs.add(() -> {
            Point point = getMousePosition();
            if (point == null)
                return;
            ObjPos pos = new ObjPos(point).divide((float) gameTransform.getScaleX(), ((float) gameTransform.getScaleY())).addY(BLOCK_DIMENSIONS.y);
            if (state == GameState.MAIN_MENU)
                mainMenuClickables.mousePressed(e, pos);
        });
    }

    @Override
    public synchronized void mouseReleased(MouseEvent e) {
        qInputs.add(() -> {
            Point point = getMousePosition();
            if (point == null)
                return;
            ObjPos pos = new ObjPos(point).divide((float) gameTransform.getScaleX(), ((float) gameTransform.getScaleY())).addY(BLOCK_DIMENSIONS.y);
            if (state == GameState.MAIN_MENU)
                mainMenuClickables.mouseReleased(e, pos);
        });
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void delete() {

    }
}
