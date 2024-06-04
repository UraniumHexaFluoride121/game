package level.objects;

import foundation.MainPanel;
import foundation.ObjPos;
import foundation.tick.Tickable;
import physics.CollisionHandler;
import physics.CollisionObject;
import render.Renderable;
import render.renderables.RenderGameElement;

import java.awt.*;

public abstract class BlockLike implements Tickable, Renderable, CollisionObject {
    public RenderGameElement renderElement;
    public ObjPos pos;
    public CollisionHandler.CollisionObjectData collisionObjectData;

    public BlockLike(ObjPos pos) {
        this.pos = pos;
    }

    //init MUST be called after object creation
    public BlockLike init() {
        updateRenderer();
        registerTickable();
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
        removeTickable();
        MainPanel.GAME_RENDERER.register(this);
    }

    @Override
    public CollisionHandler.CollisionObjectData getCollisionData() {
        return collisionObjectData;
    }

    @Override
    public void setCollisionData(CollisionHandler.CollisionObjectData data) {
        collisionObjectData = data;
    }
}
