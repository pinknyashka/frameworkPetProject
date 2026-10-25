package ru.framework2d.graphics.render.camera;

import java.util.ArrayList;

import com.badlogic.gdx.math.Matrix4;

import ru.framework2d.core.Component;
import ru.framework2d.graphics.cameras.OutputGraphicalCamera;
import ru.framework2d.graphics.render.RenderFrame;
import ru.framework2d.graphics.render.RenderServices;
import ru.framework2d.graphics.render.RenderSystem;

public class CameraRenderSystem extends RenderSystem {

    private final ArrayList<OutputGraphicalCamera> cameras = new ArrayList<OutputGraphicalCamera>();
    private final Matrix4 cameraMatrix = new Matrix4();

    @SuppressWarnings("unchecked")
    public CameraRenderSystem() {
        super(OutputGraphicalCamera.class);
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);
        if (component instanceof OutputGraphicalCamera) {
            OutputGraphicalCamera camera = (OutputGraphicalCamera) component;
            if (!camera.entity.isPrototype && !cameras.contains(camera)) {
                cameras.add(camera);
            }
        }
        return component;
    }

    @Override
    public void collect(RenderFrame frame, RenderServices services) {
        OutputGraphicalCamera camera = null;
        for (int i = 0; i < cameras.size(); i++) {
            if (cameras.get(i).isEnabled.value) {
                camera = cameras.get(i);
                break;
            }
        }

        if (camera == null) {
            frame.projection.set(services.baseProjection());
            frame.cameraX = 0;
            frame.cameraY = 0;
            frame.zoom = 1.0f;
            frame.worldLeft = 0;
            frame.worldTop = 0;
            frame.worldRight = frame.screenWidth;
            frame.worldBottom = frame.screenHeight;
            return;
        }

        float cameraX = (float) camera.position.x.value + (float) camera.localPos.x.value;
        float cameraY = (float) camera.position.y.value + (float) camera.localPos.y.value;
        float eyeX = (float) camera.eyePos.x.value;
        float eyeY = (float) camera.eyePos.y.value;
        float zoom = camera.zoom.value;
        if (zoom == 0) {
            zoom = 1.0f;
        }

        float cameraAngle = 0;
        if (camera.isRotatable.value) {
            cameraAngle += camera.position.getAlpha();
        }
        if (camera.isLocalRotatable.value) {
            cameraAngle += camera.localPos.getAlpha();
        }

        cameraMatrix.idt()
                .translate(cameraX, cameraY, 0)
                .rotate(0, 0, 1, (float) Math.toDegrees(cameraAngle))
                .scale(zoom, zoom, 1)
                .translate(-eyeX, -eyeY, 0);

        frame.cameraX = cameraX;
        frame.cameraY = cameraY;
        frame.zoom = zoom;
        frame.projection.set(services.baseProjection()).mul(cameraMatrix);
        updateWorldBounds(frame, cameraX, cameraY, eyeX, eyeY, zoom, cameraAngle);
    }

    private void updateWorldBounds(RenderFrame frame, float cameraX, float cameraY,
            float eyeX, float eyeY, float zoom, float cameraAngle) {
        float cos = (float) Math.cos(-cameraAngle);
        float sin = (float) Math.sin(-cameraAngle);

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            float screenX = (i == 1 || i == 2) ? frame.screenWidth : 0;
            float screenY = (i >= 2) ? frame.screenHeight : 0;
            float localX = (screenX - cameraX) / zoom;
            float localY = (screenY - cameraY) / zoom;
            float worldX = eyeX + cos * localX - sin * localY;
            float worldY = eyeY + sin * localX + cos * localY;
            minX = Math.min(minX, worldX);
            minY = Math.min(minY, worldY);
            maxX = Math.max(maxX, worldX);
            maxY = Math.max(maxY, worldY);
        }

        frame.worldLeft = minX;
        frame.worldTop = minY;
        frame.worldRight = maxX;
        frame.worldBottom = maxY;
    }
}
