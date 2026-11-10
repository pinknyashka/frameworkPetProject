package ru.framework2d.compat;

/** Replacement for android.graphics.ColorFilter. */
public class ColorFilter {

    /** Transform an input ARGB pixel to an output ARGB pixel. */
    public int filter(int color) {
        return color;
    }
}
