package ru.framework2d.graphics.render.sprite;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Texture;

import ru.framework2d.compat.Rect;
import ru.framework2d.core.Component;
import ru.framework2d.core.Engine;
import ru.framework2d.graphics.draw.sprite.OutputGraphicalSprite;
import ru.framework2d.graphics.render.RenderFrame;
import ru.framework2d.graphics.render.RenderItem;
import ru.framework2d.graphics.render.RenderServices;
import ru.framework2d.graphics.render.RenderSystem;
import ru.framework2d.graphics.render.RenderTransform;

public class SpriteRenderSystem extends RenderSystem {

    private final ArrayList<OutputGraphicalSprite> sprites = new ArrayList<OutputGraphicalSprite>();
    private final RenderTransform transform = new RenderTransform();

    @SuppressWarnings("unchecked")
    public SpriteRenderSystem() {
        super(OutputGraphicalSprite.class);
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);
        if (component instanceof OutputGraphicalSprite) {
            OutputGraphicalSprite sprite = (OutputGraphicalSprite) component;
            if (sprite.sprite == null && sprite.spriteName.value != null && sprite.spriteName.value.length() > 0) {
                sprite.sprite = Engine.resources.getSprite(sprite.spriteName.value,
                        sprite.spriteCollumnsNum.value, sprite.spriteRowsNum.value);
            }
            if (!sprite.entity.isPrototype && !sprites.contains(sprite)) {
                sprites.add(sprite);
            }
        }
        return component;
    }

    @Override
    public void collect(RenderFrame frame, RenderServices services) {
        for (int i = 0; i < sprites.size(); i++) {
            OutputGraphicalSprite sprite = sprites.get(i);
            if (sprite.sprite == null || sprite.sprite.spriteImage == null
                    || sprite.sprite.spriteImage.texture == null
                    || sprite.sprite.lstSpritesRects.isEmpty()) {
                continue;
            }
            if (!transform.compute(sprite)) {
                continue;
            }
            float width = (float) sprite.size.x.value;
            float height = (float) sprite.size.y.value;
            if (width == 0 || height == 0) {
                width = Math.max(width, sprite.radius.value * 2.0f);
                height = Math.max(height, sprite.radius.value * 2.0f);
            }

            int spriteNo = sprite.currentSpriteNo.value;
            if (spriteNo < 0) {
                spriteNo = 0;
            } else if (spriteNo >= sprite.sprite.lstSpritesRects.size()) {
                spriteNo = sprite.sprite.lstSpritesRects.size() - 1;
            }
            Rect source = sprite.sprite.lstSpritesRects.get(spriteNo);

            if (sprite.cullable.value) {
                float cullRadius = Math.max(width, height) * 0.75f + 4.0f;
                if (!frame.isVisible(transform.x, transform.y, cullRadius)) {
                    continue;
                }
            }

            Texture texture = sprite.sprite.spriteImage.texture;
            services.configure(texture);

            RenderItem item = frame.obtainItem();
            item.kind = RenderItem.KIND_SPRITE;
            item.layer = sprite.layer.value;
            item.receiveShadows = sprite.layer.value > -2.0f;
            item.x = transform.x;
            item.y = transform.y;
            item.z = transform.z;
            item.angle = transform.angle;
            item.alpha = transform.alpha;
            item.width = width;
            item.height = height;
            item.texture = texture;
            item.srcX = source.left;
            item.srcY = source.top;
            item.srcWidth = source.width();
            item.srcHeight = source.height();
            item.flipY = true;
            item.deriveNormals = false;
            item.normalMap = services.normalMap(sprite.normalMapName.value);
            item.heightScale = sprite.heightScale.value;
            item.specular = sprite.specular.value;
            item.shininess = sprite.shininess.value;
        }
    }
}
