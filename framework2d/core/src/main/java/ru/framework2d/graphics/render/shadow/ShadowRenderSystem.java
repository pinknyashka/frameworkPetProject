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

    private static final int CIRCLE_SEGMENTS = 24;

    private final ArrayList<OutputGraphicalShadow> shadows = new ArrayList<OutputGraphicalShadow>();
    private final RenderTransform transform = new RenderTransform();

    private final float[] vertices = new float[8192 * 3];
    private int vertexFloats;

    private final float[] pointsX = new float[16];
    private final float[] pointsY = new float[16];
    private int pointCount;

    private final int[] hull = new int[16];
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
                appendCylinder(shadow, light);
            } else if (shadow.geometry instanceof GeometryBlock) {
                appendBlock(shadow, light);
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

    private void appendCylinder(OutputGraphicalShadow shadow, LightState light) {
        GeometryVerticalCylinder cylinder = (GeometryVerticalCylinder) shadow.geometry;
        float topZ = transform.z + cylinder.height.value;
        if (cylinder.height.value <= 0 || topZ >= light.position.z - 0.001f) {
            return;
        }

        float topDx = transform.x - light.position.x;
        float topDy = transform.y - light.position.y;
        float topDz = topZ - light.position.z;
        float distanceToTop = (float) Math.sqrt(topDx * topDx + topDy * topDy + topDz * topDz);
        if (distanceToTop < 0.001f) {
            return;
        }

        float fullHeight = Math.abs(topDz) + cylinder.height.value;
        float ratio = fullHeight / Math.abs(topDz);
        float centerX = light.position.x + topDx * ratio;
        float centerY = light.position.y + topDy * ratio;
        float radius = cylinder.radius.value * ratio;
        if (radius <= 0) {
            return;
        }

        float step = (float) (Math.PI * 2.0 / CIRCLE_SEGMENTS);
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;
            addVertex(centerX, centerY);
            addVertex(centerX + (float) Math.cos(a1) * radius, centerY + (float) Math.sin(a1) * radius);
            addVertex(centerX + (float) Math.cos(a2) * radius, centerY + (float) Math.sin(a2) * radius);
        }
    }

    private void appendBlock(OutputGraphicalShadow shadow, LightState light) {
        GeometryBlock block = (GeometryBlock) shadow.geometry;
        float halfWidth = block.width.value * 0.5f;
        float halfDepth = block.depth.value * 0.5f;
        if (halfWidth <= 0 || halfDepth <= 0) {
            return;
        }

        float[] localX = { -halfWidth, -halfWidth, halfWidth, halfWidth };
        float[] localY = { -halfDepth, halfDepth, halfDepth, -halfDepth };
        float bottomZ = transform.z;
        float topZ = transform.z + block.height.value;
        float cos = (float) Math.cos(transform.angle);
        float sin = (float) Math.sin(transform.angle);

        pointCount = 0;
        for (int level = 0; level < 2; level++) {
            float z = level == 0 ? bottomZ : topZ;
            if (z >= light.position.z - 0.001f) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                float px = transform.x + cos * localX[i] - sin * localY[i];
                float py = transform.y + sin * localX[i] + cos * localY[i];
                appendProjectedPoint(light, px, py, z);
            }
        }

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

    private void appendProjectedPoint(LightState light, float px, float py, float pz) {
        if (pointCount >= pointsX.length) {
            return;
        }
        float dz = pz - light.position.z;
        if (dz > -0.0001f) {
            return;
        }
        float t = -light.position.z / dz;
        if (t <= 0) {
            return;
        }
        pointsX[pointCount] = light.position.x + (px - light.position.x) * t;
        pointsY[pointCount] = light.position.y + (py - light.position.y) * t;
        pointCount++;
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

    private void addVertex(float x, float y) {
        if (vertexFloats + 3 > vertices.length) {
            return;
        }
        vertices[vertexFloats++] = x;
        vertices[vertexFloats++] = y;
        vertices[vertexFloats++] = 0;
    }
}
