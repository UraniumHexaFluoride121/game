package level.objects;

import foundation.Deletable;
import foundation.math.ObjPos;
import foundation.math.RandomType;
import foundation.tick.RegisteredTickable;
import level.Level;
import level.ObjectLayer;
import physics.*;
import render.BoundedRenderable;
import render.RenderOrder;
import render.event.RenderBlockUpdate;
import render.event.RenderEvent;
import render.renderables.RenderGameElement;

import java.awt.*;

public abstract class BlockLike implements RegisteredTickable, BoundedRenderable, CollisionObject {
    public HitBox hitBox;
    public RenderGameElement renderElement;
    public ObjPos pos;
    public float friction = 1, bounciness = 0;
    public CollisionHandler.CollisionObjectData collisionObjectData;
    public final String name;
    public int zOrder;

    public Level level;

    public final int randomSeed;

    public BlockLike(ObjPos pos, String name, Level level) {
        this.pos = pos;
        this.name = name;
        this.level = level;
        randomSeed = level.randomHandler.generateNewRandomSeed(RandomType.TEXTURE);
    }

    //init MUST be called after object creation
    public BlockLike init(RenderGameElement renderElement) {
        this.renderElement = renderElement;
        if (blockRequiresTick())
            registerTickable();
        zOrder = level.gameRenderer.getNextZOrder();
        level.gameRenderer.register(this);
        return this;
    }

    public boolean blockRequiresTick() {
        return true;
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

    public synchronized void renderUpdateBlock(RenderEvent type) {
        renderElement.onEvent(new RenderBlockUpdate(type, this));
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
        level.gameRenderer.remove(this);
        if (hitBox instanceof Deletable d)
            d.delete();
        level = null;
    }

    @Override
    public float getFriction() {
        return friction;
    }

    @Override
    public float getBounciness() {
        return bounciness;
    }

    public void setFriction(float f) {
        friction = f;
    }

    public void setBounciness(float b) {
        bounciness = b;
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

    @Override
    public float getTopRenderBound() {
        return hitBox.getTop() + 1;
    }

    @Override
    public float getBottomRenderBound() {
        return hitBox.getBottom() - 1;
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }
}
