package ru.framework2d.compat;

/** Replacement for android.graphics.RectF (float rectangle). */
public class RectF {

    public float left;
    public float top;
    public float right;
    public float bottom;

    public RectF() {
    }

    public RectF(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public RectF(Rect r) {
        this.left = r.left;
        this.top = r.top;
        this.right = r.right;
        this.bottom = r.bottom;
    }

    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public float width() {
        return right - left;
    }

    public float height() {
        return bottom - top;
    }

    /** Transform this rectangle by an affine matrix (result is the axis-aligned bounds). */
    public void transform(Matrix m) {
        float[][] corners = {
                { left, top }, { right, top }, { right, bottom }, { left, bottom }
        };
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] p : corners) {
            float x = m.a * p[0] + m.c * p[1] + m.tx;
            float y = m.b * p[0] + m.d * p[1] + m.ty;
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        left = minX;
        top = minY;
        right = maxX;
        bottom = maxY;
    }
}
