package ru.fungames.checkers.launcher;

import java.util.ArrayList;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.ObjectMap;

import ru.framework2d.compat.Bitmap;
import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Color;
import ru.framework2d.compat.Paint;
import ru.framework2d.compat.Rect;
import ru.framework2d.compat.RectF;
import ru.framework2d.core.GameResources;

/**
 * Prototype launcher: a customizable preparation screen shown before the engine tree is built.
 *
 * MAIN  - New Game / Continue / Tests / About buttons.
 * MAPS  - scrollable list of maps (game maps or test scenarios). A list entry is a plain
 *         button today; full preview + description comes later, so the entry layout keeps
 *         room for it.
 * ABOUT - build information.
 *
 * Drawn through the compat Canvas like the engine's OutputDraw path; taps/scroll come straight
 * from libGDX input because the engine touch pipeline does not exist yet at this point.
 */
public class LauncherScreen extends InputAdapter {

    public static final int SCREEN_MAIN = 0;
    public static final int SCREEN_ABOUT = 1;
    public static final int SCREEN_MAPS = 2;

    private static final int BUTTON_NEW_GAME = 1;
    private static final int BUTTON_CONTINUE = 2;
    private static final int BUTTON_TESTS = 3;
    private static final int BUTTON_ABOUT = 4;
    private static final int BUTTON_BACK = 5;
    private static final int BUTTON_TOGGLE_LIST = 6;

    private static final float MAP_ITEM_HEIGHT = 108f;
    private static final float THUMBNAIL_SIZE = 84f;
    private static final float TAP_SLOP = 12f;

    public interface MapChooser {
        void onMapChosen(String mapName);
    }

    private final GameResources resources;
    private final MapChooser chooser;

    private final ArrayList<MapInfo> gameMaps = new ArrayList<MapInfo>();
    private final ArrayList<MapInfo> testMaps = new ArrayList<MapInfo>();
    private final ObjectMap<String, Bitmap> previewCache = new ObjectMap<String, Bitmap>();
    private Bitmap noPreviewPlaceholder;

    private int screen = SCREEN_MAIN;
    private boolean showingTestMaps;

    private float scrollOffset;
    private float contentHeight;
    private float listTop;
    private float listBottom;

    private int primaryPointer = -1;
    private float primaryX;
    private float primaryY;
    private float dragDistance;

    private String lastPlayedMap;

    private final RectF backButton = new RectF();
    private final ArrayList<RectF> frameButtonRects = new ArrayList<RectF>();
    private final ArrayList<Integer> frameButtonIds = new ArrayList<Integer>();

    private final Paint fillPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int frames;
    private int autoSelectMapAfterFrames;
    private String autoSelectMapName;
    private boolean autoSelectTests;
    private boolean autoSelectAbout;
    private int autoScrollPixels;

    private final String preferredGameMap;

    public LauncherScreen(GameResources resources, MapChooser chooser, String preferredGameMap,
            String lastPlayedMap) {
        this.resources = resources;
        this.chooser = chooser;
        this.preferredGameMap = preferredGameMap;
        this.lastPlayedMap = lastPlayedMap;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        smallPaint.setColor(Color.LTGRAY);
        smallPaint.setTextSize(14f);
    }

    // ------------------------------------------------------------------ data

    public void setLastPlayedMap(String mapName) {
        lastPlayedMap = mapName;
    }

    /**
     * Smoke helper: after the given number of drawn frames the launcher jumps to a screen.
     * {@code about} opens the about screen; a named map file starts that map; otherwise the map
     * list opens ({@code tests} picks the test-scenario list) and {@code scrollPixels} offsets it.
     */
    public void setAutoSelectMapAfterFrames(int frames, String mapFileName, boolean tests,
            int scrollPixels, boolean about) {
        autoSelectMapAfterFrames = frames;
        autoSelectMapName = mapFileName;
        autoSelectTests = tests;
        autoSelectAbout = about;
        autoScrollPixels = scrollPixels;
    }

    private void collectMapsIfNeeded() {
        if (!gameMaps.isEmpty() || !testMaps.isEmpty()) {
            return;
        }
        addNames(resources.getLocalMapNames());
        addNames(resources.getSdMapNames());
        sortMaps(gameMaps);
        sortMaps(testMaps);
    }

    /** The default/last map first, then by file name, so the list order is stable across runs. */
    private void sortMaps(ArrayList<MapInfo> maps) {
        java.util.Collections.sort(maps, new java.util.Comparator<MapInfo>() {
            @Override
            public int compare(MapInfo left, MapInfo right) {
                int rank = rank(left) - rank(right);
                return rank != 0 ? rank : left.fileName.compareToIgnoreCase(right.fileName);
            }
        });
    }

    private int rank(MapInfo info) {
        String base = info.fileName.replaceFirst("\\.xml$", "");
        return base.equals(preferredGameMap) ? 0 : 1;
    }

    private void addNames(String[] names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name == null || !name.toLowerCase(java.util.Locale.US).endsWith(".xml")) {
                continue;
            }
            MapInfo info = new MapInfo(name);
            if (info.test) {
                testMaps.add(info);
            } else {
                gameMaps.add(info);
            }
        }
    }

    private ArrayList<MapInfo> currentEntries() {
        if (screen != SCREEN_MAPS) {
            return null;
        }
        return showingTestMaps ? testMaps : gameMaps;
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (primaryPointer < 0) {
            primaryPointer = pointer;
            primaryX = screenX;
            primaryY = screenY;
            dragDistance = 0f;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer != primaryPointer) {
            return true;
        }
        dragDistance += Math.abs(screenY - primaryY) + Math.abs(screenX - primaryX);
        scrollOffset -= (screenY - primaryY);
        clampScroll();
        primaryX = screenX;
        primaryY = screenY;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer != primaryPointer) {
            return true;
        }
        primaryPointer = -1;
        if (dragDistance <= TAP_SLOP) {
            handleTap(screenX, screenY);
        }
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (pointer == primaryPointer) {
            primaryPointer = -1;
        }
        return true;
    }

    /** Desktop convenience: Escape leaves the current launcher sub-screen. */
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE && screen != SCREEN_MAIN) {
            showMain();
            return true;
        }
        return false;
    }

    private void handleTap(float x, float y) {
        if (backButton.width() > 0f && backButton.contains(x, y)) {
            showMain();
            return;
        }
        for (int i = 0; i < frameButtonRects.size(); i++) {
            RectF rect = frameButtonRects.get(i);
            if (rect.contains(x, y)) {
                activate(frameButtonIds.get(i));
                return;
            }
        }
        if (screen == SCREEN_MAPS && y >= listTop && y <= listBottom) {
            ArrayList<MapInfo> entries = currentEntries();
            clampScroll();
            float local = y + scrollOffset - listTop;
            int index = local < 0f ? -1 : (int) (local / MAP_ITEM_HEIGHT);
            if (index >= 0 && index < entries.size() && isEntryVisible(index)) {
                chooseMap(entries.get(index));
            }
        }
    }

    /** Mirrors the drawing rule: an entry is only reachable when it is fully inside the list window. */
    private boolean isEntryVisible(int index) {
        float itemTop = listTop + index * MAP_ITEM_HEIGHT - scrollOffset;
        return itemTop >= listTop && itemTop + MAP_ITEM_HEIGHT <= listBottom;
    }

    private void activate(int id) {
        switch (id) {
            case BUTTON_NEW_GAME:
                openMapList(false);
                break;
            case BUTTON_CONTINUE:
                if (lastPlayedMap != null) {
                    chooser.onMapChosen(lastPlayedMap);
                }
                break;
            case BUTTON_TESTS:
                openMapList(true);
                break;
            case BUTTON_ABOUT:
                screen = SCREEN_ABOUT;
                break;
            case BUTTON_BACK:
                showMain();
                break;
            case BUTTON_TOGGLE_LIST:
                collectMapsIfNeeded();
                showingTestMaps = !showingTestMaps;
                scrollOffset = 0f;
                break;
            default:
                break;
        }
    }

    private void openMapList(boolean tests) {
        collectMapsIfNeeded();
        showingTestMaps = tests;
        scrollOffset = 0f;
        screen = SCREEN_MAPS;
    }

    private void showMain() {
        screen = SCREEN_MAIN;
    }

    private void chooseMap(MapInfo info) {
        chooser.onMapChosen(info.fileName.replaceFirst("\\.xml$", ""));
    }

    // ----------------------------------------------------------------- draw

    public void draw(Canvas canvas, float width, float height) {
        frames++;
        collectMapsIfNeeded();

        frameButtonRects.clear();
        frameButtonIds.clear();
        backButton.set(0f, 0f, 0f, 0f);

        fillPaint.setColor(Color.argb(255, 34, 39, 48));
        canvas.drawRect(0f, 0f, width, height, fillPaint);

        float margin = Math.min(width, height) * 0.08f;
        float buttonWidth = width - 2f * margin;

        switch (screen) {
            case SCREEN_MAPS:
                drawMapList(canvas, width, height, margin, buttonWidth);
                break;
            case SCREEN_ABOUT:
                drawAbout(canvas, width, height, margin, buttonWidth);
                break;
            default:
                drawMain(canvas, width, height, margin, buttonWidth);
                break;
        }
    }

    /** Frame tick kept outside the SpriteBatch: engine building must not happen mid-draw. */
    public void update() {
        if (autoSelectMapAfterFrames > 0 && frames >= autoSelectMapAfterFrames) {
            autoSelectMapAfterFrames = 0;
            if (autoSelectAbout) {
                screen = SCREEN_ABOUT;
            } else if (autoSelectMapName != null) {
                showingTestMaps = autoSelectTests;
                chooser.onMapChosen(autoSelectMapName.replaceFirst("\\.xml$", ""));
            } else {
                openMapList(autoSelectTests);
                // No clamp here: the list geometry is only known once it has been drawn once.
                scrollOffset = autoScrollPixels;
            }
        }
    }

    private void drawMain(Canvas canvas, float width, float height, float margin, float buttonWidth) {
        drawTitle(canvas, "framework2d checkers", width, height * 0.16f, 30f, Color.WHITE);
        float x = margin;
        float y = height * 0.28f;
        float step = height * 0.095f;
        button(canvas, x, y, buttonWidth, step, "New Game", BUTTON_NEW_GAME, true);
        y += step * 1.2f;
        button(canvas, x, y, buttonWidth, step, continueLabel(), BUTTON_CONTINUE, lastPlayedMap != null);
        y += step * 1.2f;
        button(canvas, x, y, buttonWidth, step, "Tests", BUTTON_TESTS, true);
        y += step * 1.2f;
        button(canvas, x, y, buttonWidth, step, "About", BUTTON_ABOUT, true);
        drawText(canvas, smallPaint, "launcher prototype", width / 2f, height * 0.92f, true);
    }

    private String continueLabel() {
        return lastPlayedMap == null ? "Continue" : "Continue: " + lastPlayedMap;
    }

    private void drawAbout(Canvas canvas, float width, float height, float margin, float buttonWidth) {
        backButton(canvas);
        drawTitle(canvas, "About", width, height * 0.16f, 30f, Color.WHITE);
        float y = height * 0.28f;
        drawText(canvas, textPaint, "framework2d checkers prototype", width / 2f, y, true);
        drawText(canvas, smallPaint, "engine: libGDX + framework2d handlers/components",
                width / 2f, y + 34f, true);
        drawText(canvas, smallPaint, "maps: " + gameMaps.size() + " game, " + testMaps.size()
                + " test scenarios", width / 2f, y + 54f, true);
    }

    private void drawMapList(Canvas canvas, float width, float height, float margin, float buttonWidth) {
        backButton(canvas);
        drawTitle(canvas, showingTestMaps ? "Test scenarios" : "New game",
                width, height * 0.075f, 26f, Color.WHITE);

        float toggleHeight = height * 0.05f;
        float toggleBottom = button(canvas, margin, height * 0.10f, buttonWidth, toggleHeight,
                showingTestMaps ? "show game maps" : "show test scenarios", BUTTON_TOGGLE_LIST, true);

        listTop = toggleBottom + margin * 0.25f;
        listBottom = height - Math.max(40f, margin * 0.5f);

        ArrayList<MapInfo> entries = currentEntries();
        contentHeight = entries.size() * MAP_ITEM_HEIGHT;
        clampScroll();

        for (int i = 0; i < entries.size(); i++) {
            float itemTop = listTop + i * MAP_ITEM_HEIGHT - scrollOffset;
            if (itemTop < listTop) {
                continue;
            }
            // The compat Canvas has no clipping: only entries fully inside the list window are drawn.
            if (itemTop + MAP_ITEM_HEIGHT > listBottom) {
                break;
            }
            drawMapEntry(canvas, entries.get(i), margin, itemTop, buttonWidth);
        }

        if (entries.isEmpty()) {
            drawText(canvas, smallPaint, "no maps found", width / 2f, listTop + 30f, true);
        }
        drawText(canvas, smallPaint, "prototype: preview and description come later",
                width / 2f, height - 14f, true);
    }

    private void clampScroll() {
        float view = Math.max(0f, listBottom - listTop);
        float limit = Math.max(0f, contentHeight - view);
        scrollOffset = Math.max(0f, Math.min(limit, scrollOffset));
    }

    private void drawMapEntry(Canvas canvas, MapInfo info, float x, float y, float width) {
        fillPaint.setColor(Color.argb(255, 48, 55, 66));
        canvas.drawRect(x, y, x + width, y + MAP_ITEM_HEIGHT - 8f, fillPaint);

        float inner = y + (MAP_ITEM_HEIGHT - 8f - THUMBNAIL_SIZE) / 2f;
        Bitmap preview = previewOf(info);
        if (preview != null) {
            canvas.drawBitmap(preview,
                    new Rect(0, 0, preview.getWidth(), preview.getHeight()),
                    new Rect((int) (x + 12f), (int) inner,
                            (int) (x + 12f + THUMBNAIL_SIZE), (int) (inner + THUMBNAIL_SIZE)),
                    fillPaint);
        }

        float textX = x + 12f + THUMBNAIL_SIZE + 16f;
        float textWidth = width - THUMBNAIL_SIZE - 40f;
        textPaint.setTextSize(20f);
        textPaint.setColor(Color.WHITE);
        drawText(canvas, textPaint, fitText(textPaint, info.name, textWidth), textX, inner + 26f, false);
        drawText(canvas, smallPaint, fitText(smallPaint, info.fileName, textWidth),
                textX, inner + 48f, false);
        if (info.test) {
            smallPaint.setColor(Color.YELLOW);
            drawText(canvas, smallPaint, "test scenario", textX, inner + 68f, false);
            smallPaint.setColor(Color.LTGRAY);
        } else if (rank(info) == 0) {
            smallPaint.setColor(Color.CYAN);
            drawText(canvas, smallPaint, "default map", textX, inner + 68f, false);
            smallPaint.setColor(Color.LTGRAY);
        }
        textPaint.setTextSize(24f);
    }

    /** Paint.measureText is an estimate; truncate with ".." so text never leaves its cell. */
    private String fitText(Paint paint, String text, float maxWidth) {
        if (text == null || paint.measureText(text) * 1.25f <= maxWidth) {
            return text;
        }
        String out = text;
        while (out.length() > 2 && paint.measureText(out + "..") * 1.25f > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "..";
    }

    private Bitmap previewOf(MapInfo info) {
        if (info.preview == null) {
            if (noPreviewPlaceholder == null) {
                noPreviewPlaceholder = resources.getImageFromLocalRes("no_data.png");
            }
            return noPreviewPlaceholder;
        }
        Bitmap preview = previewCache.get(info.preview, null);
        if (preview == null) {
            preview = resources.getImage(info.preview);
            previewCache.put(info.preview, preview);
        }
        return preview;
    }

    // --------------------------------------------------------------- widgets

    private void backButton(Canvas canvas) {
        fillPaint.setColor(Color.argb(255, 60, 68, 82));
        canvas.drawRect(12f, 12f, 112f, 52f, fillPaint);
        textPaint.setTextSize(18f);
        textPaint.setColor(Color.WHITE);
        drawText(canvas, textPaint, "< Back", 24f, 38f, false);
        textPaint.setTextSize(24f);
        backButton.set(12f, 12f, 112f, 52f);
    }

    private float button(Canvas canvas, float x, float y, float width, float height,
            String label, int id, boolean enabled) {
        fillPaint.setColor(enabled ? Color.argb(255, 70, 100, 160) : Color.argb(120, 60, 66, 78));
        canvas.drawRect(x, y, x + width, y + height, fillPaint);
        float size = height * 0.42f;
        textPaint.setTextSize(size);
        textPaint.setColor(enabled ? Color.WHITE : Color.GRAY);
        // Paint.measureText is an estimate; keep a margin so long labels never leave the button.
        while (size > 10f && textPaint.measureText(label) * 1.25f > width - 16f) {
            size -= 1f;
            textPaint.setTextSize(size);
        }
        drawText(canvas, textPaint, label, x + width / 2f, y + height * 0.65f, true);
        textPaint.setTextSize(24f);
        frameButtonRects.add(new RectF(x, y, x + width, y + height));
        frameButtonIds.add(id);
        return y + height;
    }

    private void drawTitle(Canvas canvas, String text, float width, float y, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        drawText(canvas, textPaint, text, width / 2f, y, true);
        textPaint.setTextSize(24f);
    }

    private void drawText(Canvas canvas, Paint paint, String text, float centerXOrX, float baseline,
            boolean centered) {
        float x = centered ? centerXOrX - paint.measureText(text) / 2f : centerXOrX;
        canvas.drawText(text, x, baseline, paint);
    }

    // ------------------------------------------------------------- teardown

    public void dispose() {
        if (!previewCache.isEmpty()) {
            for (Bitmap bitmap : previewCache.values()) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
            previewCache.clear();
        }
        if (noPreviewPlaceholder != null) {
            noPreviewPlaceholder.recycle();
            noPreviewPlaceholder = null;
        }
    }
}
