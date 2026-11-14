package ru.fungames.checkers.launcher;

import ru.framework2d.compat.Bitmap;
import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Color;
import ru.framework2d.compat.Paint;
import ru.framework2d.compat.RectF;
import ru.framework2d.core.GameResources;

/**
 * A carved plank button drawn over the running game screen ("< Back", "Settings"): the same
 * art and placement rules as the launcher buttons, sized and placed like
 * {@code LauncherScreen}'s own back button so all screens read as one UI.
 *
 * The app shell owns the tap: tapping one hands a message to the shell (return to menu, open
 * settings), the engine never sees a half-tap under the button.
 */
public class OverlayButton {

    private final GameResources resources;
    private final String label;
    private final RectF bounds;

    private Bitmap unpressed;
    private Bitmap pressed;
    private boolean loadAttempted;
    private boolean pressedShown;

    private final Paint artPaint = new Paint();
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackPaint = new Paint();

    public OverlayButton(GameResources resources, String label, float left, float top,
            float width, float height) {
        this.resources = resources;
        this.label = label;
        this.bounds = new RectF(left, top, left + width, top + height);
        artPaint.setColor(Color.WHITE);
        labelPaint.setColor(ButtonArt.LABEL_FILL);
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public float centerX() {
        return bounds.left + bounds.width() / 2f;
    }

    public float centerY() {
        return bounds.top + bounds.height() / 2f;
    }

    /** Pressed look while the owning pointer is down (same feedback as the launcher buttons). */
    public void setPressed(boolean pressed) {
        pressedShown = pressed;
    }

    public void draw(Canvas canvas) {
        loadIfNeeded();
        Bitmap bitmap = pressedShown ? pressed : unpressed;
        if (bitmap != null && bitmap.texture != null) {
            ButtonArt.draw(canvas, bitmap, bounds.left, bounds.top, bounds.width(),
                    bounds.height(), artPaint);
        } else {
            fallbackPaint.setColor(pressedShown
                    ? Color.argb(255, 55, 80, 130) : Color.argb(255, 70, 100, 160));
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, fallbackPaint);
        }
        ButtonArt.drawLabel(canvas, labelPaint, label, bounds.left, bounds.top, bounds.width(),
                bounds.height(), bounds.height() * 0.42f);
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
