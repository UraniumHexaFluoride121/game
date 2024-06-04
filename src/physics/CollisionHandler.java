package physics;

import foundation.ObjPos;
import foundation.tick.TickOrder;
import foundation.tick.Tickable;

import java.util.HashSet;

public class CollisionHandler implements Tickable {
    /*
     * We divide the collisions into sections along the Y-axis. These sections are so that
     * we can only check collisions for objects within the same section, thus avoiding collision
     * checks for far away objects that are obviously not colliding. The array has a fixed number of
     * these sections which are created based on the max level height and sections size. Enough sections
     * will be created to fill the level. We also have a number of buffer sections, which are there to
     * allow objects to be above or below level without crashing the game.
     *
     * There are two sets of objects we keep track of, one with all collision objects and one
     * with just the dynamic objects. This is to optimize the collision checks by only testing
     * the dynamic objects against all the other objects, instead of testing all objects against
     * all other objects. We know that the vast majority of objects are going to be static, and there's
     * no need to test them against each other.
     */
    private final HashSet<CollisionObject>[] collisionObjects, dynamicObjects;

    //World border collision objects are always tested against each dynamic object,
    //no matter which section it happens to be in
    private final HashSet<CollisionObject> worldBorderCollisionObjects = new HashSet<>();

    //A set containing all dynamic objects. Used to iterate through all dynamics when
    //refreshing their position in the sectioned set to avoid ConcurrentModificationException
    private final HashSet<CollisionObject> dynamicObjectSet = new HashSet<>();
    private final int maxHeight, sectionSize, bufferSections, sectionCount;

    private boolean deleted = false;

    public CollisionHandler(int maxHeight, int sectionSize, int bufferSections) {
        this.maxHeight = maxHeight;
        this.sectionSize = sectionSize;
        this.bufferSections = bufferSections;
        sectionCount = ((int) Math.ceil((double) maxHeight / sectionSize)) + 2 * bufferSections;
        collisionObjects = new HashSet[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            collisionObjects[i] = new HashSet<>();
        }
        dynamicObjects = new HashSet[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            dynamicObjects[i] = new HashSet<>();
        }
        //World floor
        worldBorderCollisionObjects.add(new WorldBorderObject(new StaticHitBox(
                0, 10, 0, 30, new ObjPos())
        ));
        //Left wall
        worldBorderCollisionObjects.add(new WorldBorderObject(new StaticHitBox(
                maxHeight + sectionSize * bufferSections, 10, 10, 0, new ObjPos())
        ));
        //Right wall
        worldBorderCollisionObjects.add(new WorldBorderObject(new StaticHitBox(
                maxHeight + sectionSize * bufferSections, 10, 0, 10, new ObjPos(30))
        ));
        registerTickable();
    }

    public void register(CollisionObject... objects) {
        for (CollisionObject o : objects) {
            CollisionObjectData data = generateData(o);
            o.setCollisionData(data);
            if (o.getCollisionType() == CollisionType.DYNAMIC) {
                dynamicObjects[data.bottomSection].add(o);
                dynamicObjects[data.topSection].add(o);
                dynamicObjectSet.add(o);
            }
            collisionObjects[data.bottomSection].add(o);
            collisionObjects[data.topSection].add(o);
        }
    }

    public void remove(CollisionObject o) {
        CollisionObjectData data = o.getCollisionData();
        o.setCollisionData(null);
        if (o.getCollisionType() == CollisionType.DYNAMIC) {
            dynamicObjects[data.bottomSection].remove(o);
            dynamicObjects[data.topSection].remove(o);
            dynamicObjectSet.remove(o);
        }
        collisionObjects[data.bottomSection].remove(o);
        collisionObjects[data.topSection].remove(o);
    }

    private CollisionObjectData generateData(CollisionObject o) {
        HitBox hitBox = o.getHitBox();
        return new CollisionObjectData(
                yPosToSection(hitBox.getBottom()),
                yPosToSection(hitBox.getTop())
        );
    }

    private int yPosToSection(float y) {
        return ((int) (y / sectionSize)) + bufferSections;
    }

    private void clearCollidedWith() {
        for (int i = 0; i < sectionCount; i++) {
            collisionObjects[i].forEach(o -> o.getCollisionData().collidedWith.clear());
            dynamicObjects[i].forEach(o -> o.getCollisionData().collidedWith.clear());
        }
    }

    @Override
    public void tick(float deltaTime) {
        if (deleted)
            return;

        dynamicObjectSet.forEach(o -> {
            CollisionObjectData newData = generateData(o);
            CollisionObjectData oldData = o.getCollisionData();
            if (!newData.equals(oldData)) {
                dynamicObjects[oldData.bottomSection].remove(o);
                dynamicObjects[oldData.topSection].remove(o);
                dynamicObjects[newData.bottomSection].add(o);
                dynamicObjects[newData.topSection].add(o);
            }
        });

        for (int i = 0; i < sectionCount; i++) {
            HashSet<CollisionObject> objects = collisionObjects[i];
            HashSet<CollisionObject> dynamics = dynamicObjects[i];
            dynamics.forEach(dynamic -> {
                if (!dynamic.hasCollision())
                    return;
                HitBox dynamicBox = dynamic.getHitBox();
                objects.forEach(otherObj -> {
                    if (dynamic == otherObj)
                        return;
                    if (!otherObj.hasCollision())
                        return;
                    if (dynamic.getCollisionData().collidedWith.contains(otherObj))
                        return;

                    HitBox otherBox = otherObj.getHitBox();
                    if (dynamicBox.isColliding(otherBox)) {
                        dynamic.onCollision(otherObj);
                        dynamic.getCollisionData().collidedWith.add(otherObj);
                        otherObj.getCollisionData().collidedWith.add(dynamic);
                    }
                });
                if (dynamic.hasWorldBorderCollision()) {
                    worldBorderCollisionObjects.forEach(otherObj -> {
                        HitBox otherBox = otherObj.getHitBox();
                        if (dynamicBox.isColliding(otherBox)) {
                            dynamic.onCollision(otherObj);
                            dynamic.getCollisionData().collidedWith.add(otherObj);
                            otherObj.getCollisionData().collidedWith.add(dynamic);
                        }
                    });
                }
            });
        }
        clearCollidedWith();
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.COLLISION_CHECK;
    }

    @Override
    public void delete() {
        //It's possible for this class to receive a tick after being deleted
        //due to deletion not immediately removing it from the tickables list.
        //We mark ourselves as deleted, so we know whether to cancel a tick if
        //this happens.
        deleted = true;
        removeTickable();

        for (int i = 0; i < sectionCount; i++) {
            collisionObjects[i].clear();
            dynamicObjects[i].clear();
        }
    }


    public static class CollisionObjectData {
        //The section that the top and bottom which the HitBox is in
        //Usually these will be the same, unless the HitBox happens to be on the
        //boundary between two sections
        public final int topSection, bottomSection;

        //For each tick, this stores the objects that have already been collided with.
        //This list is cleared after each tick, and is there to avoid multiple collisions
        //being done between two objects within a single tick.
        public HashSet<CollisionObject> collidedWith = new HashSet<>();

        public CollisionObjectData(int bottomSection, int topSection) {
            this.bottomSection = bottomSection;
            this.topSection = topSection;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CollisionObjectData data) {
                return topSection == data.topSection && bottomSection == data.bottomSection;
            }
            return false;
        }
    }
}
