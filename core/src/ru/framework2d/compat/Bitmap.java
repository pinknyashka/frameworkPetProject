package ru.framework2d.compat;

import com.badlogic.gdx.graphics.Texture;

/** Replacement for android.graphics.Bitmap, backed by a libGDX Texture. */
public class Bitmap {

    public Texture texture;
    private int width;
    private int height;
    private boolean recycled;

    public Bitmap(Texture texture) {
        this.texture = texture;
        if (texture != null) {
            this.width = texture.getWidth();
            this.height = texture.getHeight();
        }
    }

    public Bitmap(Texture texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isRecycled() {
        return recycled;
    }

    public void recycle() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        recycled = true;
    }
}
