package foundation.math;

import foundation.Deletable;
import foundation.Main;
import foundation.MainPanel;
import level.procedural.Layout;
import physics.StaticHitBox;
import render.BoundedRenderable;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameCircle;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class BezierCurve3 implements BoundedRenderable, Deletable {
    private final float px1, py1, px2, py2, px3, py3;
    private final float bxMin, byMin, bxMax, byMax;
    private final StaticHitBox box;
    private final float debugBoundTop, debugBoundBottom;
    private Renderable debugRenderable = null;
    private final Renderable[] renderPoints;

    public BezierCurve3(ObjPos p1, ObjPos p2, ObjPos p3) {
        this(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
    }

    public BezierCurve3(float px1, float py1, float px2, float py2, float px3, float py3, float addend) {
        this(px1 + addend, py1 + addend, px2 + addend, py2 + addend, px3 + addend, py3 + addend);
    }

    public BezierCurve3(float px1, float py1, float px2, float py2, float px3, float py3) {
        this.px1 = px1;
        this.py1 = py1;
        this.px2 = px2;
        this.py2 = py2;
        this.px3 = px3;
        this.py3 = py3;
        bxMin = calculateBound(px1, px2, px3, true);
        bxMax = calculateBound(px1, px2, px3, false);
        byMin = calculateBound(py1, py2, py3, true);
        byMax = calculateBound(py1, py2, py3, false);

        box = new StaticHitBox(byMax, byMin, bxMin, bxMax);

        debugBoundTop = Math.max(Math.max(py1, py2), py3);
        debugBoundBottom = Math.min(Math.min(py1, py2), py3);

        if (Layout.DEBUG_RENDER) {
            renderPoints = new Renderable[40 + 3];
            for (int i = 0; i < 40; i++) {
                float t = i / 39f;
                ObjPos pos = sampleCurve(t);
                renderPoints[i] = new RenderGameCircle(RenderOrder.BLOCK, Color.GREEN, 0.15f, () -> pos);
            }
            renderPoints[40] = new RenderGameCircle(RenderOrder.BLOCK, Color.RED, 0.3f, () -> new ObjPos(px1, py1));
            renderPoints[41] = new RenderGameCircle(RenderOrder.BLOCK, Color.RED, 0.3f, () -> new ObjPos(px2, py2));
            renderPoints[42] = new RenderGameCircle(RenderOrder.BLOCK, Color.RED, 0.3f, () -> new ObjPos(px3, py3));
            MainPanel.GAME_RENDERER.register(this);
        } else
            renderPoints = new Renderable[0];
    }

    public ObjPos sampleCurve(float t) {
        return new ObjPos(
                (1 - t) * (1 - t) * px1 + 2 * t * (1 - t) * px2 + t * t * px3,
                (1 - t) * (1 - t) * py1 + 2 * t * (1 - t) * py2 + t * t * py3
        );
    }

    private float calculateBound(float p1, float p2, float p3, boolean isMin) {
        float t = (p2 - p1) / -(p1 - 2 * p2 + p3);
        if (p1 - 2 * p2 + p3 == 0 || t < 0 || t > 1)
            return isMin ? Math.min(p1, p3) : Math.max(p1, p3);
        float ext = (1 - t) * (1 - t) * p1 + 2 * t * (1 - t) * p2 + t * t * p3;
        return isMin ? Math.min(ext, Math.min(p1, p3)) : Math.max(ext, Math.max(p1, p3));
    }

    public float closestPointOnCurve(ObjPos pos) {
        float dx = px1 - 2 * px2 + px3, dy = py1 - 2 * py2 + py3;
        float ex = px2 - px1, ey = py2 - py1;
        float fx = px1 - pos.x, fy = py1 - pos.y;
        float a = 4 * (dx * dx + dy * dy), b = 12 * (dx * ex + dy * ey), c = 4 * (dx * fx + 2 * ex * ex + dy * fy + 2 * ey * ey), d = 4 * (ex * fx + ey * fy);
        boolean zeroLength = a == 0 && b == 0 && c == 0 && d == 0;
        float distanceToClosest = pos.distance(sampleCurve(0));
        float closestPoint = 0;
        if (zeroLength) {
            return 0;
        } else {
            float[] solutions = MathHelper.solveCubic(a, b, c, d);
            for (float solution : solutions) {
                if (solution < 0 || solution > 1)
                    continue;
                float dist = pos.distance(sampleCurve(solution));
                if (dist < distanceToClosest) {
                    distanceToClosest = dist;
                    closestPoint = solution;
                }
            }
        }
        if (pos.distance(sampleCurve(1)) < distanceToClosest)
            return 1;
        return closestPoint;
    }

    public float distanceToCurve(ObjPos pos) {
        float dx = px1 - 2 * px2 + px3, dy = py1 - 2 * py2 + py3;
        float ex = px2 - px1, ey = py2 - py1;
        float fx = px1 - pos.x, fy = py1 - pos.y;
        float a = 4 * (dx * dx + dy * dy), b = 12 * (dx * ex + dy * ey), c = 4 * (dx * fx + 2 * ex * ex + dy * fy + 2 * ey * ey), d = 4 * (ex * fx + ey * fy);
        boolean zeroLength = a == 0 && b == 0 && c == 0 && d == 0;
        float distanceToClosest = pos.distance(sampleCurve(0));
        if (zeroLength) {
            return pos.distance(new ObjPos(px1, py1));
        } else {
            float[] solutions = MathHelper.solveCubic(a, b, c, d);
            for (float solution : solutions) {
                if (solution < 0 || solution > 1)
                    break;
                float dist = pos.distance(sampleCurve(solution));
                if (dist < distanceToClosest) {
                    distanceToClosest = dist;
                }
            }
        }
        return Math.min(pos.distance(sampleCurve(1)), distanceToClosest);
    }

    public float distanceToCurve(float x, float y) {
        return distanceToCurve(new ObjPos(x, y));
    }

    public void forEachBlockNearCurve(float dist, BiConsumer<ObjPos, Float> function) {
        int fromX = Math.max(0, Math.round(bxMin - dist - 0.5f));
        int toX = Math.min(Main.BLOCKS_X - 1, Math.round(bxMax + dist - 0.5f));
        int fromY = Math.max(0, Math.round(byMin - dist - 0.5f));
        int toY = Math.min(Main.BLOCKS_X - 1, Math.round(byMax + dist - 0.5f));
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                float distanceToCurve = distanceToCurve(new ObjPos(x + 0.5f, y + 0.5f));
                if (distanceToCurve <= dist)
                    function.accept(new ObjPos(x, y), distanceToCurve);
            }
        }
    }

    //Filter by (t, dist) predicate
    public void forEachBlockNearCurve(float dist, BiPredicate<Float, Float> filter, BiConsumer<ObjPos, Float> function) {
        int fromX = Math.max(0, Math.round(bxMin - dist - 0.5f));
        int toX = Math.min(Main.BLOCKS_X - 1, Math.round(bxMax + dist - 0.5f));
        int fromY = Math.max(0, Math.round(byMin - dist - 0.5f));
        int toY = Math.round(byMax + dist - 0.5f);
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                ObjPos pos = new ObjPos(x + 0.5f, y + 0.5f); //We sample the middle of the block
                float closestPoint = closestPointOnCurve(pos);
                float distanceToCurve = pos.distance(sampleCurve(closestPoint));
                if (!filter.test(closestPoint, distanceToCurve))
                    continue;
                if (distanceToCurve <= dist)
                    function.accept(new ObjPos(x, y), distanceToCurve);
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        for (Renderable renderPoint : renderPoints) {
            renderPoint.render(g);
        }
        if (debugRenderable != null)
            debugRenderable.render(g);
    }

    public void setDebugPoint(float t) {
        ObjPos pos = sampleCurve(t);
        debugRenderable = new RenderGameCircle(RenderOrder.BLOCK, Color.RED, 0.3f, () -> pos);
    }

    public StaticHitBox getBox() {
        return box;
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.DEBUG;
    }

    @Override
    public float getTopRenderBound() {
        return debugBoundTop;
    }

    @Override
    public float getBottomRenderBound() {
        return debugBoundBottom;
    }

    public float getBoundTop() {
        return byMax;
    }

    public float getBoundBottom() {
        return byMin;
    }

    public float getBoundLeft() {
        return bxMin;
    }

    public float getBoundRight() {
        return bxMax;
    }

    @Override
    public void delete() {
        if (Layout.DEBUG_RENDER) {
            MainPanel.GAME_RENDERER.remove(this);
        }
    }

    @Override
    public String toString() {
        return "[points: (" + px1 + ", " + py1 + "), (" + px2 + ", " + py2 + "), (" + px3 + ", " + py3 + "), bounds: x: " + bxMin + " -> " + bxMax + ", y: " + byMin + " -> " + byMax + "]";
    }
}
