package render;

public interface TickedRenderable extends Renderable {
    boolean requiresTick();
}
