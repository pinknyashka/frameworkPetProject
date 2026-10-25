package ru.framework2d.graphics.render;

import com.badlogic.gdx.graphics.Texture;

public class RenderItem {

    public static final int KIND_SPRITE = 0;
    public static final int KIND_RELIEF = 1;
    public static final int KIND_TEXT = 2;

    public int kind;
    public float layer;
    public float x;
    public float y;
    public float z;
    public float angle;
    public float alpha = 1.0f;
    public float width;
    public float height;

    public Texture texture;
    public int srcX;
    public int srcY;
    public int srcWidth;
    public int srcHeight;
    public boolean flipY = true;

    public Texture normalMap;
    public boolean deriveNormals;
    public float heightScale = 1.0f;
    public float specular;
    public float shininess = 16.0f;
    public float tintR = 1.0f;
    public float tintG = 1.0f;
    public float tintB = 1.0f;
    public boolean receiveShadows = true;

    public String text;
    public int textColor;
    public float textSize = 14.0f;

    public void reset() {
        kind = KIND_SPRITE;
        layer = 0;
        x = 0;
        y = 0;
        z = 0;
        angle = 0;
        alpha = 1.0f;
        width = 0;
        height = 0;
        texture = null;
        srcX = 0;
        srcY = 0;
        srcWidth = 0;
        srcHeight = 0;
        flipY = true;
        normalMap = null;
        deriveNormals = false;
        heightScale = 1.0f;
        specular = 0;
        shininess = 16.0f;
        tintR = 1.0f;
        tintG = 1.0f;
        tintB = 1.0f;
        receiveShadows = true;
        text = null;
        textColor = 0;
        textSize = 14.0f;
    }
}
