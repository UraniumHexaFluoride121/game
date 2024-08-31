package render;

import java.util.HashMap;

public class RenderEvent {
    public static final HashMap<String, RenderEvent> ALL_EVENTS = new HashMap<>();
    public static final RenderEvent
            ON_SWITCH_TO = new RenderEvent("onSwitchTo"),

            ON_BLOCK_FALLING = new RenderEvent("onBlockFalling"),
            ON_BLOCK_LAND = new RenderEvent("onBlockLand"),

            ON_PLAYER_INPUT_JUMP = new RenderEvent("onPlayerInputJump"),
            ON_PLAYER_INPUT_LEFT = new RenderEvent("onPlayerInputLeft"),
            ON_PLAYER_INPUT_RIGHT = new RenderEvent("onPlayerInputRight"),
            ON_PLAYER_INPUT_STANDING_STILL = new RenderEvent("onPlayerInputStandingStill");

    public final String s;

    RenderEvent(String s) {
        this.s = s;
        ALL_EVENTS.put(s, this);
    }

    @Override
    public String toString() {
        return s;
    }
}
