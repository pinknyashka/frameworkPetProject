package ru.framework2d.graphics.render;

import com.badlogic.gdx.math.Vector3;

public class LightState {

    public final Vector3 position = new Vector3();
    public float red = 1.0f;
    public float green = 1.0f;
    public float blue = 1.0f;
    public float intensity = 1.0f;
    public float radius = 1000.0f;
    public float diffuse = 0.3f;
    public boolean castsShadows = true;

    public void reset() {
        position.set(0, 0, 0);
        red = 1.0f;
        green = 1.0f;
        blue = 1.0f;
        intensity = 1.0f;
        radius = 1000.0f;
        diffuse = 0.3f;
        castsShadows = true;
    }
}
