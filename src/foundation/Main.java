package foundation;

import loader.JsonLoader;
import foundation.tick.Tick;
import loader.ResourceLocation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static final Tick TICK = new Tick();

    public static MainPanel window = new MainPanel();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::init);
    }

    public static final int BLOCKS_X = 30, MIN_BLOCKS_Y = 15; //The minimum number of blocks that have to be able to fit on the screen

    public static void init() {

        /*
         * Window set up will require a minimum of MIN_BLOCKS_Y to be displayed vertically, and exactly BLOCKS_X horizontally.
         * It will go into full-screen mode if both of these criteria can be achieved, and with each block being 16x16 pixels, this will
         * require that the width of the screen is a multiple of 16 * 30 (480) and that the height of the screen is at minimum half of the width.
         * If not, the screen is set to windowed mode, where we add a 5% margin to the sides of the screen and then pick the largest
         * screen size that'll work. If the screen is too small to allocate each texture pixel a screen pixel 1:1 (meaning less than about 480 x 240),
         * we'll full-screen it again but with a non-integer scaling. This option is not preferred as the textures will no longer be properly displayed.
         */
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        window.addKeyListener(window);
        //MainPanel.DEVICE_WINDOW_SIZE = new ObjPos(1000, 400);
        MainPanel.DEVICE_WINDOW_SIZE = new ObjPos(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height);
        MainPanel.RENDER_WINDOW_SIZE = MainPanel.DEVICE_WINDOW_SIZE.copy();
        int textureSize = (int) Math.min(MainPanel.RENDER_WINDOW_SIZE.x / (BLOCKS_X * 16), MainPanel.RENDER_WINDOW_SIZE.y / (MIN_BLOCKS_Y * 16)); //The screen-size height and width of a texture pixel


        window.setResizable(false);
        window.pack();

        if (textureSize == MainPanel.DEVICE_WINDOW_SIZE.x / (BLOCKS_X * 16) || textureSize < 1) {
            //If we didn't round down, it must mean that we can full-screen it. Either that, or
            //we full-screen it because the screen is too small
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(window);
        } else {
            MainPanel.RENDER_WINDOW_SIZE.multiply(0.95f, 0.95f); //If we can't full-screen it, we should add some margin to the window
            textureSize = (int) Math.min(MainPanel.RENDER_WINDOW_SIZE.x / (BLOCKS_X * 16), MainPanel.RENDER_WINDOW_SIZE.y / (MIN_BLOCKS_Y * 16)); //Recalculate texture size with the margin in mind
            MainPanel.RENDER_WINDOW_SIZE.set(textureSize * 16 * BLOCKS_X, MainPanel.RENDER_WINDOW_SIZE.y); //set the final size of the render box

            Insets insets = window.getInsets();
            //set screen size plus insets. There shouldn't be a problem with adding insets since we have margin.
            window.setSize(textureSize * 16 * BLOCKS_X + insets.left + insets.right, (int) MainPanel.RENDER_WINDOW_SIZE.y + insets.top + insets.bottom);
            MainPanel.gameTransform.translate(insets.left, insets.top);
        }
        MainPanel.BLOCK_DIMENSIONS = new ObjPos(BLOCKS_X, MainPanel.RENDER_WINDOW_SIZE.y / (textureSize * 16));

        //scale, flip and lower the render coordinate system.
        //This scales it from one render coordinate unit being the size of a screen pixel to
        //being the size of a block, and sets the origin of the coordinate system to be in the
        //bottom left corner instead of the top left. We also flip the y-axis so that y+ is up instead of down.
        MainPanel.gameTransform.scale(textureSize * 16, -textureSize * 16);
        MainPanel.gameTransform.translate(0, -MainPanel.BLOCK_DIMENSIONS.y);

        window.setVisible(true);

        try {
            window.createBufferStrategy(2, new BufferCapabilities(new ImageCapabilities(true), new ImageCapabilities(false), BufferCapabilities.FlipContents.PRIOR));
        } catch (AWTException e) {
            throw new RuntimeException("Failed to create buffer strategy");
        }
        window.init();

        TICK.start();
    }
}