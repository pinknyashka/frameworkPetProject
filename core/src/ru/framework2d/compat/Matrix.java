package ru.framework2d.compat;

/** Replacement for android.graphics.Matrix (2D affine). Used by shadow layers. */
public class Matrix {

    public float a = 1, b = 0, c = 0, d = 1, tx = 0, ty = 0;

    public void setRotate(float degrees) {
        double rad = Math.toRadians(degrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        a = cos;
        b = sin;
        c = -sin;
        d = cos;
        tx = 0;
        ty = 0;
    }

    public void setTranslate(float dx, float dy) {
        a = 1;
        b = 0;
        c = 0;
        d = 1;
        tx = dx;
        ty = dy;
    }

    public void postTranslate(float dx, float dy) {
        tx = a * dx + c * dy + tx;
        ty = b * dx + d * dy + ty;
    }

    public void preTranslate(float dx, float dy) {
        tx += dx;
        ty += dy;
    }

    public void postRotate(float degrees) {
        double rad = Math.toRadians(degrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float na = a * cos + c * sin;
        float nb = b * cos + d * sin;
        float nc = -a * sin + c * cos;
        float nd = -b * sin + d * cos;
        a = na;
        b = nb;
        c = nc;
        d = nd;
    }
}
