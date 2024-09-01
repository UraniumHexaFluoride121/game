package level.objects;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import loader.AssetManager;
import loader.ResourceLocation;
import physics.DynamicHitBox;
import physics.HitBox;
import render.RenderOrder;
import render.renderables.RenderGameElement;
import render.renderables.RenderTexture;

import java.awt.*;

public class PhysicsBlock extends PhysicsObject {
    private final DynamicHitBox hitBox;
    public PhysicsBlock(ObjPos pos) {
        super(pos);
        hitBox = new DynamicHitBox(1, 0, 0, 1, this::getPos);
    }

    @Override
    public RenderGameElement createRefreshedRenderer() {
        return new RenderTexture(RenderOrder.BLOCK, this::getPos, AssetManager.getAnimatedTexture(new ResourceLocation("test.json")));
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.BLOCK_MOVEMENT;
    }

    @Override
    public HitBox getHitBox() {
        return hitBox;
    }

    @Override
    public boolean hasCollision() {
        return true;
    }
}
