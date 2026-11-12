package ru.fungames.checkers.launcher;

import ru.framework2d.compat.Bitmap;
import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Color;
import ru.framework2d.compat.Paint;
import ru.framework2d.compat.RectF;
import ru.framework2d.core.GameResources;

/**
 * The "< Back" overlay shown on the running game screen: the same carved plank button as the
 * launcher's sub-screens, tapping it returns to the main menu (the app shell owns the tap,
 * the engine never sees it).
 *
 * Sized and placed exactly like {@code LauncherScreen}'s own back button so the two screens
 * read as one UI.
 */
public class BackToMenuButton {

    private static final float LEFT = 12f;
    private static final float TOP = 12f;
    private static final float WIDTH = 100f;
    private static final float HEIGHT = 40f;

    private final GameResources resources;
    private final RectF bounds = new RectF(LEFT, TOP, LEFT + WIDTH, TOP + HEIGHT);

    private Bitmap unpressed;
    private Bitmap pressed;
    private boolean loadAttempted;
    private boolean pressedShown;

    private final Paint artPaint = new Paint();
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackPaint = new Paint();

    public BackToMenuButton(GameResources resources) {
        this.resources = resources;
        artPaint.setColor(Color.WHITE);
        labelPaint.setColor(ButtonArt.TEXT_COLOR);
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public float centerX() {
        return bounds.left + WIDTH / 2f;
    }

    public float centerY() {
        return bounds.top + HEIGHT / 2f;
    }

    /** Pressed look while the owning pointer is down (same feedback as the launcher buttons). */
    public void setPressed(boolean pressed) {
        pressedShown = pressed;
    }

    public void draw(Canvas canvas) {
        loadIfNeeded();
        Bitmap bitmap = pressedShown ? pressed : unpressed;
        if (bitmap != null && bitmap.texture != null) {
            ButtonArt.draw(canvas, bitmap, bounds.left, bounds.top, WIDTH, HEIGHT, artPaint);
        } else {
            fallbackPaint.setColor(pressedShown
                    ? Color.argb(255, 55, 80, 130) : Color.argb(255, 70, 100, 160));
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, fallbackPaint);
        }
        ButtonArt.drawLabel(canvas, labelPaint, "< Back", bounds.left, bounds.top, WIDTH, HEIGHT,
                HEIGHT * 0.42f);
    }

    private void loadIfNeeded() {
        if (loadAttempted) {
            return;
        }
        loadAttempted = true;
        unpressed = resources.getImageFromLocalRes("button_unpressed.png");
        pressed = resources.getImageFromLocalRes("button_pressed.png");
        ButtonArt.loadSmooth(unpressed);
        ButtonArt.loadSmooth(pressed);
    }

    public void dispose() {
        if (unpressed != null) {
            unpressed.recycle();
            unpressed = null;
        }
        if (pressed != null) {
            pressed.recycle();
            pressed = null;
        }
    }
}
