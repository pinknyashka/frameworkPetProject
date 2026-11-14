package ru.framework2d.compat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * Builds the app's screen text font at a resolution that is only ever scaled down.
 *
 * libGDX's bundled bitmap font is 15 px tall; text at 20-30 px upscales it into blocky
 * artefacts. FreeType renders the bundled fonts antialiased at {@link #DESIGN_PIXELS} px, and
 * the drawing canvas divides requested text sizes by that design size, so the same paint size
 * keeps the same visual scale as with the 15 px font - just smooth.
 *
 * The first file wins. Nunito Black is the UI face: at phone size its thick rounded strokes
 * survive the carved plank texture, where regular-weight DejaVu Sans turned into unreadable
 * hairlines (see art/Buttons/stock/Readable_font.png for the look this is after). DejaVu stays
 * as the second attempt in case the newer ttf fails to load. Both are free to bundle and ship,
 * see assets/fonts/LICENSE.
 */
public final class SmoothFonts {

    /** Glyph generation height; every real text size (max ~30 px) downscales from it. */
    public static final int DESIGN_PIXELS = 48;

    /** Tried in order; first one that generates a usable font is the app's UI font. */
    private static final String[] FONT_FILES = {
            "fonts/Nunito-Black.ttf",
            "fonts/DejaVuSans.ttf",
    };

    private SmoothFonts() {
    }

    /**
     * Generates the smooth font and aligns {@code canvas} to its design size. Falls back to
     * libGDX's built-in 15 px font (and the canvas default base size) when the gdx-freetype
     * native or the ttf is unavailable: text then renders as it did before this existed.
     */
    public static BitmapFont installOn(Canvas canvas) {
        for (String fontFile : FONT_FILES) {
            BitmapFont font = tryGenerate(fontFile);
            if (font != null) {
                canvas.setTextBaseSize(DESIGN_PIXELS);
                return font;
            }
        }
        Gdx.app.log("compat", "no freetype font available, using built-in 15px font;");
        return new BitmapFont();
    }

    /** Null when this file or the freetype native is unusable; logs why, once per attempt. */
    private static BitmapFont tryGenerate(String fontFile) {
        try {
            FreeTypeFontGenerator generator =
                    new FreeTypeFontGenerator(Gdx.files.internal(fontFile));
            try {
                FreeTypeFontParameter parameter = new FreeTypeFontParameter();
                parameter.size = DESIGN_PIXELS;
                // ASCII plus the Cyrillic block: the built-in font covered both, keep map text
                // and labels in any language of the game data drawable.
                parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                        + "\u0401\u0410-\u042F\u0430-\u044F\u0451\u2013\u2014\u2018\u2019\u201C\u201D\u2026";
                return generator.generateFont(parameter);
            } finally {
                generator.dispose();
            }
        } catch (Throwable fontUnavailable) {
            // UnsatisfiedLinkError is an Error, not an Exception: the native simply may be absent.
            Gdx.app.log("compat", "freetype font " + fontFile + " unavailable: "
                    + fontUnavailable);
            return null;
        }
    }
}
