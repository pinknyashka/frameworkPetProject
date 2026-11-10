package ru.framework2d.compat;

/** Replacement for android.graphics.Color (constants + helpers). */
public final class Color {

    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    private Color() {
    }

    public static int alpha(int color) {
        return (color >>> 24);
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int parseColor(String colorString) {
        String s = colorString.trim();
        if ("transparent".equalsIgnoreCase(s)) {
            return TRANSPARENT;
        }
        int named = colorName(s);
        if (named != 0) {
            return named;
        }
        if (s.charAt(0) == '#') {
            s = s.substring(1);
        }
        if (s.length() == 6) {
            s = "FF" + s;
        }
        return (int) (0xFFFFFFFFL & Long.parseLong(s, 16));
    }

    /** android.graphics.Color accepts a fixed set of color names; maps use them directly. */
    private static int colorName(String s) {
        switch (s.toLowerCase(java.util.Locale.US)) {
            case "black": return BLACK;
            case "darkgray": return DKGRAY;
            case "gray": case "grey": return GRAY;
            case "lightgray": case "lightgrey": return LTGRAY;
            case "white": return WHITE;
            case "red": return RED;
            case "green": return GREEN;
            case "blue": return BLUE;
            case "yellow": return YELLOW;
            case "cyan": return CYAN;
            case "magenta": return MAGENTA;
            case "transparent": return TRANSPARENT;
            default: return 0;
        }
    }
}
