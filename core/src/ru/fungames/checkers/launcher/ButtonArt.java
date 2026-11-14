package ru.fungames.checkers.launcher;

import com.badlogic.gdx.graphics.Texture;

import ru.framework2d.compat.Bitmap;
import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Color;
import ru.framework2d.compat.Paint;
import ru.framework2d.compat.Rect;
import ru.framework2d.compat.RectF;

/**
 * The carved plank button art (assets/Images/button_unpressed/pressed.png) shared by the
 * launcher screens and the in-game back button: nine-slice draw plus a label aligned to the
 * carved text band.
 */
final class ButtonArt {

    static final int SRC_X = 135;
    static final int SRC_Y = 240;
    static final int SRC_WIDTH = 1650;
    static final int SRC_HEIGHT = 630;
    static final int SRC_SLICE = 190;

    /** Width / height of the source rect: a button keeps this aspect, or the plank stretches. */
    static final float ASPECT = SRC_WIDTH / (float) SRC_HEIGHT;

    /**
     * The button art is a carved plank: a smooth band between two decorative carve rows carries
     * the label. Measuring the art, the band runs over the middle 0.30..0.65 of the nine-slice
     * source rect, so the label must sit at its centre, not at the button's geometric centre.
     * draw() scales every source region proportionally, so that fraction of the source rect is
     * the same fraction of any drawn button.
     */
    static final float FIELD_CENTER_Y = 0.47f;

    /**
     * Label colours, following the art/Buttons/stock/Readable_font.png sample: light cream
     * glyphs, a thick dark-brown outline ring and a soft drop shadow. Dark ink printed onto
     * the wood texture (the previous style) loses its thin strokes to the grain at phone
     * sizes; outlined cream survives it.
     */
    static final int LABEL_FILL = Color.rgb(250, 243, 224);
    static final int LABEL_FILL_DISABLED = Color.argb(230, 250, 243, 224);

    private static final int LABEL_OUTLINE = Color.rgb(84, 44, 18);
    private static final int LABEL_SHADOW = Color.argb(150, 30, 14, 5);

    // Draw scratch shared by all callers: drawing is single-threaded, one draw at a time.
    private static final Rect nineSrc = new Rect();
    private static final RectF nineDst = new RectF();
    private static final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        outlinePaint.setColor(LABEL_OUTLINE);
        shadowPaint.setColor(LABEL_SHADOW);
    }

    private ButtonArt() {
    }

    static void loadSmooth(Bitmap bitmap) {
        if (bitmap != null && bitmap.texture != null) {
            bitmap.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    static void draw(Canvas canvas, Bitmap bitmap, float x, float y, float width, float height,
            Paint paint) {
        int srcW = SRC_WIDTH;
        int srcH = SRC_HEIGHT;
        int slice = Math.min(SRC_SLICE, Math.min(srcW / 3, srcH / 3));

        float cornerX = slice * (width / (float) srcW);
        float cornerY = slice * (height / (float) srcH);
        if (width <= cornerX * 2f || height <= cornerY * 2f || cornerX <= 0f || cornerY <= 0f) {
            drawPart(canvas, bitmap, paint, SRC_X, SRC_Y, srcW, srcH, x, y, width, height);
            return;
        }

        int centerW = srcW - 2 * slice;
        int centerH = srcH - 2 * slice;
        float centerWidth = width - 2f * cornerX;
        float centerHeight = height - 2f * cornerY;

        drawPart(canvas, bitmap, paint, SRC_X, SRC_Y, slice, slice,
                x, y, cornerX, cornerY);
        drawPart(canvas, bitmap, paint, SRC_X + srcW - slice, SRC_Y, slice, slice,
                x + width - cornerX, y, cornerX, cornerY);
        drawPart(canvas, bitmap, paint, SRC_X, SRC_Y + srcH - slice, slice, slice,
                x, y + height - cornerY, cornerX, cornerY);
        drawPart(canvas, bitmap, paint, SRC_X + srcW - slice, SRC_Y + srcH - slice,
                slice, slice, x + width - cornerX, y + height - cornerY, cornerX, cornerY);

        drawPart(canvas, bitmap, paint, SRC_X + slice, SRC_Y, centerW, slice,
                x + cornerX, y, centerWidth, cornerY);
        drawPart(canvas, bitmap, paint, SRC_X + slice, SRC_Y + srcH - slice, centerW, slice,
                x + cornerX, y + height - cornerY, centerWidth, cornerY);
        drawPart(canvas, bitmap, paint, SRC_X, SRC_Y + slice, slice, centerH,
                x, y + cornerY, cornerX, centerHeight);
        drawPart(canvas, bitmap, paint, SRC_X + srcW - slice, SRC_Y + slice, slice, centerH,
                x + width - cornerX, y + cornerY, cornerX, centerHeight);
        drawPart(canvas, bitmap, paint, SRC_X + slice, SRC_Y + slice, centerW, centerH,
                x + cornerX, y + cornerY, centerWidth, centerHeight);
    }

    /**
     * Centres the label on the carved band and paints it the way the reference art does:
     * uppercase cream glyphs over a dark outline ring and a soft drop shadow, which keeps the
     * text legible over the wood grain at phone pixel sizes. The caller owns the fill paint
     * (its colour stays as set); only the text size is changed, shrunk until the real glyph
     * advance plus the outline spread fits the carved width ({@code Paint.measureText} is only
     * a fallback when the canvas has no font metrics).
     */
    static void drawLabel(Canvas canvas, Paint paint, String label, float x, float y, float width,
            float height, float maxTextSize) {
        label = label.toUpperCase(java.util.Locale.ROOT);
        float size = maxTextSize;
        paint.setTextSize(size);
        // Keep the label clear of the decorative ends of the plank.
        float maxTextWidth = width - Math.max(48f, width * 0.24f);
        float advance = measuredWidth(canvas, paint, label);
        while (size > 10f && advance + 2f * outlineSpread(size) > maxTextWidth) {
            size -= 1f;
            paint.setTextSize(size);
            advance = measuredWidth(canvas, paint, label);
        }
        // Centre on the real glyph advances: Paint.measureText is a character-count estimate, so
        // centring on it shifts short labels off the middle of the band.
        float textX = x + width / 2f - advance / 2f;
        // Baseline placed so the capitals straddle the carved band's centre: the drawText bridge
        // paints capitals about 0.7*size tall above the baseline, so half of that is below it.
        float baseline = y + height * FIELD_CENTER_Y + size * 0.35f;
        float spread = outlineSpread(size);

        shadowPaint.setTextSize(size);
        canvas.drawText(label, textX + spread, baseline + spread, shadowPaint);
        outlinePaint.setTextSize(size);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                canvas.drawText(label, textX + dx * spread, baseline + dy * spread, outlinePaint);
            }
        }
        canvas.drawText(label, textX, baseline, paint);
    }

    /** Outline ring radius and shadow offset: about 8% of the text size, at least 2 px. */
    private static float outlineSpread(float textSize) {
        return Math.max(2f, textSize * 0.08f);
    }

    private static float measuredWidth(Canvas canvas, Paint paint, String label) {
        float advance = canvas.textWidth(label, paint);
        return advance > 0f ? advance : paint.measureText(label) * 1.25f;
    }

    private static void drawPart(Canvas canvas, Bitmap bitmap, Paint paint, int srcX, int srcY,
            int srcW, int srcH, float dstX, float dstY, float dstW, float dstH) {
        nineSrc.set(srcX, srcY, srcX + srcW, srcY + srcH);
        nineDst.set(dstX, dstY, dstX + dstW, dstY + dstH);
        canvas.drawBitmap(bitmap, nineSrc, nineDst, paint);
    }
}
