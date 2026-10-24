package ru.framework2d.compat;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.FloatArray;

/**
 * libGDX-backed replacement for android.graphics.Canvas.
 *
 * Coordinates are y-down / top-left-origin (matching Android) via the projection the
 * render bridge installs on the SpriteBatch. Geometry is emitted through one SpriteBatch;
 * the current 2D affine transform is applied via SpriteBatch.setTransform.
 *
 * Migration pass approximations: path fill = triangle fan (approx convex), gradients =
 * single-color disc, clipping = no-op. Refined after the port per "migrate first, change later".
 */
public class Canvas {

    private SpriteBatch batch;
    private BitmapFont font;
    private Texture white;

    // 2D affine: x' = a*x + c*y + tx ; y' = b*x + d*y + ty
    private float a = 1, b = 0, c = 0, d = 1, tx = 0, ty = 0;
    private final java.util.ArrayList<float[]> stack = new java.util.ArrayList<float[]>();

    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 scratchProjection = new Matrix4();
    private Matrix4 baseProjection;
    private final com.badlogic.gdx.graphics.Color scratchColor = new com.badlogic.gdx.graphics.Color();

    public Canvas() {
    }

    public void setFrame(SpriteBatch batch, Matrix4 baseProjection, BitmapFont font) {
        this.batch = batch;
        this.font = font;
        if (this.baseProjection == null) {
            this.baseProjection = new Matrix4();
        }
        this.baseProjection.set(baseProjection);
        if (white == null) {
            Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            px.setColor(1, 1, 1, 1);
            px.fill();
            white = new Texture(px);
            px.dispose();
            white.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        resetTransform();
    }

    public void resetTransform() {
        a = 1;
        b = 0;
        c = 0;
        d = 1;
        tx = 0;
        ty = 0;
        stack.clear();
    }

    // ---------------------------------------------------------------- transforms

    public int save() {
        stack.add(new float[] { a, b, c, d, tx, ty });
        return stack.size();
    }

    public void restore() {
        if (!stack.isEmpty()) {
            set(stack.remove(stack.size() - 1));
        }
    }

    public void restoreToCount(int count) {
        while (stack.size() >= count && !stack.isEmpty()) {
            set(stack.remove(stack.size() - 1));
        }
    }

    private void set(float[] m) {
        a = m[0];
        b = m[1];
        c = m[2];
        d = m[3];
        tx = m[4];
        ty = m[5];
    }

    public void translate(float dx, float dy) {
        tx = a * dx + c * dy + tx;
        ty = b * dx + d * dy + ty;
    }

    public void scale(float sx, float sy) {
        a *= sx;
        b *= sx;
        c *= sy;
        d *= sy;
    }

    public void rotate(float degrees) {
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

    private void applyTransformToBatch() {
        float[] v = transformMatrix.val;
        v[0] = a;  v[1] = b;  v[2] = 0;  v[3] = 0;
        v[4] = c;  v[5] = d;  v[6] = 0;  v[7] = 0;
        v[8] = 0;  v[9] = 0;  v[10] = 1; v[11] = 0;
        v[12] = tx; v[13] = ty; v[14] = 0; v[15] = 1;
        // Apply the affine by folding it into the projection: point -> affine -> base ortho.
        scratchProjection.set(baseProjection);
        scratchProjection.mul(transformMatrix);
        batch.setProjectionMatrix(scratchProjection);
    }

    // ---------------------------------------------------------------- clipping (no-op for now)

    public void clipPath(Path path) {
        // TODO: approximate with GL stencil after the core port.
    }

    public void clipRect(Rect r) {
    }

    public void clipRect(RectF r) {
    }

    public void clipRect(float left, float top, float right, float bottom) {
    }

    // ---------------------------------------------------------------- bitmap draws

    /**
     * libGDX reads texture regions from the top-left like Android, but its SpriteBatch maps the
     * region onto the quad in y-up order. The render bridge projects with a y-down matrix, so the
     * region is drawn with flipY to keep images the right way round.
     */
    private static final boolean FLIP_Y = true;

    public void drawBitmap(Bitmap bmp, Rect src, Rect dst, Paint paint) {
        if (bmp == null || bmp.texture == null) {
            return;
        }
        applyTransformToBatch();
        batch.setColor(toGdx(bitmapColor(paint)));
        batch.draw(bmp.texture, (float) dst.left, (float) dst.top, (float) dst.width(), (float) dst.height(),
                src.left, src.top, src.width(), src.height(), false, FLIP_Y);
    }

    public void drawBitmap(Bitmap bmp, Rect src, RectF dst, Paint paint) {
        if (bmp == null || bmp.texture == null) {
            return;
        }
        applyTransformToBatch();
        batch.setColor(toGdx(bitmapColor(paint)));
        batch.draw(bmp.texture, dst.left, dst.top, dst.right - dst.left, dst.bottom - dst.top,
                src.left, src.top, src.width(), src.height(), false, FLIP_Y);
    }

    public void drawBitmap(Bitmap bmp, float left, float top, Paint paint) {
        if (bmp == null || bmp.texture == null) {
            return;
        }
        applyTransformToBatch();
        batch.setColor(toGdx(bitmapColor(paint)));
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        batch.draw(bmp.texture, left, top, (float) width, (float) height,
                0, 0, width, height, false, FLIP_Y);
    }

    // ---------------------------------------------------------------- shape draws

    public void drawRect(RectF r, Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    public void drawRect(Rect r, Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        applyTransformToBatch();
        batch.setColor(toGdx(fillColor(paint)));
        batch.draw(white, left, top, right - left, bottom - top);
    }

    public void drawPath(Path path, Paint paint) {
        FloatArray p = path.getPoints();
        if (p.size < 6) {
            return;
        }
        applyTransformToBatch();
        fillPolygon(p, colorBits(fillColor(paint)));
    }

    private void fillPolygon(FloatArray pts, float colorBits) {
        int n = pts.size / 2;
        if (n < 3) {
            return;
        }
        float x0 = pts.get(0);
        float y0 = pts.get(1);
        for (int i = 1; i < n - 1; i++) {
            float x2 = pts.get((i + 1) * 2);
            float y2 = pts.get((i + 1) * 2 + 1);
            float[] v = {
                    x0, y0, colorBits, 0.5f, 0.5f,
                    pts.get(i * 2), pts.get(i * 2 + 1), colorBits, 0.5f, 0.5f,
                    x2, y2, colorBits, 0.5f, 0.5f,
                    x2, y2, colorBits, 0.5f, 0.5f
            };
            batch.draw(white, v, 0, 20);
        }
    }

    void fillShape(GradientDrawable drawable, Paint paint) {
        float radius = drawable.getGradientRadius();
        if (radius <= 0) {
            radius = 32f;
        }
        applyTransformToBatch();
        fillDisc(0, 0, radius, colorBits(fillColor(paint)));
    }

    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        applyTransformToBatch();
        fillDisc(cx, cy, radius, colorBits(fillColor(paint)));
    }

    private void fillDisc(float cx, float cy, float radius, float colorBits) {
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg;
            double a1 = 2 * Math.PI * (i + 1) / seg;
            float x1 = cx + (float) Math.cos(a0) * radius;
            float y1 = cy + (float) Math.sin(a0) * radius;
            float x2 = cx + (float) Math.cos(a1) * radius;
            float y2 = cy + (float) Math.sin(a1) * radius;
            float[] v = {
                    cx, cy, colorBits, 0.5f, 0.5f,
                    x1, y1, colorBits, 0.5f, 0.5f,
                    x2, y2, colorBits, 0.5f, 0.5f,
                    x2, y2, colorBits, 0.5f, 0.5f
            };
            batch.draw(white, v, 0, 20);
        }
    }

    // ---------------------------------------------------------------- text

    private final com.badlogic.gdx.graphics.g2d.GlyphLayout textLayout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    /**
     * Glyph quads are emitted by hand: BitmapFont's built-in cache does not see the per-draw
     * projection matrix this bridge installs, and the batch is projected y-down like Android.
     * The caller's y is the baseline, as in android.graphics.Canvas.drawText.
     */
    public void drawText(String text, float x, float y, Paint paint) {
        if (font == null || text == null) {
            return;
        }
        applyTransformToBatch();
        font.getData().setScale(paint.getTextSize() / 15f);
        float cf = colorBits(fillColor(paint));
        float sx = font.getData().scaleX;
        float sy = font.getData().scaleY;
        textLayout.setText(font, text);
        for (int r = 0; r < textLayout.runs.size; r++) {
            com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun run = textLayout.runs.get(r);
            float pen = x + run.x;
            for (int i = 0; i < run.glyphs.size; i++) {
                BitmapFont.Glyph g = run.glyphs.get(i);
                if (g == null) {
                    continue;
                }
                pen += run.xAdvances.get(i);
                float w = g.width * sx;
                float h = g.height * sy;
                float gx = pen + g.xoffset * sx;
                float gy = y - font.getData().capHeight - font.getData().ascent
                        - g.yoffset * sy - h;
                batch.draw(font.getRegion(g.page).getTexture(), new float[] {
                        gx, gy, cf, g.u, g.v2,
                        gx, gy + h, cf, g.u, g.v,
                        gx + w, gy + h, cf, g.u2, g.v,
                        gx + w, gy, cf, g.u2, g.v2
                }, 0, 20);
            }
        }
    }

    // ---------------------------------------------------------------- color helpers

    private int bitmapColor(Paint paint) {
        int argb = (paint.getAlpha() << 24) | (Color.WHITE & 0x00FFFFFF);
        ColorFilter cf = paint.getColorFilter();
        if (cf != null) {
            argb = cf.filter(argb);
        }
        return argb;
    }

    private int fillColor(Paint paint) {
        int argb = (paint.getAlpha() << 24) | (paint.getColor() & 0x00FFFFFF);
        ColorFilter cf = paint.getColorFilter();
        if (cf != null) {
            argb = cf.filter(argb);
        }
        return argb;
    }

    private com.badlogic.gdx.graphics.Color toGdx(int argb) {
        scratchColor.set(toGdxInt(argb));
        return scratchColor;
    }

    /** libGDX vertex colors are the NaN-safe float bits of the RGBA-packed color. */
    private float colorBits(int argb) {
        return toGdx(argb).toFloatBits();
    }

    /** Convert Android ARGB to libGDX RGBA int. */
    private static int toGdxInt(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int bl = argb & 0xFF;
        return (r << 24) | (g << 16) | (bl << 8) | alpha;
    }
}
