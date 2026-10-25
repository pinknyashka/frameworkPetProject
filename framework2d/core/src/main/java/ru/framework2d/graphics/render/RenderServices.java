package ru.framework2d.graphics.render;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

import ru.framework2d.compat.Bitmap;
import ru.framework2d.compat.Canvas;
import ru.framework2d.core.Engine;
import ru.framework2d.compat.Log;

/**
 * Owns the libGDX objects used by the HQ render pass. Nothing is allocated in
 * the constructor; every method that touches GL calls {@link #ensureInitialized()}.
 */
public class RenderServices {

    private static final String LOG_TAG = "RenderHQ";

    private SpriteBatch batch;
    private SpriteBatch uiBatch;
    private BitmapFont font;
    private Canvas textCanvas;

    private ShaderProgram sceneShader;
    private ShaderProgram shadowShader;
    private ShaderProgram blurShader;

    private Mesh shadowMesh;
    private Mesh fullscreenMesh;

    private FrameBuffer shadowMask;
    private FrameBuffer shadowBlur;
    private Texture emptyTexture;

    private final Matrix4 baseProjection = new Matrix4();
    private final HashMap<String, Texture> normalMaps = new HashMap<String, Texture>();

    private int width = 1;
    private int height = 1;
    private float shadowScale = 0.5f;
    private boolean shadowsEnabled = true;
    private boolean initialized;
    private boolean shadersFailed;

    private int currentMaterialState = -1;
    private Texture currentNormalMap;
    private boolean currentReceiveShadows = true;

    private final float[] lightPositions = new float[12];
    private final float[] lightColors = new float[12];
    private final float[] lightParams = new float[12];

    public void configure(float shadowBufferScale, boolean enableShadows) {
        if (shadowBufferScale > 0.1f && shadowBufferScale <= 2.0f) {
            shadowScale = shadowBufferScale;
        }
        shadowsEnabled = enableShadows;
    }

    public Matrix4 baseProjection() {
        return baseProjection;
    }

    public void setBaseProjection(Matrix4 projection) {
        if (projection != null) {
            baseProjection.set(projection);
        }
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        if (Gdx.gl == null) {
            return;
        }

        batch = new SpriteBatch();
        uiBatch = new SpriteBatch();
        font = new BitmapFont();
        textCanvas = new Canvas();

        sceneShader = loadShader("shaders/hq_scene.vert", "shaders/hq_scene.frag", "hq_scene");
        shadowShader = loadShader("shaders/hq_shadow.vert", "shaders/hq_shadow.frag", "hq_shadow");
        blurShader = loadShader("shaders/hq_blur.vert", "shaders/hq_blur.frag", "hq_blur");
        shadersFailed = sceneShader == null;

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(0, 0, 0, 1);
        pixel.fill();
        emptyTexture = new Texture(pixel);
        emptyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixel.dispose();

        shadowMesh = new Mesh(false, 8192, 0, VertexAttribute.Position());
        fullscreenMesh = new Mesh(false, 4, 6, VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        fullscreenMesh.setVertices(new float[] {
                -1, -1, 0, 0, 0,
                 1, -1, 0, 1, 0,
                 1,  1, 0, 1, 1,
                -1,  1, 0, 0, 1
        });
        fullscreenMesh.setIndices(new short[] { 0, 1, 2, 2, 3, 0 });

        initialized = true;
        createFramebuffers();
    }

    private ShaderProgram loadShader(String vertexPath, String fragmentPath, String name) {
        FileHandle vertexFile = Gdx.files.internal(vertexPath);
        FileHandle fragmentFile = Gdx.files.internal(fragmentPath);
        if (!vertexFile.exists() || !fragmentFile.exists()) {
            Log.e(LOG_TAG, "Missing shader files for " + name);
            return null;
        }

        ShaderProgram.pedantic = false;
        ShaderProgram program = new ShaderProgram(vertexFile, fragmentFile);
        if (!program.isCompiled()) {
            Log.e(LOG_TAG, name + " failed to compile: " + program.getLog());
            program.dispose();
            return null;
        }
        return program;
    }

    public void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        ensureInitialized();
        createFramebuffers();
    }

    private void createFramebuffers() {
        if (!initialized || Gdx.gl == null) {
            return;
        }

        int newWidth = Math.max(1, (int) (width * shadowScale));
        int newHeight = Math.max(1, (int) (height * shadowScale));

        if (shadowMask != null && shadowMask.getWidth() == newWidth && shadowMask.getHeight() == newHeight) {
            return;
        }

        if (shadowMask != null) {
            shadowMask.dispose();
            shadowMask = null;
        }
        if (shadowBlur != null) {
            shadowBlur.dispose();
            shadowBlur = null;
        }

        shadowMask = new FrameBuffer(Pixmap.Format.RGBA8888, newWidth, newHeight, false);
        shadowBlur = new FrameBuffer(Pixmap.Format.RGBA8888, newWidth, newHeight, false);
    }

    public void beginScene(RenderFrame frame) {
        ensureInitialized();
        if (batch == null) {
            return;
        }

        currentMaterialState = -1;
        currentNormalMap = null;
        currentReceiveShadows = true;

        if (!shadersFailed) {
            batch.setShader(sceneShader);
        } else {
            batch.setShader(null);
        }
        batch.setProjectionMatrix(frame.projection);
        batch.begin();

        if (!shadersFailed) {
            applyLightUniforms(frame);
            sceneShader.setUniformf("u_ambient", frame.ambient);
            sceneShader.setUniformf("u_screenSize", frame.screenWidth, frame.screenHeight);
            sceneShader.setUniformf("u_shadowStrength", frame.shadowStrength);
            sceneShader.setUniformi("u_shadowsEnabled", frame.shadowTexture != null ? 1 : 0);
            sceneShader.setUniformi("u_shadowMap", 1);
            sceneShader.setUniformi("u_normalMap", 2);
            sceneShader.setUniformi("u_receiveShadows", 1);

            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
            Texture shadow = frame.shadowTexture != null ? frame.shadowTexture : emptyTexture;
            shadow.bind();
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        }
    }

    private void applyLightUniforms(RenderFrame frame) {
        int count = Math.min(4, frame.lightCount());
        for (int i = 0; i < 4; i++) {
            lightPositions[i * 3] = 0;
            lightPositions[i * 3 + 1] = 0;
            lightPositions[i * 3 + 2] = 1;
            lightColors[i * 3] = 0;
            lightColors[i * 3 + 1] = 0;
            lightColors[i * 3 + 2] = 0;
            lightParams[i * 3] = 1000.0f;
            lightParams[i * 3 + 1] = 1.0f;
            lightParams[i * 3 + 2] = 0.3f;
        }

        for (int i = 0; i < count; i++) {
            LightState light = frame.light(i);
            lightPositions[i * 3] = light.position.x;
            lightPositions[i * 3 + 1] = light.position.y;
            lightPositions[i * 3 + 2] = light.position.z;
            lightColors[i * 3] = light.red * light.intensity;
            lightColors[i * 3 + 1] = light.green * light.intensity;
            lightColors[i * 3 + 2] = light.blue * light.intensity;
            lightParams[i * 3] = Math.max(1.0f, light.radius);
            lightParams[i * 3 + 1] = light.intensity;
            lightParams[i * 3 + 2] = light.diffuse;
        }

        sceneShader.setUniformi("u_lightCount", count);
        sceneShader.setUniform3fv("u_lightPosition", lightPositions, 0, 4);
        sceneShader.setUniform3fv("u_lightColor", lightColors, 0, 4);
        sceneShader.setUniform3fv("u_lightParams", lightParams, 0, 4);
    }

    public void drawItem(RenderItem item) {
        if (batch == null || item.texture == null) {
            return;
        }

        batch.setColor(item.tintR, item.tintG, item.tintB, item.alpha);

        if (!shadersFailed) {
            int state = item.deriveNormals ? 1 : 0;
            if (item.normalMap != null) {
                state |= 2;
            }
            if (state != currentMaterialState || item.normalMap != currentNormalMap
                    || item.receiveShadows != currentReceiveShadows) {
                batch.flush();
                applyMaterial(item);
                currentReceiveShadows = item.receiveShadows;
                sceneShader.setUniformi("u_receiveShadows", item.receiveShadows ? 1 : 0);
            }
        }

        batch.draw(item.texture,
                item.x - item.width * 0.5f, item.y - item.height * 0.5f,
                item.width * 0.5f, item.height * 0.5f,
                item.width, item.height,
                1.0f, 1.0f,
                (float) Math.toDegrees(item.angle),
                item.srcX, item.srcY, item.srcWidth, item.srcHeight,
                false, item.flipY);
    }

    private void applyMaterial(RenderItem item) {
        sceneShader.setUniformi("u_deriveNormals", item.deriveNormals ? 1 : 0);
        sceneShader.setUniformf("u_heightScale", item.heightScale);
        sceneShader.setUniformf("u_specular", item.specular);
        sceneShader.setUniformf("u_shininess", Math.max(1.0f, item.shininess));
        sceneShader.setUniformf("u_normalStep", item.deriveNormals ? 0.0035f : 0.001f);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE2);
        Texture normal = item.normalMap != null ? item.normalMap : emptyTexture;
        normal.bind();
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        sceneShader.setUniformi("u_hasNormalMap", item.normalMap != null ? 1 : 0);
        currentNormalMap = item.normalMap;
        currentMaterialState = (item.deriveNormals ? 1 : 0) | (item.normalMap != null ? 2 : 0);
    }

    public void endScene() {
        if (batch != null) {
            batch.end();
        }
        currentMaterialState = -1;
        currentNormalMap = null;
        currentReceiveShadows = true;
    }

    public SpriteBatch textBatch() {
        ensureInitialized();
        return uiBatch;
    }

    public Canvas textCanvas() {
        ensureInitialized();
        return textCanvas;
    }

    public BitmapFont font() {
        ensureInitialized();
        return font;
    }

    public boolean beginShadowPass() {
        ensureInitialized();
        if (!shadowsEnabled || shadowShader == null || shadowMask == null) {
            return false;
        }

        shadowMask.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        return true;
    }

    public void drawShadowTriangles(float[] vertices, int floatCount, Matrix4 projection) {
        if (shadowShader == null || shadowMesh == null || floatCount == 0) {
            return;
        }
        if (floatCount > shadowMesh.getMaxVertices() * 3) {
            return;
        }
        shadowShader.bind();
        shadowShader.setUniformMatrix("u_projTrans", projection);
        shadowMesh.setVertices(vertices, 0, floatCount);
        shadowMesh.render(shadowShader, GL20.GL_TRIANGLES);
    }

    public void endShadowPass(float softness) {
        if (shadowMask == null) {
            return;
        }
        shadowMask.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);

        if (blurShader == null || shadowBlur == null) {
            return;
        }

        float radius = Math.max(1.0f, softness);
        float offsetX = radius / Math.max(1, shadowMask.getWidth());
        float offsetY = radius / Math.max(1, shadowMask.getHeight());

        renderBlur(shadowMask.getColorBufferTexture(), shadowBlur, offsetX, 0);
        renderBlur(shadowBlur.getColorBufferTexture(), shadowMask, 0, offsetY);
    }

    private void renderBlur(Texture source, FrameBuffer target, float offsetX, float offsetY) {
        target.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        blurShader.bind();
        blurShader.setUniformf("u_offset", offsetX, offsetY);
        source.bind(0);
        blurShader.setUniformi("u_texture", 0);
        fullscreenMesh.render(blurShader, GL20.GL_TRIANGLES);

        target.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
    }

    public Texture shadowTexture() {
        if (shadowMask == null) {
            return null;
        }
        return shadowMask.getColorBufferTexture();
    }

    public void configure(Texture texture) {
        if (texture != null) {
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    public Texture normalMap(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        if (normalMaps.containsKey(name)) {
            return normalMaps.get(name);
        }
        Bitmap bitmap = Engine.getResources().getImage(name);
        Texture texture = bitmap != null ? bitmap.texture : null;
        configure(texture);
        normalMaps.put(name, texture);
        return texture;
    }

    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (uiBatch != null) {
            uiBatch.dispose();
            uiBatch = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (shadowMask != null) {
            shadowMask.dispose();
            shadowMask = null;
        }
        if (shadowBlur != null) {
            shadowBlur.dispose();
            shadowBlur = null;
        }
        if (sceneShader != null) {
            sceneShader.dispose();
            sceneShader = null;
        }
        if (shadowShader != null) {
            shadowShader.dispose();
            shadowShader = null;
        }
        if (blurShader != null) {
            blurShader.dispose();
            blurShader = null;
        }
        if (shadowMesh != null) {
            shadowMesh.dispose();
            shadowMesh = null;
        }
        if (fullscreenMesh != null) {
            fullscreenMesh.dispose();
            fullscreenMesh = null;
        }
        if (emptyTexture != null) {
            emptyTexture.dispose();
            emptyTexture = null;
        }
        textCanvas = null;
        normalMaps.clear();
        initialized = false;
    }
}
