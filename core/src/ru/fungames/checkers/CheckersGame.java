package ru.fungames.checkers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.MotionEvent;
import ru.framework2d.compat.SmoothFonts;
import ru.framework2d.core.Engine;
import ru.framework2d.core.GameCore;
import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.graphics.RenderPeriphery;
import ru.framework2d.touches.TouchPeriphery;

import ru.framework2d.gdx.TouchBridge;

import ru.fungames.checkers.launcher.BackToMenuButton;
import ru.fungames.checkers.launcher.LauncherScreen;

public class CheckersGame extends Game {

    public static final String DEFAULT_MAP = "checkers_tree";
    public static final String PREFERENCES = "framework2d";
    public static final String LAST_MAP_KEY = "lastMap";

    /** Map name (without .xml) loaded by {@link #startGame(String)}. */
    public String mapName = DEFAULT_MAP;

    /** When true, {@link #create()} shows the launcher and defers engine creation until a map is chosen. */
    public boolean showLauncher = true;

    /** When > 0, the launcher auto-selects a map after this many frames (smoke helper). */
    public int launcherAutoSelectAfterFrames;

    /** Optional map file name (with or without .xml) for the auto-select helper. */
    public String launcherAutoSelectMap;

    /** When true, the auto-select helper works on the test-scenario list instead of game maps. */
    public boolean launcherAutoSelectTests;

    /** Optional list offset applied when the auto-select helper opens the map list (smoke helper). */
    public int launcherAutoScrollPixels;

    /** When true, the auto-select helper opens the about screen instead of a map list (smoke helper). */
    public boolean launcherAutoOpenAbout;

    /** When > 0, a synthetic tap on the in-game back button fires after this many frames (smoke helper). */
    public int backSmokeAfterFrames;

    private LauncherScreen launcher;
    private BackToMenuButton backToMenuButton;
    private GameInputProcessor gameInputProcessor;
    private boolean gameStarted;

    private GameCore core;
    private GraphicalPeriphery periphery;
    private TouchPeriphery touch;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private Canvas canvas;

    /** When > 0, the desktop smoke test exits after this many frames. 0 = run forever. */
    public int exitAfterFrames = 0;
    private int frames;

    /** When set, the frame that ends the smoke run is dumped as PNG to this path. */
    public String screenshotPath;

    public boolean dragSmoke;

    @Override
    public void create() {
        batch = new SpriteBatch();
        canvas = new Canvas();
        font = SmoothFonts.installOn(canvas);

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (showLauncher) {
            launcher = new LauncherScreen(new ru.framework2d.core.GameResources(),
                    new LauncherScreen.MapChooser() {
                        @Override
                        public void onMapChosen(String chosenMapName) {
                            startGame(chosenMapName);
                        }
                    }, DEFAULT_MAP, launcherLastMap());
            // Only meaningful when there is a menu to go back to; direct-to-map runs have none.
            backToMenuButton = new BackToMenuButton(new ru.framework2d.core.GameResources());
            launcher.setAutoSelectMapAfterFrames(launcherAutoSelectAfterFrames,
                    launcherAutoSelectMap, launcherAutoSelectTests, launcherAutoScrollPixels,
                    launcherAutoOpenAbout);
            Gdx.input.setInputProcessor(launcher);
            Gdx.app.log("framework2d", "launcher shown; lastMap=" + launcherLastMap() + ";");
        } else {
            startGame(mapName == null ? DEFAULT_MAP : mapName);
        }
    }

    /** Builds the engine tree for the chosen map. Called once per game session. */
    public void startGame(String chosenMapName) {
        mapName = chosenMapName == null || chosenMapName.isEmpty() ? DEFAULT_MAP : chosenMapName;
        if (launcher != null && launcherAutoSelectAfterFrames == 0) {
            // Only a real player choice owns the "last played map" record; smoke runs stay side-effect free.
            Gdx.app.getPreferences(PREFERENCES).putString(LAST_MAP_KEY, mapName).flush();
        }

        core = new GameCore();
        core.createObjectsModel(mapName);

        for (Engine engine : core.enginesManager.lstEngines) {
            if (engine instanceof GraphicalPeriphery) {
                periphery = (GraphicalPeriphery) engine;
            } else if (engine instanceof TouchPeriphery) {
                touch = (TouchPeriphery) engine;
            }
        }

        // The back button gets the tap first so it never also lands in the game; maps without a
        // touch engine get a null bridge, and everything but the button is then ignored.
        gameInputProcessor = new GameInputProcessor(touch != null ? new TouchBridge(touch) : null);
        Gdx.input.setInputProcessor(gameInputProcessor);

        gameStarted = true;
        Gdx.app.log("framework2d", "engine built; map=" + mapName
                + "; periphery=" + periphery + "; touch=" + touch);
    }

    /**
     * Tears the engine tree down and returns to the launcher. Note: the engine singletons
     * (Engine.resources / entityStorage / enginesManager / enginesScheduler) are re-injected
     * by the next {@link GameCore} constructor; this is the safe restart path.
     */
    public void returnToMenu() {
        if (periphery instanceof RenderPeriphery) {
            ((RenderPeriphery) periphery).dispose();
        }
        core = null;
        periphery = null;
        touch = null;
        gameStarted = false;
        if (backToMenuButton != null) {
            backToMenuButton.setPressed(false);
        }
        if (launcher != null) {
            launcher.setLastPlayedMap(mapName);
            Gdx.input.setInputProcessor(launcher);
        }
        Gdx.app.log("framework2d", "returned to launcher;");
    }

    private String launcherLastMap() {
        return Gdx.app.getPreferences(PREFERENCES).getString(LAST_MAP_KEY, null);
    }

    @Override
    public void resize(int width, int height) {
        if (camera == null) {
            return;
        }
        // y-down projection, top-left origin (matches the engine's Android coordinate space).
        camera.setToOrtho(true, width, height);
        camera.update();

        if (periphery instanceof RenderPeriphery) {
            ((RenderPeriphery) periphery).resize(width, height, camera.combined);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (batch == null) {
            return;
        }

        camera.update();
        if (gameStarted && periphery != null) {
            renderGame();
        } else if (!gameStarted && launcher != null) {
            renderLauncher();
        }

        if (exitAfterFrames > 0 && ++frames >= exitAfterFrames) {
            if (screenshotPath != null) {
                Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0,
                        Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                flipPixmapVertically(pixmap);
                PixmapIO.writePNG(new FileHandle(screenshotPath), pixmap);
                pixmap.dispose();
                Gdx.app.log("framework2d", "smoke: screenshot written to " + screenshotPath + ";");
            }
            Gdx.app.log("framework2d", "smoke: rendered " + frames + " frames; exiting;");
            Gdx.app.exit();
        }
    }

    private void renderGame() {
        if (dragSmoke) {
            driveDragSmoke();
        }
        if (periphery instanceof RenderPeriphery) {
            ((RenderPeriphery) periphery).renderFrame(camera.combined);
        } else {
            canvas.setFrame(batch, camera.combined, font);
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            periphery.draw(canvas);
            batch.end();
        }

        if (backToMenuButton != null) {
            canvas.setFrame(batch, camera.combined, font);
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            backToMenuButton.draw(canvas);
            batch.end();
        }

        if (backSmokeAfterFrames > 0 && frames >= backSmokeAfterFrames) {
            backSmokeAfterFrames = 0;
            if (backToMenuButton != null && gameInputProcessor != null) {
                int x = (int) backToMenuButton.centerX();
                int y = (int) backToMenuButton.centerY();
                Gdx.app.log("framework2d", "smoke: tapping the in-game back button;");
                gameInputProcessor.touchDown(x, y, 0, 0);
                gameInputProcessor.touchUp(x, y, 0, 0);
            }
        }
    }

    /**
     * Input for the running game: a gesture that starts on the back button is fully owned here
     * (the engine never sees a half-tap under the button), everything else goes to the map's
     * touch pipeline. With no touch engine in the map the bridge is null and only the button acts.
     */
    private final class GameInputProcessor extends com.badlogic.gdx.InputAdapter {

        private final TouchBridge bridge;
        private int buttonPointer = -1;

        GameInputProcessor(TouchBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (isOnBackButton(screenX, screenY)) {
                buttonPointer = pointer;
                backToMenuButton.setPressed(true);
                return true;
            }
            return bridge != null && bridge.touchDown(screenX, screenY, pointer, button);
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (pointer == buttonPointer) {
                return true;
            }
            return bridge != null && bridge.touchDragged(screenX, screenY, pointer);
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (pointer == buttonPointer) {
                buttonPointer = -1;
                backToMenuButton.setPressed(false);
                if (isOnBackButton(screenX, screenY)) {
                    returnToMenu();
                }
                return true;
            }
            return bridge != null && bridge.touchUp(screenX, screenY, pointer, button);
        }

        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
            if (pointer == buttonPointer) {
                buttonPointer = -1;
                backToMenuButton.setPressed(false);
                return true;
            }
            return bridge != null && bridge.touchCancelled(screenX, screenY, pointer, button);
        }

        private boolean isOnBackButton(int screenX, int screenY) {
            return backToMenuButton != null && backToMenuButton.contains(screenX, screenY);
        }
    }

    private void renderLauncher() {
        canvas.setFrame(batch, camera.combined, font);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        launcher.draw(canvas, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
        launcher.update();
    }

    /**
     * {@code Pixmap.createFromFrameBuffer} copies GL rows as-is, i.e. bottom row first, so the
     * written PNG is a vertical mirror of the window. Flip it back so smoke shots match what
     * the window actually shows.
     */
    private static void flipPixmapVertically(Pixmap pixmap) {
        java.nio.ByteBuffer pixels = pixmap.getPixels();
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        int rowBytes = width * 4;
        byte[] top = new byte[rowBytes];
        byte[] bottom = new byte[rowBytes];
        for (int row = 0; row < height / 2; row++) {
            int topOffset = row * rowBytes;
            int bottomOffset = (height - 1 - row) * rowBytes;
            pixels.position(topOffset);
            pixels.get(top, 0, rowBytes);
            pixels.position(bottomOffset);
            pixels.get(bottom, 0, rowBytes);
            pixels.position(topOffset);
            pixels.put(bottom, 0, rowBytes);
            pixels.position(bottomOffset);
            pixels.put(top, 0, rowBytes);
        }
        pixels.position(0);
    }

    private void driveDragSmoke() {
        if (touch == null) {
            return;
        }
        if (frames == 5) {
            touch.toHandleEvent(new MotionEvent(MotionEvent.ACTION_DOWN)
                    .addActionPointer(30, 90, 0));
        } else if (frames >= 10 && frames <= 30) {
            float t = (frames - 10) / 20f;
            touch.toHandleEvent(new MotionEvent(MotionEvent.ACTION_MOVE)
                    .addActionPointer(30 + 270 * t, 90 + 360 * t, 0));
        } else if (frames == 35) {
            touch.toHandleEvent(new MotionEvent(MotionEvent.ACTION_UP)
                    .addActionPointer(300, 450, 0));
        }
    }

    @Override
    public void dispose() {
        if (periphery instanceof RenderPeriphery) {
            ((RenderPeriphery) periphery).dispose();
        }
        if (launcher != null) {
            launcher.dispose();
        }
        if (backToMenuButton != null) {
            backToMenuButton.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
