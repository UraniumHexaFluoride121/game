package render.ui;

import foundation.math.ObjPos;
import physics.HitBox;

import java.awt.event.MouseEvent;

public interface Clickable {
    HitBox clickBox();
    void onClick(MouseEvent e, ObjPos pos, boolean pressed, boolean wasClicked);
}
