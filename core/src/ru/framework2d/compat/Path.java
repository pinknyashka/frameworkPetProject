package ru.framework2d.compat;

import com.badlogic.gdx.utils.FloatArray;

/** Replacement for android.graphics.Path (polyline/polygon builder). */
public class Path {

    public enum Direction {
        CW, CCW
    }

    public enum FillType {
        WINDING, EVEN_ODD
    }

    private final FloatArray points = new FloatArray();
    private FillType fillType = FillType.WINDING;

    public Path() {
    }

    public Path(Path copy) {
        this.points.addAll(copy.points);
        this.fillType = copy.fillType;
    }

    public void reset() {
        points.clear();
    }

    public void rewind() {
        reset();
    }

    public void moveTo(float x, float y) {
        points.add(x, y);
    }

    public void lineTo(float x, float y) {
        points.add(x, y);
    }

    public void rLineTo(float dx, float dy) {
        if (points.size >= 2) {
            points.add(points.peek() + dx, points.get(points.size - 2) + dy);
        } else {
            points.add(dx, dy);
        }
    }

    public void close() {
        if (points.size >= 2) {
            points.add(points.get(0), points.get(1));
        }
    }

    public void addRect(float left, float top, float right, float bottom) {
        moveTo(left, top);
        lineTo(right, top);
        lineTo(right, bottom);
        lineTo(left, bottom);
        close();
    }

    public void addRect(RectF r) {
        addRect(r.left, r.top, r.right, r.bottom);
    }

    public void addRect(RectF r, Direction dir) {
        addRect(r.left, r.top, r.right, r.bottom);
    }

    public void addPath(Path other) {
        points.addAll(other.points);
    }

    public void addCircle(float cx, float cy, float radius, Direction dir) {
        int seg = 24;
        for (int i = 0; i <= seg; i++) {
            double a = 2 * Math.PI * i / seg;
            float x = cx + (float) Math.cos(a) * radius;
            float y = cy + (float) Math.sin(a) * radius;
            if (i == 0) {
                moveTo(x, y);
            } else {
                lineTo(x, y);
            }
        }
        close();
    }

    public void transform(Matrix m) {
        for (int i = 0; i + 1 < points.size; i += 2) {
            float x = points.get(i);
            float y = points.get(i + 1);
            points.set(i, m.a * x + m.c * y + m.tx);
            points.set(i + 1, m.b * x + m.d * y + m.ty);
        }
    }

    public void setFillType(FillType type) {
        this.fillType = type;
    }

    public FillType getFillType() {
        return fillType;
    }

    public int getNumPoints() {
        return points.size / 2;
    }

    public float pointX(int index) {
        return points.get(index * 2);
    }

    public float pointY(int index) {
        return points.get(index * 2 + 1);
    }

    public FloatArray getPoints() {
        return points;
    }
}
