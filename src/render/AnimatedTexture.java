package render;

import foundation.tick.AnimatedTickable;

import java.awt.*;
import java.util.ArrayList;

public class AnimatedTexture implements Renderable, AnimatedTickable {
    private final ArrayList<Renderable> elements = new ArrayList<>();
    private final ArrayList<Renderable> initial = new ArrayList<>();
    private int index = 0;
    private float currentTime = 0;
    private final float frameDuration;
    private final boolean pickRandomFrame;
    private boolean isOnInitial;

    public AnimatedTexture(boolean pickRandomFrame, float frameDuration) {
        this.pickRandomFrame = pickRandomFrame;
        this.frameDuration = frameDuration;
        isOnInitial = !pickRandomFrame;
    }

    public void addRenderable(Renderable r) {
        elements.add(r);
    }

    public void addRenderableInitial(Renderable r) {
        initial.add(r);
    }

    private ArrayList<Renderable> getActiveList() {
        return isOnInitial && !initial.isEmpty() ? initial : elements;
    }

    @Override
    public void tick(float deltaTime) {
        currentTime += deltaTime;
        if (currentTime > frameDuration) {
            currentTime -= frameDuration;
            if (pickRandomFrame) {
                index = ((int) (Math.random() * getActiveList().size()));
            } else {
                index++;
                if (index >= getActiveList().size()) {
                    index = 0;
                    isOnInitial = false;
                }
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        getActiveList().get(index).render(g);
    }

    @Override
    public void onSwitchTo() {
        if (!pickRandomFrame) {
            index = 0;
            isOnInitial = true;
        }
    }
}
