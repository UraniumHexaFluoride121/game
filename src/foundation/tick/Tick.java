package foundation.tick;

import foundation.Main;
import render.Renderer;

import java.util.HashSet;
import java.util.TreeMap;

public class Tick extends Thread {
    private final HashSet<Tickable> qRegister = new HashSet<>(), qRemove = new HashSet<>();
    private final TreeMap<TickOrder, HashSet<Tickable>> tickables = new TreeMap<>();

    public Tick() {
        for (TickOrder value : TickOrder.values()) {
            tickables.put(value, new HashSet<>());
        }
    }

    public void register(Tickable t) {
        qRegister.add(t);
    }

    public void remove(Tickable t) {
        qRemove.add(t);
    }

    //we queue tickables when removing and adding to avoid ConcurrentModificationException
    private void processQueued() {
        qRegister.forEach(t -> tickables.get(t.getTickOrder()).add(t));
        qRegister.clear();

        qRemove.forEach(t -> tickables.get(t.getTickOrder()).remove(t));
        qRemove.clear();
    }

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
            tickables.forEach((order, set) -> set.forEach(t -> t.tick(deltaTime)));

            //render frame
            Main.window.paintComponents(Main.window.getBufferStrategy().getDrawGraphics());
            Main.window.getBufferStrategy().show();
        }
    }
}
