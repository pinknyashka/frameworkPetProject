package ru.framework2d.compat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

/** Replacement for android.graphics.BitmapFactory (file-based decode only). */
public final class BitmapFactory {

    private BitmapFactory() {
    }

    public static Bitmap decodeFile(String path) {
        try {
            Texture texture = new Texture(Gdx.files.absolute(path));
            return new Bitmap(texture);
        } catch (Exception e) {
            return null;
        }
    }
}
