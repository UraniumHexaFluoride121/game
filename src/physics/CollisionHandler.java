package physics;

import foundation.Main;
import foundation.math.ObjPos;
import foundation.tick.RegisteredTickable;
import foundation.tick.TickOrder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollisionHandler implements RegisteredTickable {
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
    private final Set<CollisionObject>[] collisionObjects, dynamicObjects;
    private Set<CollisionObject>[] proceduralObjects;
    private final HashMap<CollisionObject, CollisionObjectData> proceduralObjectData = new HashMap<>();

    //World border collision objects are always tested against each dynamic object,
    //no matter which section it happens to be in
    private final Set<CollisionObject> worldBorderCollisionObjects = ConcurrentHashMap.newKeySet();

    //A set containing all movable objects. Used to iterate through all movable objects when
    //refreshing their position in the sectioned set to avoid ConcurrentModificationException
    private final Set<CollisionObject> movableObjectSet = ConcurrentHashMap.newKeySet();

    public final Set<CollisionObject> qAdd = ConcurrentHashMap.newKeySet(), qRemove = ConcurrentHashMap.newKeySet();

    private final int sectionSize, bufferSections, sectionCount;

    private boolean deleted = false;

    public CollisionHandler(int maxHeight, int sectionSize, int bufferSections) {
        this.sectionSize = sectionSize;
        this.bufferSections = bufferSections;
        sectionCount = ((int) Math.ceil((double) maxHeight / sectionSize)) + 2 * bufferSections;
        collisionObjects = new Set[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            collisionObjects[i] = ConcurrentHashMap.newKeySet();
        }
        dynamicObjects = new Set[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            dynamicObjects[i] = ConcurrentHashMap.newKeySet();
        }
        proceduralObjects = new Set[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            proceduralObjects[i] = ConcurrentHashMap.newKeySet();
        }
        //World floor
        worldBorderCollisionObjects.add(new WorldBorderObject(new StaticHitBox(
                0, 30, 0, Main.BLOCKS_X, new ObjPos())
        ));
        //Left wall
        worldBorderCollisionObjects.add(new WorldBorderObject(new StaticHitBox(
                maxHeight + sectionSize * bufferSections, 10, 10, 0, new ObjPos())
        ));
        //Right wall
        worldBorderCollisionObjects.add(new WorldBorderObject(new StaticHitBox(
                maxHeight + sectionSize * bufferSections, 10, 0, 10, new ObjPos(Main.BLOCKS_X))
        ));
        registerTickable();
    }

    public synchronized void registerProcedural(CollisionObject... objects) {
        for (CollisionObject o : objects) {
            CollisionObjectData data = generateData(o);
            o.setCollisionData(data);
            proceduralObjects[data.bottomSection].add(o);
            proceduralObjects[data.topSection].add(o);
            proceduralObjectData.put(o, data);
        }
    }

    public synchronized void removeProcedural(CollisionObject... objects) {
        for (CollisionObject o : objects) {
            CollisionObjectData data = proceduralObjectData.get(o);
            proceduralObjectData.remove(o);
            proceduralObjects[data.bottomSection].remove(o);
            proceduralObjects[data.topSection].remove(o);
        }
    }

    public synchronized void clearProcedural() {
        removeProcedural(proceduralObjectData.keySet().toArray(new CollisionObject[0]));
    }

    public synchronized void register(CollisionObject... objects) {
        for (CollisionObject o : objects) {
            CollisionObjectData data = generateData(o);
            o.setCollisionData(data);
            if (o.getCollisionType().interactsDynamically) {
                dynamicObjects[data.bottomSection].add(o);
                dynamicObjects[data.topSection].add(o);
            }
            if (o.getCollisionType().requiresPositionUpdates)
                movableObjectSet.add(o);
            collisionObjects[data.bottomSection].add(o);
            collisionObjects[data.topSection].add(o);
        }
    }

    public synchronized void remove(CollisionObject... objects) {
        for (CollisionObject o : objects) {
            CollisionObjectData data = o.getCollisionData();
            o.setCollisionData(null);
            if (o.getCollisionType().interactsDynamically) {
                dynamicObjects[data.bottomSection].remove(o);
                dynamicObjects[data.topSection].remove(o);
            }
            if (o.getCollisionType().requiresPositionUpdates)
                movableObjectSet.remove(o);
            collisionObjects[data.bottomSection].remove(o);
            collisionObjects[data.topSection].remove(o);
        }
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
            dynamicObjects[i].forEach(o -> o.getCollisionData().collidedWith.clear());
        }
    }

    @Override
    public synchronized void tick(float deltaTime) {
        if (deleted)
            return;

        qAdd.forEach(this::register);
        qAdd.clear();
        qRemove.forEach(this::remove);
        qRemove.clear();

        movableObjectSet.forEach(o -> {
            CollisionObjectData newData = generateData(o);
            CollisionObjectData oldData = o.getCollisionData();
            if (!newData.equals(oldData)) {
                if (o.getCollisionType().interactsDynamically) {
                    dynamicObjects[oldData.bottomSection].remove(o);
                    dynamicObjects[oldData.topSection].remove(o);
                    dynamicObjects[newData.bottomSection].add(o);
                    dynamicObjects[newData.topSection].add(o);
                }
                collisionObjects[oldData.bottomSection].remove(o);
                collisionObjects[oldData.topSection].remove(o);
                collisionObjects[newData.bottomSection].add(o);
                collisionObjects[newData.topSection].add(o);
                o.setCollisionData(newData);
            }
            o.dynamicPreTick(deltaTime);
        });

        int loops = 0;
        while (true) {
            loops++;
            AtomicBoolean isCollision = testIsCollision(loops <= 5, loops >= 25);
            if (!isCollision.get())
                break;
            clearCollidedWith();
            if (loops > 50)
                throw new RuntimeException("Failed to solve collision");
        }
        clearCollidedWith();
        movableObjectSet.forEach(o -> o.dynamicPostTick(deltaTime));
    }

    private AtomicBoolean testIsCollision(boolean constraintsOnly, boolean alwaysSnap) {
        AtomicBoolean hasHadCollision = new AtomicBoolean(false);
        for (int i = 0; i < sectionCount; i++) {
            if (dynamicObjects[i].isEmpty())
                continue;
            Set<CollisionObject> objects = collisionObjects[i];
            Set<CollisionObject> dynamics = dynamicObjects[i];
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
                        dynamic.onCollision(otherObj, constraintsOnly, alwaysSnap);
                        dynamic.getCollisionData().collidedWith.add(otherObj);
                        if (dynamics.contains(otherObj))
                            otherObj.getCollisionData().collidedWith.add(dynamic);
                        hasHadCollision.set(true);
                    }
                });
                if (dynamic.hasWorldBorderCollision()) {
                    worldBorderCollisionObjects.forEach(otherObj -> {
                        HitBox otherBox = otherObj.getHitBox();
                        if (dynamicBox.isColliding(otherBox)) {
                            dynamic.onCollision(otherObj, constraintsOnly, alwaysSnap);
                            dynamic.getCollisionData().collidedWith.add(otherObj);
                            otherObj.getCollisionData().collidedWith.add(dynamic);
                            hasHadCollision.set(true);
                        }
                    });
                }
            });
        }
        return hasHadCollision;
    }

    public CollisionObject getObjectAt(ObjPos pos) {
        for (CollisionObject object : collisionObjects[yPosToSection(pos.y)]) {
            if (object.hasCollision() && object.getHitBox().isPositionInside(pos))
                return object;
        }
        return null;
    }

    public synchronized HashSet<CollisionObject> getBoxCollidingWith(HitBox box) {
        int topSection = yPosToSection(box.getTop());
        int bottomSection = yPosToSection(box.getBottom());

        HashSet<CollisionObject> objects = new HashSet<>();

        for (CollisionObject object : worldBorderCollisionObjects) {
            if (object.hasCollision() && object.getHitBox().isColliding(box))
                objects.add(object);
        }

        for (CollisionObject object : proceduralObjects[topSection]) {
            if (object.hasCollision() && object.getHitBox().isColliding(box))
                objects.add(object);
        }
        if (bottomSection != topSection) {
            for (CollisionObject object : proceduralObjects[bottomSection]) {
                if (object.hasCollision() && object.getHitBox().isColliding(box))
                    objects.add(object);
            }
        }
        return objects;
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.COLLISION_CHECK;
    }

    @Override
    public synchronized void delete() {
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
