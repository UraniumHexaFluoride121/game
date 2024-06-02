package level.objects;

import foundation.Main;
import foundation.MainPanel;
import foundation.ObjPos;
import foundation.tick.Tickable;
import render.Renderable;
import render.renderables.RenderGameElement;

import java.awt.*;

public abstract class BlockLike implements Tickable, Renderable {
    public RenderGameElement renderElement;
    public ObjPos pos;

    public BlockLike(ObjPos pos) {
        this.pos = pos;
    }

    //init MUST be called after object creation
    public BlockLike init() {
        updateRenderer();
        Main.TICK.register(this);
        MainPanel.GAME_RENDERER.register(this);
        return this;
    }

    public void updateRenderer() {
        renderElement = createRefreshedRenderer();
    }

    public abstract RenderGameElement createRefreshedRenderer();

    public ObjPos getPos() {
        return pos;
    }

    @Override
    public void render(Graphics2D g) {
        renderElement.render(g);
    }

    @Override
    public void tick(float deltaTime) {
        renderElement.tick(deltaTime);
    }

    @Override
    public void delete() {
        renderElement.delete();
        Main.TICK.remove(this);
        MainPanel.GAME_RENDERER.register(this);
    }
}
