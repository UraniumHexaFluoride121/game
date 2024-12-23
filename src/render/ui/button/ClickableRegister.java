package render.ui.button;

import foundation.Deletable;
import foundation.math.ObjPos;

import java.awt.event.MouseEvent;
import java.util.HashSet;

public class ClickableRegister implements Deletable {
    private final HashSet<Clickable> remove = new HashSet<>(), add = new HashSet<>();
    private final HashSet<Clickable> elements = new HashSet<>();

    public synchronized void mousePressed(MouseEvent e, ObjPos pos) {
        updateClickables();
        elements.forEach(c -> c.onClick(e, pos, true, c.clickBox().isPositionInside(pos)));
    }

    public synchronized void mouseReleased(MouseEvent e, ObjPos pos) {
        updateClickables();
        elements.forEach(c -> c.onClick(e, pos, false, c.clickBox().isPositionInside(pos)));
    }

    public synchronized void registerClickable(Clickable r) {
        add.add(r);
    }

    public synchronized void removeClickable(Clickable r) {
        remove.add(r);
    }

    private void updateClickables() {
        elements.addAll(add);
        remove.forEach(elements::remove);
        add.clear();
        remove.clear();
    }

    @Override
    public void delete() {
        elements.clear();
        remove.clear();
        add.clear();
    }
}
