package level.objects;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import render.RenderOrder;
import render.renderables.RenderGameElement;
import render.renderables.RenderGameSquare;

import java.awt.*;

public class DebugSquare extends BlockLike {
    private final Color color;

    public DebugSquare(ObjPos pos, Color color) {
        super(pos);
        this.color = color;
    }

    @Override
    public RenderGameElement createRefreshedRenderer() {
        return new RenderGameSquare(RenderOrder.BLOCK, color, 1, 0, 0, 1, this::getPos);
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.ANIMATIONS_ONLY;
    }
}
