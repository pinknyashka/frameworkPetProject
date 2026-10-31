package ru.framework2d.graphics.render;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;

import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Log;
import ru.framework2d.compat.Paint;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.core.agents.EngineLocks;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.core.agents.LockBusyException;
import ru.framework2d.core.agents.LockMode;
import ru.framework2d.core.agents.ResourceLease;
import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.graphics.RenderPeriphery;
import ru.framework2d.touches.WorldPointer;

/**
 * HQ graphics bridge. Keeps the legacy engine/map schema but renders the frame
 * through a single SpriteBatch with a lighting/shadow shader pass.
 */
public class OutputRenderHQ extends Engine implements GraphicalPeriphery, RenderPeriphery {

    private static final String LOG_TAG = "RenderHQ";

    private final RenderServices services = new RenderServices();
    private final RenderFrame frame = new RenderFrame();
    private final Matrix4 baseProjection = new Matrix4();
    private final ArrayList<OutputGraphicalInterface> graphicals = new ArrayList<OutputGraphicalInterface>();

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean drawing;
    private long lastLockWarning;
    private HandlerRenderHQ handler;

    private float shadowBufferScale = 1.0f;
    private boolean shadowsEnabled = true;
    private boolean shadowSharp = true;

    @Override
    public void setOptions(TagOptions options) {
        for (int i = 0; i < options.lstPropertys.size(); i++) {
            Property property = options.lstPropertys.get(i);
            if (property.isNamed("shadowBufferScale")) {
                try {
                    shadowBufferScale = Float.parseFloat(property.data.toString());
                } catch (NumberFormatException e) {
                    Log.w(LOG_TAG, "Invalid shadowBufferScale: " + property.data);
                }
            } else if (property.isNamed("shadowSharp")) {
                shadowSharp = Boolean.parseBoolean(property.data.toString());
            } else if (property.isNamed("softShadows")) {
                shadowSharp = !Boolean.parseBoolean(property.data.toString());
            } else if (property.isNamed("shadows")) {
                shadowsEnabled = Boolean.parseBoolean(property.data.toString());
            } else if (property.isNamed("quality")) {
                if (property.data.toString().equalsIgnoreCase("low")) {
                    shadowsEnabled = false;
                }
            }
        }
        services.configure(shadowBufferScale, shadowsEnabled, shadowSharp);
    }

    @Override
    public void renderFrame(Matrix4 projection) {
        if (projection != null) {
            services.setBaseProjection(projection);
            baseProjection.set(projection);
        }
        if (Gdx.gl == null) {
            return;
        }

        ResourceLease lease;
        try {
            lease = EngineLocks.tryAcquire(EngineResource.GRAPHICS, LockMode.READ, "HQ render frame");
        } catch (LockBusyException e) {
            long now = System.currentTimeMillis();
            if (now - lastLockWarning > 2000L) {
                Log.w(LOG_TAG, "HQ draw skipped: " + e.describe());
                lastLockWarning = now;
            }
            return;
        }

        services.ensureInitialized();
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        services.resize(width, height);

        if (handler == null) {
            findHandler();
        }
        if (handler == null) {
            lease.close();
            return;
        }

        drawing = true;
        try {
            frame.begin(width, height);
            handler.renderFrame(frame, services);
        } finally {
            frame.end();
            drawing = false;
            lease.close();
        }

        enginesScheduler.overDraw();
    }

    private void findHandler() {
        for (int i = 0; i < lstSubEngines.size(); i++) {
            Engine engine = lstSubEngines.get(i);
            if (engine instanceof HandlerRenderHQ) {
                handler = (HandlerRenderHQ) engine;
                return;
            }
        }
        for (int i = 0; i < enginesManager.lstEngines.size(); i++) {
            Engine engine = enginesManager.lstEngines.get(i);
            if (engine instanceof HandlerRenderHQ) {
                handler = (HandlerRenderHQ) engine;
                return;
            }
        }
    }

    @Override
    public void resize(int width, int height, Matrix4 projection) {
        if (projection != null) {
            baseProjection.set(projection);
            services.setBaseProjection(projection);
        }
        services.resize(width, height);
    }

    @Override
    public void dispose() {
        services.dispose();
    }

    @Override
    public void draw(Canvas c) {
        renderFrame(baseProjection);
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    @Override
    public void setDrawState(boolean drawInProgress) {
        drawing = drawInProgress;
    }

    @Override
    public boolean isDrawInProgress() {
        return drawing;
    }

    @Override
    public Canvas getCanvas() {
        return null;
    }

    @Override
    public ArrayList<OutputGraphicalInterface> getListGraphicals() {
        return graphicals;
    }

    @Override
    public void addGraphical(OutputGraphicalInterface graphical) {
        if (graphical.entity == null || graphical.entity.isPrototype) {
            return;
        }

        if (graphical.entity.parent != null && !graphical.entity.parent.lstGraphicals.isEmpty()) {
            int lastGraphicalNo = graphical.entity.parent.lstGraphicals.size() - 1;
            OutputGraphicalInterface lastGraphical = graphical.entity.parent.lstGraphicals.get(lastGraphicalNo);

            if (!lastGraphical.lstChildren.contains(graphical)) {
                lastGraphical.lstChildren.add(graphical);
            }
            graphical.parentGraphical = lastGraphical;
        } else if (!graphicals.contains(graphical) && !(graphical instanceof WorldPointer)) {
            graphicals.add(graphical);
        }
    }

    @Override
    public void update() {
        setWorkState(true);
    }

    public RenderServices services() {
        return services;
    }
}
