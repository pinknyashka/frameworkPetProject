package ru.fungames.checkers.launcher;

import java.util.ArrayList;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
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
 * MAIN  - game logo over the New Game / Continue / Settings / About buttons.
 * MAPS  - scrollable list of maps (game maps or test scenarios). A list entry is a plain
 *         button today; full preview + description comes later, so the entry layout keeps
 *         room for it.
 * ABOUT - build information.
 * SETTINGS - graphical/audio options; placeholder rows today, the switches are not wired.
 *            The shell can also open this screen over a running game (openSettingsOverlay);
 *            then Back returns to the game instead of the main menu.
 *
 * Drawn through the compat Canvas like the engine's OutputDraw path; taps/scroll come straight
 * from libGDX input because the engine touch pipeline does not exist yet at this point.
 */
public class LauncherScreen extends InputAdapter {

    public static final int SCREEN_MAIN = 0;
    public static final int SCREEN_ABOUT = 1;
    public static final int SCREEN_MAPS = 2;
    public static final int SCREEN_SETTINGS = 3;

    private static final int BUTTON_NEW_GAME = 1;
    private static final int BUTTON_CONTINUE = 2;
    private static final int BUTTON_SETTINGS = 3;
    private static final int BUTTON_ABOUT = 4;
    private static final int BUTTON_BACK = 5;
    private static final int BUTTON_TOGGLE_LIST = 6;
    /** Not yet wired: drawn dimmed, a tap does nothing (activate() has no case for it). */
    private static final int BUTTON_SETTINGS_ROW = 7;

    private static final int BUTTON_NONE = -1;

    private static final float MAP_ITEM_HEIGHT = 108f;
    private static final float THUMBNAIL_SIZE = 84f;
    private static final float TAP_SLOP = 12f;

    /** Logo from art/Logo-checkers.png, keyed and trimmed by art/PrepareLogo.java. */
    private static final String LOGO_IMAGE = "checkers_logo.png";
    private static final String FALLBACK_TITLE = "framework2d checkers";
    private static final float LOGO_TOP = 0.01f;
    private static final float LOGO_MAX_WIDTH_RATIO = 0.558f;
    private static final float LOGO_MAX_HEIGHT_RATIO = 0.288f;

    // Shared with the main-screen layout of art/PrepareLogo.java preview; keep both in sync.
    private static final int MAIN_BUTTON_COUNT = 4;
    private static final float MAIN_BUTTON_WIDTH_RATIO = 0.38f;
    private static final float MAIN_MIN_BUTTON_HEIGHT = 30f;
    private static final float MAIN_STACK_TOP = 0.34f;
    private static final float MAIN_STACK_BOTTOM = 0.90f;

    /**
     * Sub-screen planks (map toggle, settings rows) stop growing taller than this of the screen.
     * Sized from the same width budget as the main stack this puts them at the main buttons' size
     * in portrait, and keeps the map list's vertical budget in short windows.
     */
    private static final float SUB_PLANK_MAX_HEIGHT_RATIO = 0.115f;
    private final float[] plankSizeScratch = new float[2];

    public interface MapChooser {
        void onMapChosen(String mapName);

        /** The settings screen opened over a running game (openSettingsOverlay) was left. */
        void onSettingsClosed();
    }

    private final GameResources resources;
    private final MapChooser chooser;

    private final ArrayList<MapInfo> gameMaps = new ArrayList<MapInfo>();
    private final ArrayList<MapInfo> testMaps = new ArrayList<MapInfo>();
    private final ObjectMap<String, Bitmap> previewCache = new ObjectMap<String, Bitmap>();
    private Bitmap noPreviewPlaceholder;
    private Bitmap mainBackground;
    private boolean mainBackgroundLoadAttempted;
    private final Rect mainBackgroundSrc = new Rect();
    private final RectF mainBackgroundDst = new RectF();

    private Bitmap logo;
    private boolean logoLoadAttempted;
    private final Rect logoSrc = new Rect();
    private final RectF logoDst = new RectF();

    private Bitmap buttonUnpressed;
    private Bitmap buttonPressed;
    private boolean buttonImagesLoadAttempted;
    private int pressedButtonId = BUTTON_NONE;

    private int screen = SCREEN_MAIN;
    private boolean showingTestMaps;
    /** True while the settings screen is shown over a running game: Back returns to the game. */
    private boolean settingsOverlayMode;

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
    private final Paint imagePaint = new Paint();
    private final Paint disabledImagePaint = new Paint();

    private int frames;
    private int autoSelectMapAfterFrames;
    private String autoSelectMapName;
    private boolean autoSelectTests;
    private boolean autoSelectAbout;
    private boolean autoSelectSettings;
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
        imagePaint.setColor(Color.WHITE);
        disabledImagePaint.setColor(Color.argb(160, 255, 255, 255));
    }

    // ------------------------------------------------------------------ data

    public void setLastPlayedMap(String mapName) {
        lastPlayedMap = mapName;
    }

    /**
     * Smoke helper: after the given number of drawn frames the launcher jumps to a screen.
     * {@code about}/{@code settings} open that screen; a named map file starts that map;
     * otherwise the map list opens ({@code tests} picks the test-scenario list) and
     * {@code scrollPixels} offsets it.
     */
    public void setAutoSelectMapAfterFrames(int frames, String mapFileName, boolean tests,
            int scrollPixels, boolean about, boolean settings) {
        autoSelectMapAfterFrames = frames;
        autoSelectMapName = mapFileName;
        autoSelectTests = tests;
        autoSelectAbout = about;
        autoSelectSettings = settings;
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
            pressedButtonId = findButtonId(screenX, screenY);
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer != primaryPointer) {
            return true;
        }
        dragDistance += Math.abs(screenY - primaryY) + Math.abs(screenX - primaryX);
        if (dragDistance <= TAP_SLOP) {
            pressedButtonId = findButtonId(screenX, screenY);
        } else {
            pressedButtonId = BUTTON_NONE;
        }
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
        pressedButtonId = BUTTON_NONE;
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (pointer == primaryPointer) {
            primaryPointer = -1;
            pressedButtonId = BUTTON_NONE;
        }
        return true;
    }

    /** Desktop convenience: Escape leaves the current launcher sub-screen. */
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE && screen != SCREEN_MAIN) {
            leaveSubScreen();
            return true;
        }
        return false;
    }

    private int findButtonId(float x, float y) {
        for (int i = 0; i < frameButtonRects.size(); i++) {
            if (frameButtonRects.get(i).contains(x, y)) {
                return frameButtonIds.get(i);
            }
        }
        return BUTTON_NONE;
    }

    private void handleTap(float x, float y) {
        if (backButton.width() > 0f && backButton.contains(x, y)) {
            leaveSubScreen();
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
            case BUTTON_SETTINGS:
                screen = SCREEN_SETTINGS;
                break;
            case BUTTON_ABOUT:
                screen = SCREEN_ABOUT;
                break;
            case BUTTON_BACK:
                leaveSubScreen();
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
        settingsOverlayMode = false;
        screen = SCREEN_MAIN;
    }

    /**
     * Show the settings screen over a running game: Back then returns to the game through
     * {@code chooser.onSettingsClosed()} instead of going to the main menu.
     */
    public void openSettingsOverlay() {
        settingsOverlayMode = true;
        screen = SCREEN_SETTINGS;
    }

    private void leaveSubScreen() {
        if (settingsOverlayMode && screen == SCREEN_SETTINGS) {
            settingsOverlayMode = false;
            chooser.onSettingsClosed();
        } else {
            showMain();
        }
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
            case SCREEN_SETTINGS:
                drawSettings(canvas, width, height, margin, buttonWidth);
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
            } else if (autoSelectSettings) {
                screen = SCREEN_SETTINGS;
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
        drawMainBackground(canvas, width, height);
        float logoBottom = drawLogo(canvas, width, height);

        // Main labels are short, so keep the art close to its original aspect and make the
        // buttons wide only enough for the carved text area. If the window is short, scale the
        // whole button stack down rather than stretching a button out of proportion, and always
        // start it under the logo so the two never overlap on a small window.
        float aspect = ButtonArt.SRC_WIDTH / (float) ButtonArt.SRC_HEIGHT;
        float desiredWidth = Math.min(buttonWidth, width * MAIN_BUTTON_WIDTH_RATIO);
        float desiredHeight = desiredWidth / aspect;
        float gap = Math.max(8f, desiredHeight * 0.12f);
        float stackBottom = height * MAIN_STACK_BOTTOM;
        float stackTop = Math.max(logoBottom + gap, height * MAIN_STACK_TOP);
        float mainButtonHeight = Math.max(MAIN_MIN_BUTTON_HEIGHT, Math.min(desiredHeight,
                (stackBottom - stackTop - (MAIN_BUTTON_COUNT - 1) * gap) / MAIN_BUTTON_COUNT));
        float mainButtonWidth = Math.min(desiredWidth, mainButtonHeight * aspect);
        gap = Math.max(8f, mainButtonHeight * 0.12f);
        float mainTextSize = Math.min(26f, mainButtonHeight * 0.34f);
        float x = (width - mainButtonWidth) / 2f;
        float y = Math.min(stackTop, stackBottom - MAIN_BUTTON_COUNT * mainButtonHeight
                - (MAIN_BUTTON_COUNT - 1) * gap);
        button(canvas, x, y, mainButtonWidth, mainButtonHeight, "New Game", BUTTON_NEW_GAME, true,
                mainTextSize);
        y += mainButtonHeight + gap;
        button(canvas, x, y, mainButtonWidth, mainButtonHeight, continueLabel(), BUTTON_CONTINUE,
                lastPlayedMap != null, mainTextSize);
        y += mainButtonHeight + gap;
        button(canvas, x, y, mainButtonWidth, mainButtonHeight, "Settings", BUTTON_SETTINGS, true,
                mainTextSize);
        y += mainButtonHeight + gap;
        button(canvas, x, y, mainButtonWidth, mainButtonHeight, "About", BUTTON_ABOUT, true,
                mainTextSize);
        drawText(canvas, smallPaint, footerLabel(width), width / 2f, height * 0.94f, true);
    }

    private String footerLabel(float width) {
        if (lastPlayedMap == null) {
            return "launcher prototype";
        }
        String footer = "last map: " + lastPlayedMap;
        String full = "launcher prototype - " + footer;
        return smallPaint.measureText(full) * 1.25f <= width - 24f ? full : footer;
    }

    private void drawMainBackground(Canvas canvas, float width, float height) {
        if (!mainBackgroundLoadAttempted) {
            mainBackgroundLoadAttempted = true;
            mainBackground = resources.getImageFromLocalRes("main.png");
            if (mainBackground != null && mainBackground.texture != null) {
                mainBackground.texture.setFilter(Texture.TextureFilter.Linear,
                        Texture.TextureFilter.Linear);
            }
        }
        if (mainBackground == null || mainBackground.texture == null) {
            return;
        }

        int bitmapWidth = mainBackground.getWidth();
        int bitmapHeight = mainBackground.getHeight();
        if (bitmapWidth <= 0 || bitmapHeight <= 0) {
            return;
        }

        float scale = Math.max(width / (float) bitmapWidth, height / (float) bitmapHeight);
        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;

        mainBackgroundSrc.set(0, 0, bitmapWidth, bitmapHeight);
        mainBackgroundDst.set((width - scaledWidth) / 2f, (height - scaledHeight) / 2f,
                (width + scaledWidth) / 2f, (height + scaledHeight) / 2f);
        canvas.drawBitmap(mainBackground, mainBackgroundSrc, mainBackgroundDst, imagePaint);
    }

    /**
     * The logo takes the place of the plain title label. It keeps the artwork aspect, is never
     * upscaled past its own pixels, and is capped well below the button stack, so the grain of the
     * checkers stays readable and the characters on the menu background stay visible around it.
     *
     * @return the bottom edge of the logo; the button stack starts below it
     */
    private float drawLogo(Canvas canvas, float width, float height) {
        loadLogoIfNeeded();
        // GameResources answers a missing file with a 1x1 placeholder instead of null.
        if (logo == null || logo.texture == null || logo.getWidth() <= 1 || logo.getHeight() <= 1) {
            drawTitle(canvas, FALLBACK_TITLE, width, height * 0.16f, 30f, Color.WHITE);
            return height * 0.16f;
        }

        float scale = Math.min(Math.min(width * LOGO_MAX_WIDTH_RATIO, logo.getWidth())
                / (float) logo.getWidth(), height * LOGO_MAX_HEIGHT_RATIO
                / (float) logo.getHeight());
        float logoWidth = logo.getWidth() * scale;
        float logoHeight = logo.getHeight() * scale;
        float top = height * LOGO_TOP;

        logoSrc.set(0, 0, logo.getWidth(), logo.getHeight());
        logoDst.set((width - logoWidth) / 2f, top, (width + logoWidth) / 2f, top + logoHeight);
        canvas.drawBitmap(logo, logoSrc, logoDst, imagePaint);
        return top + logoHeight;
    }

    private void loadLogoIfNeeded() {
        if (logoLoadAttempted) {
            return;
        }
        logoLoadAttempted = true;
        logo = resources.getImageFromLocalRes(LOGO_IMAGE);
        if (logo != null && logo.texture != null) {
            logo.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    private String continueLabel() {
        return "Continue";
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

    /**
     * The settings entry point. The rows are drawn dimmed like a disabled Continue because the
     * switches (graphics quality, direct shadows, sounds) are planned but not wired yet: a tap
     * on a row does nothing and it never shows the pressed art. The test-scenario list that
     * used to sit under the Settings slot is reached from the `show test scenarios` switch on
     * the New Game map list.
     */
    private void drawSettings(Canvas canvas, float width, float height, float margin,
            float buttonWidth) {
        backButton(canvas);
        drawTitle(canvas, "Settings", width, height * 0.16f, 30f, Color.WHITE);

        // The rows are planks of the same aspect as every other carved button: width comes from
        // the same budget as the main stack, height follows the art ratio, and a short window
        // caps the height instead of squashing the plank.
        plankSize(Math.min(buttonWidth, width * MAIN_BUTTON_WIDTH_RATIO),
                height * SUB_PLANK_MAX_HEIGHT_RATIO, plankSizeScratch);
        float rowWidth = plankSizeScratch[0];
        float rowHeight = plankSizeScratch[1];
        float rowGap = rowHeight * 0.25f;
        float x = (width - rowWidth) / 2f;
        float y = height * 0.26f;
        String[] rows = {"Graphics quality", "Direct shadows", "Sounds"};
        for (String row : rows) {
            button(canvas, x, y, rowWidth, rowHeight, row, BUTTON_SETTINGS_ROW, false,
                    rowHeight * 0.42f);
            y += rowHeight + rowGap;
        }
        drawText(canvas, smallPaint, "options are not wired yet - placeholders", width / 2f,
                height * 0.94f, true);
    }

    /**
     * Largest carved-button size fitting both limits while keeping the art aspect: width is
     * {@code maxWidth} unless the matching height would exceed {@code maxHeight}, in which case
     * the height is capped and the width follows the ratio. Result goes into {@code out}
     * as {width, height}.
     */
    private static void plankSize(float maxWidth, float maxHeight, float[] out) {
        float width = maxWidth;
        float height = width / ButtonArt.ASPECT;
        if (height > maxHeight) {
            height = maxHeight;
            width = height * ButtonArt.ASPECT;
        }
        out[0] = width;
        out[1] = height;
    }

    private void drawMapList(Canvas canvas, float width, float height, float margin, float buttonWidth) {
        backButton(canvas);
        drawTitle(canvas, showingTestMaps ? "Test scenarios" : "New game",
                width, height * 0.075f, 26f, Color.WHITE);

        // A plank at the art's own aspect ratio (never stretched); capped so the list keeps
        // its vertical budget on short windows.
        plankSize(Math.min(buttonWidth, width * MAIN_BUTTON_WIDTH_RATIO),
                height * SUB_PLANK_MAX_HEIGHT_RATIO, plankSizeScratch);
        float toggleBottom = button(canvas, (width - plankSizeScratch[0]) / 2f, height * 0.10f,
                plankSizeScratch[0], plankSizeScratch[1],
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
        // Height fixed by the corner art; width keeps the plank aspect (40 * 2.62), so the
        // button is never a squashed or stretched plank. Matches CheckersGame's overlay buttons.
        button(canvas, 12f, 12f, 105f, 40f, "< Back", BUTTON_BACK, true);
        backButton.set(12f, 12f, 117f, 52f);
    }

    private float button(Canvas canvas, float x, float y, float width, float height,
            String label, int id, boolean enabled) {
        return button(canvas, x, y, width, height, label, id, enabled, height * 0.42f);
    }

    private float button(Canvas canvas, float x, float y, float width, float height,
            String label, int id, boolean enabled, float maxTextSize) {
        loadButtonImagesIfNeeded();

        boolean pressed = enabled && pressedButtonId == id;
        Bitmap buttonBitmap = pressed ? buttonPressed : buttonUnpressed;
        Paint bitmapPaint = enabled ? imagePaint : disabledImagePaint;
        if (buttonBitmap != null && buttonBitmap.texture != null) {
            ButtonArt.draw(canvas, buttonBitmap, x, y, width, height, bitmapPaint);
        } else {
            fillPaint.setColor(enabled ? Color.argb(255, 70, 100, 160) : Color.argb(120, 60, 66, 78));
            canvas.drawRect(x, y, x + width, y + height, fillPaint);
        }

        textPaint.setColor(enabled ? ButtonArt.LABEL_FILL : ButtonArt.LABEL_FILL_DISABLED);
        ButtonArt.drawLabel(canvas, textPaint, label, x, y, width, height, maxTextSize);
        textPaint.setTextSize(24f);
        frameButtonRects.add(new RectF(x, y, x + width, y + height));
        frameButtonIds.add(id);
        return y + height;
    }

    private void loadButtonImagesIfNeeded() {
        if (buttonImagesLoadAttempted) {
            return;
        }
        buttonImagesLoadAttempted = true;
        buttonUnpressed = resources.getImageFromLocalRes("button_unpressed.png");
        buttonPressed = resources.getImageFromLocalRes("button_pressed.png");
        ButtonArt.loadSmooth(buttonUnpressed);
        ButtonArt.loadSmooth(buttonPressed);
    }

    private void drawTitle(Canvas canvas, String text, float width, float y, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        drawText(canvas, textPaint, text, width / 2f, y, true);
        textPaint.setTextSize(24f);
    }

    private void drawText(Canvas canvas, Paint paint, String text, float centerXOrX, float baseline,
            boolean centered) {
        // Centre on the real glyph advances: Paint.measureText is a character-count estimate, so
        // centring on it shifts short labels off the middle of their field.
        float advance = canvas.textWidth(text, paint);
        float x = centered ? centerXOrX - (advance > 0f ? advance : paint.measureText(text)) / 2f
                : centerXOrX;
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
        if (mainBackground != null) {
            mainBackground.recycle();
            mainBackground = null;
        }
        if (logo != null) {
            logo.recycle();
            logo = null;
        }
        if (buttonUnpressed != null) {
            buttonUnpressed.recycle();
            buttonUnpressed = null;
        }
        if (buttonPressed != null) {
            buttonPressed.recycle();
            buttonPressed = null;
        }
    }
}
