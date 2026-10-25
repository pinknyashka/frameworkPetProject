package ru.framework2d.graphics;

import com.badlogic.gdx.math.Matrix4;

/**
 * Optional graphics bridge for renderers that own their own SpriteBatch and
 * shader pass instead of drawing through {@link ru.framework2d.compat.Canvas}.
 */
public interface RenderPeriphery {

    void renderFrame(Matrix4 baseProjection);

    void resize(int width, int height, Matrix4 baseProjection);

    void dispose();
}
