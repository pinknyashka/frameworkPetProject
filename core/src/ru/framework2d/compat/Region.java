package ru.framework2d.compat;

/** Replacement for android.graphics.Region (only the Op enum is referenced). */
public class Region {

    public enum Op {
        DIFFERENCE, INTERSECT, REPLACE, UNION, XOR, REVERSE_DIFFERENCE
    }
}
