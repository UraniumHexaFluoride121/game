package level.objects;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import loader.AssetManager;
import loader.ResourceLocation;
import physics.CollisionBehaviour;
import physics.CollisionType;
import physics.DynamicHitBox;
import physics.HitBox;
import render.RenderOrder;
import render.renderables.RenderGameElement;
import render.renderables.RenderTexture;

import java.awt.*;

public class DebugSquare extends BlockLike {
    private final Color color;
    private final DynamicHitBox hitBox;

    public DebugSquare(ObjPos pos, Color color) {
        super(pos);
        this.color = color;
        hitBox = new DynamicHitBox(1, 0, 0, 1, this::getPos);
    }

    @Override
    public RenderGameElement createRefreshedRenderer() {
        return new RenderTexture(RenderOrder.BLOCK, this::getPos, AssetManager.getAnimatedTexture(new ResourceLocation("test.json")));
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.ANIMATIONS_ONLY;
    }

    @Override
    public HitBox getHitBox() {
        return hitBox;
    }

    @Override
    public boolean hasCollision() {
        return true;
    }

    @Override
    public CollisionType getCollisionType() {
        return CollisionType.STATIC;
    }

    @Override
    public CollisionBehaviour getCollisionBehaviour() {
        return CollisionBehaviour.IMMOVABLE;
    }
}
