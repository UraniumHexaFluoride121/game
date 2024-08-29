package render;

import foundation.tick.Tickable;

import java.awt.*;
import java.util.ArrayList;

public class AnimatedTexture implements Renderable, Tickable {
    private final ArrayList<Renderable> loop = new ArrayList<>();
    private int index = 0;
    private float currentTime = 0;
    private final float frameDuration;
    private final boolean pickRandomFrame;

    public AnimatedTexture(boolean pickRandomFrame, float frameDuration) {
        this.pickRandomFrame = pickRandomFrame;
        this.frameDuration = frameDuration;
    }

    public AnimatedTexture addRenderable(Renderable r) {
        loop.add(r);
        return this;
    }

    @Override
    public void tick(float deltaTime) {
        currentTime += deltaTime;
        if (currentTime > frameDuration) {
            currentTime -= frameDuration;
            if (pickRandomFrame) {
                index = ((int) (Math.random() * loop.size()));
            } else {
                index++;
                if (index >= loop.size()) {
                    index = 0;
                }
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        loop.get(index).render(g);
    }
}
