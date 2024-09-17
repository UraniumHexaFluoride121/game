package render.event;

import level.objects.BlockLike;

public class RenderBlockUpdate extends RenderEvent {
    public final RenderEvent type;
    public final BlockLike block;

    public RenderBlockUpdate(RenderEvent type, BlockLike block) {
        super("blockUpdate", false);
        this.type = type;
        this.block = block;
    }
}
