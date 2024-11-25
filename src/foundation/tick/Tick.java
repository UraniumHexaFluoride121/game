package foundation.tick;

import foundation.Main;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Tick extends Thread {
    private final Set<RegisteredTickable> qRegister = ConcurrentHashMap.newKeySet(), qRemove = ConcurrentHashMap.newKeySet();
    private final TreeMap<TickOrder, HashSet<RegisteredTickable>> tickables = new TreeMap<>();

    public Tick() {
        for (TickOrder value : TickOrder.values()) {
            tickables.put(value, new HashSet<>());
        }
    }

    public synchronized void register(RegisteredTickable t) {
        qRegister.add(t);
    }

    public synchronized void remove(RegisteredTickable t) {
        qRemove.add(t);
    }

    //we queue tickables when removing and adding to avoid ConcurrentModificationException
    private synchronized void processQueued() {
        qRegister.forEach(t -> tickables.get(t.getTickOrder()).add(t));
        qRegister.clear();

        qRemove.forEach(t -> tickables.get(t.getTickOrder()).remove(t));
        qRemove.clear();
    }

    //The maximum delta time allowed by the game. If we have a delta time larger
    //than this, we'll cap the delta time resulting in the game time running slower
    //than normal. This is to avoid physics problems that would otherwise happen,
    //for example weird snapping and blocks phasing through each other
    public static final float MAX_DELTA_TIME = 0.01f;

    //If the dT is too long, we cap the dT to avoid lag spikes when the game
    //falls behind. This can happen if the player tabs out of the game, for example
    public static final float TOTAL_DELTA_TIME_CAP = 0.1f;

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        long newTime;
        while (true) {
            //calculate deltaTime
            newTime = System.currentTimeMillis();
            float deltaTime = (newTime - time) / 1000f;
            time = newTime;

            //add and remove tickables
            processQueued();

            //tick tickable objects
            deltaTime = Math.min(deltaTime, TOTAL_DELTA_TIME_CAP);
            do {
                float finalDT = deltaTime;
                deltaTime -= MAX_DELTA_TIME;
                tickables.forEach((order, set) -> set.forEach(t -> t.tick(Math.min(finalDT, MAX_DELTA_TIME))));
            } while (deltaTime > 0);


            //render frame
            BufferStrategy strategy = Main.window.getBufferStrategy();
            do {
                Graphics drawGraphics = strategy.getDrawGraphics();
                Main.window.paintComponents(drawGraphics);
                drawGraphics.dispose();
                strategy.show();
            } while (strategy.contentsRestored());
            //We make sure that a detectable amount of time has passed before processing the next tick
            //If we don't, the physics may not function properly
            try {
                TimeUnit.MILLISECONDS.sleep(3 - (System.currentTimeMillis() - time));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
