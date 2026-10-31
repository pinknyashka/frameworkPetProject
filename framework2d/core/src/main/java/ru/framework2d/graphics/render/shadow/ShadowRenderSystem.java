package ru.framework2d.graphics.render.shadow;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.data.GeometryBlock;
import ru.framework2d.data.GeometryVerticalCylinder;
import ru.framework2d.graphics.draw.shadows.OutputGraphicalShadow;
import ru.framework2d.graphics.render.LightState;
import ru.framework2d.graphics.render.RenderFrame;
import ru.framework2d.graphics.render.RenderServices;
import ru.framework2d.graphics.render.RenderSystem;
import ru.framework2d.graphics.render.RenderTransform;

public class ShadowRenderSystem extends RenderSystem {

    private static final int CIRCLE_SEGMENTS = 20;
    private static final int MAX_POINTS = CIRCLE_SEGMENTS * 2 + 8;

    private static final float MIN_ELEVATION_RATIO = 1e-3f;
    private static final float MAX_STRETCH = 10.0f;
    private static final float MAX_SHADOW_LENGTH = 300.0f;

    private final ArrayList<OutputGraphicalShadow> shadows = new ArrayList<OutputGraphicalShadow>();
    private final RenderTransform transform = new RenderTransform();

    private final float[] vertices = new float[8192 * 3];
    private int vertexFloats;

    private final float[] pointsX = new float[MAX_POINTS];
    private final float[] pointsY = new float[MAX_POINTS];
    private int pointCount;

    private final int[] hull = new int[MAX_POINTS];
    private int hullCount;

    @SuppressWarnings("unchecked")
    public ShadowRenderSystem() {
        super(OutputGraphicalShadow.class);
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);
        if (component instanceof OutputGraphicalShadow) {
            OutputGraphicalShadow shadow = (OutputGraphicalShadow) component;
            if (!shadow.entity.isPrototype && !shadows.contains(shadow)) {
                shadows.add(shadow);
            }
        }
        return component;
    }

    @Override
    public void collect(RenderFrame frame, RenderServices services) {
    }

    @Override
    public void render(RenderFrame frame, RenderServices services) {
        LightState light = frame.light(0);
        if (light == null || !light.castsShadows || shadows.isEmpty()) {
            return;
        }

        vertexFloats = 0;
        float maxSoftness = 1.0f;
        float maxOpacity = 0.0f;

        for (int i = 0; i < shadows.size(); i++) {
            OutputGraphicalShadow shadow = shadows.get(i);
            if (!shadow.castShadows.value || shadow.geometry == null) {
                continue;
            }
            if (!transform.compute(shadow)) {
                continue;
            }
            maxSoftness = Math.max(maxSoftness, shadow.softness.value);
            maxOpacity = Math.max(maxOpacity, shadow.opacity.value);

            if (shadow.geometry instanceof GeometryVerticalCylinder) {
                appendCylinder(light, shadow, (GeometryVerticalCylinder) shadow.geometry);
            } else if (shadow.geometry instanceof GeometryBlock) {
                appendBlock(light, shadow, (GeometryBlock) shadow.geometry);
            }
        }

        if (vertexFloats == 0) {
            return;
        }
        if (!services.beginShadowPass()) {
            return;
        }

        services.drawShadowTriangles(vertices, vertexFloats, frame.projection);
        services.endShadowPass(maxSoftness);

        frame.shadowTexture = services.shadowTexture();
        frame.shadowStrength = Math.min(1.0f, maxOpacity);
    }

    private void appendCylinder(LightState light, OutputGraphicalShadow shadow, GeometryVerticalCylinder cylinder) {
        float radius = cylinder.radius.value;
        float height = cylinder.height.value;
        if (radius <= 0 || height <= 0) {
            return;
        }

        pointCount = 0;
        addCircle(transform.x, transform.y, radius);

        float offsetX = shadowOffsetX(light, height, shadow);
        float offsetY = shadowOffsetY(light, height, shadow);
        addCircle(transform.x + offsetX, transform.y + offsetY, radius);

        emitHull();
    }

    private void appendBlock(LightState light, OutputGraphicalShadow shadow, GeometryBlock block) {
        float halfWidth = block.width.value * 0.5f;
        float halfDepth = block.depth.value * 0.5f;
        float height = block.height.value;
        if (halfWidth <= 0 || halfDepth <= 0 || height <= 0) {
            return;
        }

        float[] localX = { -halfWidth, -halfWidth, halfWidth, halfWidth };
        float[] localY = { -halfDepth, halfDepth, halfDepth, -halfDepth };
        float cos = (float) Math.cos(transform.angle);
        float sin = (float) Math.sin(transform.angle);

        float offsetX = shadowOffsetX(light, height, shadow);
        float offsetY = shadowOffsetY(light, height, shadow);

        pointCount = 0;
        for (int i = 0; i < 4; i++) {
            float rx = cos * localX[i] - sin * localY[i];
            float ry = sin * localX[i] + cos * localY[i];
            addPoint(transform.x + rx, transform.y + ry);
            addPoint(transform.x + offsetX + rx, transform.y + offsetY + ry);
        }

        emitHull();
    }

    private float shadowOffsetX(LightState light, float height, OutputGraphicalShadow shadow) {
        float offset = shadowLength(light, height, shadow);
        return awayX(light) * offset;
    }

    private float shadowOffsetY(LightState light, float height, OutputGraphicalShadow shadow) {
        float offset = shadowLength(light, height, shadow);
        return awayY(light) * offset;
    }

    private float awayX(LightState light) {
        return awayX(transform.x, transform.y, light.position.x, light.position.y);
    }

    private float awayY(LightState light) {
        return awayY(transform.x, transform.y, light.position.x, light.position.y);
    }

    private float shadowLength(LightState light, float height, OutputGraphicalShadow shadow) {
        return shadowLength(transform.x, transform.y, light.position.x, light.position.y,
                light.position.z, height, shadow.length.value);
    }

    static float awayX(float casterX, float casterY, float lightX, float lightY) {
        float dx = casterX - lightX;
        float dy = casterY - lightY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance > 0.0001f ? dx / distance : 0.0f;
    }

    static float awayY(float casterX, float casterY, float lightX, float lightY) {
        float dx = casterX - lightX;
        float dy = casterY - lightY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance > 0.0001f ? dy / distance : 0.0f;
    }

    static float shadowLength(float casterX, float casterY, float lightX, float lightY,
            float lightZ, float height, float minLength) {
        float dx = casterX - lightX;
        float dy = casterY - lightY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        float elevation = lightZ - height;
        float stretch;
        if (elevation > MIN_ELEVATION_RATIO) {
            stretch = lightZ / elevation;
            if (stretch > MAX_STRETCH) {
                stretch = MAX_STRETCH;
            }
            if (stretch < 1.0f) {
                stretch = 1.0f;
            }
        } else {
            stretch = MAX_STRETCH;
        }

        float length = (stretch - 1.0f) * distance;
        if (length < minLength) {
            length = minLength;
        }
        if (length > MAX_SHADOW_LENGTH) {
            length = MAX_SHADOW_LENGTH;
        }
        return length;
    }

    private void addCircle(float centerX, float centerY, float radius) {
        float step = (float) (Math.PI * 2.0 / CIRCLE_SEGMENTS);
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle = i * step;
            addPoint(centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius);
        }
    }

    private void emitHull() {
        if (pointCount < 3) {
            return;
        }
        buildHull();
        for (int i = 1; i + 1 < hullCount; i++) {
            addVertex(pointsX[hull[0]], pointsY[hull[0]]);
            addVertex(pointsX[hull[i]], pointsY[hull[i]]);
            addVertex(pointsX[hull[i + 1]], pointsY[hull[i + 1]]);
        }
    }

    private void buildHull() {
        hullCount = 0;
        int start = 0;
        for (int i = 1; i < pointCount; i++) {
            if (pointsY[i] < pointsY[start]
                    || (pointsY[i] == pointsY[start] && pointsX[i] < pointsX[start])) {
                start = i;
            }
        }

        int current = start;
        int guard = 0;
        do {
            hull[hullCount++] = current;
            int next = (current + 1) % pointCount;
            for (int i = 0; i < pointCount; i++) {
                if (i == current) {
                    continue;
                }
                float cross = cross(current, next, i);
                if (cross < 0) {
                    next = i;
                } else if (cross == 0 && distanceSquared(current, i) > distanceSquared(current, next)) {
                    next = i;
                }
            }
            current = next;
        } while (current != start && hullCount < hull.length && ++guard <= pointCount + 1);
    }

    private float cross(int a, int b, int c) {
        return (pointsX[b] - pointsX[a]) * (pointsY[c] - pointsY[a])
                - (pointsY[b] - pointsY[a]) * (pointsX[c] - pointsX[a]);
    }

    private float distanceSquared(int a, int b) {
        float dx = pointsX[b] - pointsX[a];
        float dy = pointsY[b] - pointsY[a];
        return dx * dx + dy * dy;
    }

    private void addPoint(float x, float y) {
        if (pointCount >= pointsX.length) {
            return;
        }
        pointsX[pointCount] = x;
        pointsY[pointCount] = y;
        pointCount++;
    }

    private void addVertex(float x, float y) {
        if (vertexFloats + 3 > vertices.length) {
            return;
        }
        vertices[vertexFloats++] = x;
        vertices[vertexFloats++] = y;
        vertices[vertexFloats++] = 0;
    }
}
