package ru.framework2d.graphics.render.text;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Paint;
import ru.framework2d.core.Component;
import ru.framework2d.graphics.draw.text.OutputGraphicalText;
import ru.framework2d.graphics.render.RenderFrame;
import ru.framework2d.graphics.render.RenderItem;
import ru.framework2d.graphics.render.RenderServices;
import ru.framework2d.graphics.render.RenderSystem;
import ru.framework2d.graphics.render.RenderTransform;

public class TextRenderSystem extends RenderSystem {

    private final ArrayList<OutputGraphicalText> texts = new ArrayList<OutputGraphicalText>();
    private final RenderTransform transform = new RenderTransform();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @SuppressWarnings("unchecked")
    public TextRenderSystem() {
        super(OutputGraphicalText.class);
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);
        if (component instanceof OutputGraphicalText) {
            OutputGraphicalText text = (OutputGraphicalText) component;
            if (!text.entity.isPrototype && !texts.contains(text)) {
                texts.add(text);
            }
        }
        return component;
    }

    @Override
    public void collect(RenderFrame frame, RenderServices services) {
        for (int i = 0; i < texts.size(); i++) {
            OutputGraphicalText text = texts.get(i);
            if (text.text.value == null || text.text.value.length() == 0) {
                continue;
            }
            if (!transform.compute(text)) {
                continue;
            }

            RenderItem item = frame.obtainItem();
            item.kind = RenderItem.KIND_TEXT;
            item.layer = text.layer.value;
            item.x = transform.x;
            item.y = transform.y;
            item.angle = transform.angle;
            item.alpha = transform.alpha;
            item.width = (float) text.size.x.value;
            item.height = (float) text.size.y.value;
            item.text = text.text.value;
            item.textColor = text.color.value;
            item.textSize = text.fontSize.value;
        }
    }

    @Override
    public void render(RenderFrame frame, RenderServices services) {
        services.ensureInitialized();
        Canvas canvas = services.textCanvas();
        SpriteBatch batch = services.textBatch();
        if (canvas == null || batch == null) {
            return;
        }

        canvas.setFrame(batch, frame.projection, services.font());
        batch.begin();
        for (int i = 0; i < frame.items().size(); i++) {
            RenderItem item = frame.items().get(i);
            if (item.kind != RenderItem.KIND_TEXT || item.text == null) {
                continue;
            }

            paint.setColor(item.textColor);
            paint.setAlpha((int) (255.0f * item.alpha));
            paint.setTextSize(item.textSize);

            int state = canvas.save();
            canvas.translate(item.x, item.y);
            if (item.angle != 0) {
                canvas.rotate((float) Math.toDegrees(item.angle));
            }
            canvas.drawText(item.text, -item.width * 0.5f, item.height * 0.5f, paint);
            canvas.restoreToCount(state);
        }
        batch.end();
    }
}
