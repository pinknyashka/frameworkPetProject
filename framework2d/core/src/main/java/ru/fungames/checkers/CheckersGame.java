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
import ru.framework2d.core.Engine;
import ru.framework2d.core.GameCore;
import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.touches.TouchPeriphery;

import ru.framework2d.gdx.TouchBridge;

public class CheckersGame extends Game {

    public static final String DEFAULT_MAP = "checkers_tree";

    /** Map name (without .xml) loaded by {@link #create()}. */
    public String mapName = DEFAULT_MAP;

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

    @Override
    public void create() {
        core = new GameCore();
        core.createObjectsModel(mapName == null ? DEFAULT_MAP : mapName);

        for (Engine engine : core.enginesManager.lstEngines) {
            if (engine instanceof GraphicalPeriphery) {
                periphery = (GraphicalPeriphery) engine;
            } else if (engine instanceof TouchPeriphery) {
                touch = (TouchPeriphery) engine;
            }
        }

        batch = new SpriteBatch();
        font = new BitmapFont();
        canvas = new Canvas();

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (touch != null) {
            Gdx.input.setInputProcessor(new TouchBridge(touch));
        }

        Gdx.app.log("framework2d", "engine built; periphery=" + periphery + "; touch=" + touch);
    }

    @Override
    public void resize(int width, int height) {
        if (camera == null) {
            return;
        }
        // y-down projection, top-left origin (matches the engine's Android coordinate space).
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.10f, 0.11f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (periphery == null || batch == null) {
            return;
        }

        camera.update();
        canvas.setFrame(batch, camera.combined, font);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        periphery.draw(canvas);
        batch.end();

        if (exitAfterFrames > 0 && ++frames >= exitAfterFrames) {
            if (screenshotPath != null) {
                Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0,
                        Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                PixmapIO.writePNG(new FileHandle(screenshotPath), pixmap);
                pixmap.dispose();
                Gdx.app.log("framework2d", "smoke: screenshot written to " + screenshotPath + ";");
            }
            Gdx.app.log("framework2d", "smoke: rendered " + frames + " frames; exiting;");
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
