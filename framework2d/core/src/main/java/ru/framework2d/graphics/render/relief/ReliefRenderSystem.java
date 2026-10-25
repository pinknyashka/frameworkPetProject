package ru.framework2d.graphics.render.relief;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Texture;

import ru.framework2d.compat.Rect;
import ru.framework2d.core.Component;
import ru.framework2d.core.Engine;
import ru.framework2d.graphics.draw.shadows.OutputGraphicalRelief;
import ru.framework2d.graphics.render.LightState;
import ru.framework2d.graphics.render.RenderFrame;
import ru.framework2d.graphics.render.RenderItem;
import ru.framework2d.graphics.render.RenderServices;
import ru.framework2d.graphics.render.RenderSystem;
import ru.framework2d.graphics.render.RenderTransform;

public class ReliefRenderSystem extends RenderSystem {

    private final ArrayList<OutputGraphicalRelief> reliefs = new ArrayList<OutputGraphicalRelief>();
    private final RenderTransform transform = new RenderTransform();

    @SuppressWarnings("unchecked")
    public ReliefRenderSystem() {
        super(OutputGraphicalRelief.class);
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);
        if (component instanceof OutputGraphicalRelief) {
            OutputGraphicalRelief relief = (OutputGraphicalRelief) component;
            if (relief.reliefSprite == null && relief.reliefName.value != null
                    && relief.reliefName.value.length() > 0) {
                relief.reliefSprite = Engine.resources.getSprite(relief.reliefName.value,
                        relief.spriteCollumnsNum.value, relief.spriteRowsNum.value);
            }
            if (!relief.entity.isPrototype && !reliefs.contains(relief)) {
                reliefs.add(relief);
            }
        }
        return component;
    }

    @Override
    public void collect(RenderFrame frame, RenderServices services) {
        LightState light = frame.light(0);

        for (int i = 0; i < reliefs.size(); i++) {
            OutputGraphicalRelief relief = reliefs.get(i);
            if (relief.reliefSprite == null || relief.reliefSprite.spriteImage == null
                    || relief.reliefSprite.spriteImage.texture == null
                    || relief.reliefSprite.lstSpritesRects.isEmpty()) {
                continue;
            }
            if (!transform.compute(relief)) {
                continue;
            }

            float width = (float) relief.size.x.value;
            float height = (float) relief.size.y.value;
            if (width == 0 || height == 0) {
                continue;
            }

            float lightAngle = 0;
            if (light != null) {
                float dx = light.position.x - transform.x;
                float dy = light.position.y - transform.y;
                float dz = light.position.z - transform.z;
                float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length > 0.001f) {
                    lightAngle = (float) Math.asin(dz / length);
                }
            }

            int maxFrame = relief.reliefSprite.lstSpritesRects.size() - 1;
            int reliefNo = 2 + Math.abs(Math.round(
                    (relief.spriteCollumnsNum.value * relief.spriteRowsNum.value - 3)
                            * (lightAngle * 2.0f) / (float) Math.PI));
            if (reliefNo < 0) {
                reliefNo = 0;
            } else if (reliefNo > maxFrame) {
                reliefNo = maxFrame;
            }
            Rect source = relief.reliefSprite.lstSpritesRects.get(reliefNo);

            float cullRadius = Math.max(width, height) * 0.75f + 4.0f;
            if (relief.cullable.value && !frame.isVisible(transform.x, transform.y, cullRadius)) {
                continue;
            }

            Texture texture = relief.reliefSprite.spriteImage.texture;
            services.configure(texture);

            RenderItem item = frame.obtainItem();
            item.kind = RenderItem.KIND_RELIEF;
            item.layer = relief.layer.value;
            item.x = transform.x;
            item.y = transform.y;
            item.z = transform.z;
            item.angle = transform.angle;
            item.alpha = transform.alpha * 0.75f;
            item.width = width;
            item.height = height;
            item.texture = texture;
            item.srcX = source.left;
            item.srcY = source.top;
            item.srcWidth = source.width();
            item.srcHeight = source.height();
            item.flipY = true;
            item.normalMap = services.normalMap(relief.normalMapName.value);
            item.deriveNormals = item.normalMap == null;
            item.heightScale = relief.heightScale.value;
            item.specular = relief.specular.value;
            item.shininess = relief.shininess.value;

            if (light != null) {
                float dim = Math.max(0.15f, Math.min(1.0f, 1.0f - light.diffuse));
                item.tintR = light.red * dim;
                item.tintG = light.green * dim;
                item.tintB = light.blue * dim;
            }
        }
    }
}
