package ru.framework2d.graphics.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;

public class RenderFrame {

    private static final int MAX_LIGHTS = 4;

    private final ArrayList<RenderItem> items = new ArrayList<RenderItem>();
    private final ArrayList<LightState> lights = new ArrayList<LightState>();
    private int itemCount;
    private int lightCount;

    public final Matrix4 projection = new Matrix4();
    public float screenWidth;
    public float screenHeight;

    public float worldLeft;
    public float worldTop;
    public float worldRight;
    public float worldBottom;

    public float cameraX;
    public float cameraY;
    public float zoom = 1.0f;
    public float ambient = 1.0f;

    public Texture shadowTexture;
    public float shadowStrength = 0.0f;

    public RenderFrame() {
        for (int i = 0; i < MAX_LIGHTS; i++) {
            lights.add(new LightState());
        }
    }

    public void begin(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        itemCount = 0;
        lightCount = 0;
        cameraX = 0;
        cameraY = 0;
        zoom = 1.0f;
        ambient = 1.0f;
        worldLeft = 0;
        worldTop = 0;
        worldRight = width;
        worldBottom = height;
        shadowTexture = null;
        shadowStrength = 0.0f;
        projection.idt();
    }

    public void end() {
        itemCount = 0;
        lightCount = 0;
        shadowTexture = null;
    }

    public RenderItem obtainItem() {
        if (itemCount == items.size()) {
            items.add(new RenderItem());
        }
        RenderItem item = items.get(itemCount++);
        item.reset();
        return item;
    }

    public LightState obtainLight() {
        if (lightCount >= MAX_LIGHTS) {
            return null;
        }
        LightState light = lights.get(lightCount++);
        light.reset();
        return light;
    }

    public List<RenderItem> items() {
        return items.subList(0, itemCount);
    }

    public List<LightState> lights() {
        return lights.subList(0, lightCount);
    }

    public int lightCount() {
        return lightCount;
    }

    public LightState light(int index) {
        if (index < 0 || index >= lightCount) {
            return null;
        }
        return lights.get(index);
    }

    public void sortItems() {
        Collections.sort(items.subList(0, itemCount), new Comparator<RenderItem>() {
            @Override
            public int compare(RenderItem first, RenderItem second) {
                if (first.layer < second.layer) {
                    return -1;
                }
                if (first.layer > second.layer) {
                    return 1;
                }
                return 0;
            }
        });
    }

    public boolean isVisible(float x, float y, float radius) {
        return x + radius >= worldLeft && x - radius <= worldRight
                && y + radius >= worldTop && y - radius <= worldBottom;
    }
}
