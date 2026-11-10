package ru.framework2d.compat;

/** Replacement for android.graphics.LightingColorFilter: result = src * multiply + add. */
public class LightingColorFilter extends ColorFilter {

    public int colorMultiply;
    public int colorAdd;

    public LightingColorFilter(int colorMultiply, int colorAdd) {
        this.colorMultiply = colorMultiply;
        this.colorAdd = colorAdd;
    }

    public int getMultiply() {
        return colorMultiply;
    }

    public int getAdd() {
        return colorAdd;
    }

    @Override
    public int filter(int color) {
        // This filter is a lighting/color tint: it must change RGB only. Alpha is
        // controlled by Paint and the graphics transparency chain; otherwise a filter
        // built with Color.BLACK as the additive color would force shadows opaque.
        int a = Color.alpha(color);
        int r = apply(Color.red(color), Color.red(colorMultiply), Color.red(colorAdd));
        int g = apply(Color.green(color), Color.green(colorMultiply), Color.green(colorAdd));
        int b = apply(Color.blue(color), Color.blue(colorMultiply), Color.blue(colorAdd));
        return Color.argb(a, r, g, b);
    }

    private static int apply(int src, int mul, int add) {
        int v = (src * mul) / 255 + add;
        if (v < 0) {
            v = 0;
        } else if (v > 255) {
            v = 255;
        }
        return v;
    }
}
