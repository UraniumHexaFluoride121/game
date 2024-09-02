package level.objects;

import foundation.Deletable;
import foundation.MainPanel;
import foundation.ObjPos;
import foundation.tick.RegisteredTickable;
import level.ObjectLayer;
import physics.*;
import render.OrderedRenderable;
import render.RenderOrder;
import render.renderables.RenderGameElement;

import java.awt.*;

public abstract class BlockLike implements RegisteredTickable, OrderedRenderable, CollisionObject {
    public HitBox hitBox;
    public RenderGameElement renderElement;
    public ObjPos pos;
    public CollisionHandler.CollisionObjectData collisionObjectData;

    public BlockLike(ObjPos pos) {
        this.pos = pos;
    }

    //init MUST be called after object creation
    public BlockLike init(RenderGameElement renderElement) {
        this.renderElement = renderElement;
        registerTickable();
        MainPanel.GAME_RENDERER.register(this);
        return this;
    }

    public void createHitBox(float hitBoxUp, float hitBoxDown, float hitBoxLeft, float hitBoxRight) {
        hitBox = switch (getCollisionType()) {
            case STATIC -> new StaticHitBox(hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, pos);
            case MOVABLE, DYNAMIC -> new DynamicHitBox(hitBoxUp, hitBoxDown, hitBoxLeft, hitBoxRight, this::getPos);
        };
    }

    public ObjPos getPos() {
        return pos;
    }

    public abstract ObjectLayer getLayer();

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
        renderElement = null;
        removeTickable();
        MainPanel.GAME_RENDERER.remove(this);
        if (hitBox instanceof Deletable d)
            d.delete();
    }

    @Override
    public CollisionHandler.CollisionObjectData getCollisionData() {
        return collisionObjectData;
    }

    @Override
    public void setCollisionData(CollisionHandler.CollisionObjectData data) {
        collisionObjectData = data;
    }

    @Override
    public RenderOrder getRenderOrder() {
        return renderElement.getRenderOrder();
    }
}
