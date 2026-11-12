package ru.framework2d.compat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * Builds the app's screen text font at a resolution that is only ever scaled down.
 *
 * libGDX's bundled bitmap font is 15 px tall; text at 20-30 px upscales it into blocky
 * artefacts. FreeType renders the bundled DejaVu Sans antialiased at {@link #DESIGN_PIXELS}
 * px, and the drawing canvas divides requested text sizes by that design size, so the same
 * paint size keeps the same visual scale as with the 15 px font - just smooth.
 */
public final class SmoothFonts {

    /** Glyph generation height; every real text size (max ~30 px) downscales from it. */
    public static final int DESIGN_PIXELS = 48;

    /** Bundled under SIL-style DejaVu licence, see assets/fonts/LICENSE. */
    private static final String FONT_FILE = "fonts/DejaVuSans.ttf";

    private SmoothFonts() {
    }

    /**
     * Generates the smooth font and aligns {@code canvas} to its design size. Falls back to
     * libGDX's built-in 15 px font (and the canvas default base size) when the gdx-freetype
     * native or the ttf is unavailable: text then renders as it did before this existed.
     */
    public static BitmapFont installOn(Canvas canvas) {
        try {
            FreeTypeFontGenerator generator =
                    new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
            try {
                FreeTypeFontParameter parameter = new FreeTypeFontParameter();
                parameter.size = DESIGN_PIXELS;
                // ASCII plus the Cyrillic block: the built-in font covered both, keep map text
                // and labels in any language of the game data drawable.
                parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                        + "\u0401\u0410-\u042F\u0430-\u044F\u0451\u2013\u2014\u2018\u2019\u201C\u201D\u2026";
                BitmapFont smoothFont = generator.generateFont(parameter);
                canvas.setTextBaseSize(DESIGN_PIXELS);
                return smoothFont;
            } finally {
                generator.dispose();
            }
        } catch (Throwable fontUnavailable) {
            // UnsatisfiedLinkError is an Error, not an Exception: the native simply may be absent.
            Gdx.app.log("compat", "freetype font unavailable, using built-in 15px font: "
                    + fontUnavailable);
            return new BitmapFont();
        }
    }
}
