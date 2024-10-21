package foundation.tick;

import foundation.Main;

import java.util.HashSet;
import java.util.TreeMap;

public class Tick extends Thread {
    private final HashSet<RegisteredTickable> qRegister = new HashSet<>(), qRemove = new HashSet<>();
    private final TreeMap<TickOrder, HashSet<RegisteredTickable>> tickables = new TreeMap<>();

    public Tick() {
        for (TickOrder value : TickOrder.values()) {
            tickables.put(value, new HashSet<>());
        }
    }

    public void register(RegisteredTickable t) {
        qRegister.add(t);
    }

    public void remove(RegisteredTickable t) {
        qRemove.add(t);
    }

    //we queue tickables when removing and adding to avoid ConcurrentModificationException
    private void processQueued() {
        qRegister.forEach(t -> tickables.get(t.getTickOrder()).add(t));
        qRegister.clear();

        qRemove.forEach(t -> tickables.get(t.getTickOrder()).remove(t));
        qRemove.clear();
    }

    //The maximum delta time allowed by the game. If we have a delta time larger
    //than this, we'll cap the delta time resulting in the game time running slower
    //than normal. This is to avoid physics problems that would otherwise happen,
    //for example weird snapping and blocks phasing through each other
    public static final float MAX_DELTA_TIME = 0.03f;

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
            tickables.forEach((order, set) -> set.forEach(t -> t.tick(Math.min(deltaTime, MAX_DELTA_TIME))));

            //render frame
            Main.window.paintComponents(Main.window.getBufferStrategy().getDrawGraphics());
            Main.window.getBufferStrategy().show();
            //We make sure that a detectable amount of time has passed before processing the next tick
            //If we don't, the physics may not function properly
            while (System.currentTimeMillis() - time < 3);
        }
    }
}
