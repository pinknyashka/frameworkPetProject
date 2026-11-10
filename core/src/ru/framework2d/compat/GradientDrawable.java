package ru.framework2d.compat;

/**
 * Replacement for android.graphics.drawable.GradientDrawable.
 * Gradient fill is approximate during migration; refined after the port.
 */
public class GradientDrawable {

    // Shape constants (android.graphics.drawable.GradientDrawable).
    public static final int RECTANGLE = 0;
    public static final int OVAL = 1;
    public static final int LINE = 2;
    public static final int RING = 3;

    // Gradient types.
    public static final int LINEAR_GRADIENT = 0;
    public static final int RADIAL_GRADIENT = 1;
    public static final int SWEEP_GRADIENT = 2;

    public enum Orientation {
        TOP_BOTTOM, TR_BL, RIGHT_LEFT, BR_TL, BOTTOM_TOP, TL_BR, LEFT_RIGHT, BL_TR
    }

    private int shape = RECTANGLE;
    private int[] colors;
    private int gradientType = LINEAR_GRADIENT;
    private float gradientRadius;
    private float gradientCenterX = 0.5f;
    private float gradientCenterY = 0.5f;
    private Orientation orientation = Orientation.TOP_BOTTOM;
    private int width;
    private int height;
    private int alpha = 255;

    public GradientDrawable() {
    }

    public GradientDrawable(Orientation orientation, int[] colors) {
        this.orientation = orientation;
        this.colors = colors;
    }

    public GradientDrawable mutate() {
        return this;
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public int getShape() {
        return shape;
    }

    public void setColors(int[] colors) {
        this.colors = colors;
    }

    public void setColor(int color) {
        this.colors = new int[] { color };
    }

    public int[] getColors() {
        return colors;
    }

    public void setGradientType(int type) {
        this.gradientType = type;
    }

    public int getGradientType() {
        return gradientType;
    }

    public void setGradientRadius(float radius) {
        this.gradientRadius = radius;
    }

    public float getGradientRadius() {
        return gradientRadius;
    }

    public void setGradientCenter(float x, float y) {
        this.gradientCenterX = x;
        this.gradientCenterY = y;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setCornerRadius(float radius) {
        // no-op during migration
    }

    /** Approximate: draw the shape filled with the first gradient color. */
    public void draw(Canvas canvas) {
        if (colors == null || colors.length == 0) {
            return;
        }
        Paint fill = new Paint();
        fill.setColor(colors[0]);
        fill.setAlpha(colors[0] >>> 24);
        canvas.fillShape(this, fill);
    }
}
