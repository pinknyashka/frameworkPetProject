package ru.framework2d.compat;

/** Replacement for android.graphics.Paint. Holds draw settings; applied by Canvas. */
public class Paint {

    public static final int ANTI_ALIAS_FLAG = 0x01;

    public enum Style {
        FILL, STROKE, FILL_AND_STROKE
    }

    private int color = Color.BLACK;
    private int alpha = 255;
    private Style style = Style.FILL;
    private float strokeWidth = 0f;
    private float textSize = 12f;
    private boolean antiAlias;
    private ColorFilter colorFilter;

    public Paint() {
    }

    public Paint(int flags) {
        if ((flags & ANTI_ALIAS_FLAG) != 0) {
            antiAlias = true;
        }
    }

    public Paint(Paint copy) {
        this.color = copy.color;
        this.alpha = copy.alpha;
        this.style = copy.style;
        this.strokeWidth = copy.strokeWidth;
        this.textSize = copy.textSize;
        this.antiAlias = copy.antiAlias;
        this.colorFilter = copy.colorFilter;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        this.alpha = Color.alpha(color);
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public void setARGB(int a, int r, int g, int b) {
        this.alpha = a;
        this.color = Color.argb(a, r, g, b);
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float size) {
        this.textSize = size;
    }

    public void setAntiAlias(boolean aa) {
        this.antiAlias = aa;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setFilterBitmap(boolean filter) {
        // libGDX texture filtering is configured on the Texture; no-op here.
    }

    public void setDither(boolean dither) {
        // no-op
    }

    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    public void setColorFilter(ColorFilter filter) {
        this.colorFilter = filter;
    }

    public float measureText(String text) {
        return text == null ? 0f : text.length() * textSize * 0.6f;
    }
}
