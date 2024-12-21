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
import network.PacketWriter;
import network.NetworkState;
import network.Server;
import render.ui.*;
import render.ui.button.Clickable;
import render.ui.elements.UIConnectToClient;
import render.ui.elements.UIHostServer;
import render.ui.elements.UIPlayButton;
import render.ui.elements.UISeedSelector;

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

    private Level activeLevel;

    private final UIRenderer mainMenuUI = new UIRenderer();
    private final ArrayList<Clickable> mainMenuClickables = new ArrayList<>();
    private UISeedSelector seedSelector;
    private float levelTint = 0.3f;
    private float mainMenuYOffset = 0f;

    public static Server server;
    public static Client client;

    public static NetworkState networkState = NetworkState.NONE;

    public MainPanel() throws HeadlessException {
        super();
        registerTickable();
    }

    public void init() {
        AssetManager.readRegions(LEVEL_PATH);
        AssetManager.readBlocks(LEVEL_PATH);
        AssetManager.readGlyphs(LEVEL_PATH);
        AssetManager.readLayoutMarkerData(LEVEL_PATH);

        seedSelector = new UISeedSelector(0, mainMenuUI);
        mainMenuClickables.add(seedSelector);

        UIPlayButton playButton = new UIPlayButton(0, mainMenuUI);
        mainMenuClickables.add(playButton);

        UIHostServer serverButton = new UIHostServer(0, mainMenuUI);
        mainMenuClickables.add(serverButton);

        UIConnectToClient clientButton = new UIConnectToClient(0, mainMenuUI);
        mainMenuClickables.add(clientButton);
    }

    public static AtomicBoolean updatePlayers = new AtomicBoolean(false);

    public void createAndSetNewLevel(long seed) {
        if (isLevelActive())
            activeLevel.delete();
        activeLevel = new Level(seed);
        activeLevel.init();
        updatePlayers.set(true);
        cameraY = 1;
    }

    public void startLevel() {
        state = GameState.PLAYING_LEVEL;
        if (activeLevel.seed != seedSelector.getSeed())
            createAndSetNewLevel(seedSelector.getSeed());
        activeLevel.generateFull();
        activeLevel.createUI();
    }

    public void startServer() {
        if (networkState != NetworkState.NONE)
            return;
        networkState = NetworkState.SERVER;
        server = new Server();
    }

    public void startClient() {
        if (networkState != NetworkState.NONE)
            return;
        networkState = NetworkState.CLIENT;
        System.out.println("new client");
        client = new Client("127.0.0.1");
        System.out.println("new client");
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

    public void handleClientPlayerInput(InputEvent event, InputType type, int clientID) {
        Player player = activeLevel.players.get(clientID);
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
        if (isLevelActive()) {
            AffineTransform prev = g2d.getTransform();
            g2d.transform(gameTransform);
            activeLevel.background.render(g2d);
            g2d.setTransform(prev);

            activeLevel.gameRenderer.render(g2d);
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

    @Override
    public void tick(float deltaTime) {
        processQueuedInputs();
        if (updatePlayers.get() && isLevelActive()) {
            updatePlayers.set(false);
            activeLevel.spawnPlayers(getClientIDs());
        }
        if (state == GameState.MAIN_MENU) {
            seedSelector.tick(deltaTime);
            levelTint = MathUtil.linearTo(levelTint, 0.3f, 3, deltaTime);
            mainMenuYOffset = MathUtil.linearTo(mainMenuYOffset, 0, 30, deltaTime);
        } else {
            levelTint = MathUtil.linearTo(levelTint, 1, 3, deltaTime);
            mainMenuYOffset = MathUtil.linearTo(mainMenuYOffset, BLOCK_DIMENSIONS.y, BLOCK_DIMENSIONS.y * 3, deltaTime);
        }
        if (activeLevel != null && !activeLevel.deleted && activeLevel.cameraPlayer != null) {
            float y = Math.min(0, -activeLevel.cameraPlayer.pos.y + activeLevel.getCameraOffset());
            if (cameraY == 1)
                cameraY = y;
            else {
                cameraY += (y - cameraY) * deltaTime * 2.5f;
                cameraY += Math.min(Math.abs(y - cameraY), 0.3f * deltaTime) * Math.signum(y - cameraY);
            }
        }
    }

    private boolean isLevelActive() {
        return activeLevel != null && !activeLevel.deleted;
    }

    private Vector<Runnable> qInputs = new Vector<>();

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
            if (state == GameState.PLAYING_LEVEL && activeLevel != null && !activeLevel.deleted)
                activeLevel.inputHandler.queueInput(InputType.KEY_PRESSED, e);
            if (state == GameState.MAIN_MENU) {
                seedSelector.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    startLevel();
            }
        });
    }

    @Override
    public synchronized void keyReleased(KeyEvent e) {
        qInputs.add(() -> {
            if (state == GameState.PLAYING_LEVEL && activeLevel != null && !activeLevel.deleted)
                activeLevel.inputHandler.queueInput(InputType.KEY_RELEASED, e);
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
                mainMenuClickables.forEach(c -> c.onClick(e, pos, true, c.clickBox().isPositionInside(pos)));
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
                mainMenuClickables.forEach(c -> c.onClick(e, pos, false, c.clickBox().isPositionInside(pos)));
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
