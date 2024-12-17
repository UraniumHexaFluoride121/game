package render.ui;

import foundation.Deletable;
import render.Renderable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class UIRenderer implements Renderable, UIRegister, Deletable {
    private ArrayList<UIRenderable> elements = new ArrayList<>();

    @Override
    public synchronized void render(Graphics2D g) {
        elements.forEach(r -> r.render(g));
    }

    @Override
    public synchronized void registerUI(UIRenderable r) {
        elements.add(r);
        elements.sort(Comparator.comparingInt(UIRenderable::getZOrder));
    }

    @Override
    public synchronized void removeUI(UIRenderable r) {
        elements.remove(r);
        elements.sort(Comparator.comparingInt(UIRenderable::getZOrder));
    }

    @Override
    public void delete() {
        elements.clear();
    }
}
