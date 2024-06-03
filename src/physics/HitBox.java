package physics;

public interface HitBox {
    //Positions are absolute game coordinates
    float getTop();
    float getBottom();
    float getLeft();
    float getRight();

    default boolean isColliding(HitBox box) {
        return (getRight() > box.getLeft() || box.getRight() > getLeft()) &&
                (getTop() > box.getBottom() || box.getTop() > getBottom());
    }
}
