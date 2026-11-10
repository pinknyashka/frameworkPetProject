package ru.framework2d.graphics.render.light;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.graphics.draw.shadows.OutputGraphicalLight;
import ru.framework2d.graphics.render.LightState;
import ru.framework2d.graphics.render.RenderFrame;
import ru.framework2d.graphics.render.RenderServices;
import ru.framework2d.graphics.render.RenderSystem;
import ru.framework2d.graphics.render.RenderTransform;

public class LightRenderSystem extends RenderSystem {

    private final ArrayList<OutputGraphicalLight> lights = new ArrayList<OutputGraphicalLight>();
    private final RenderTransform transform = new RenderTransform();

    @SuppressWarnings("unchecked")
    public LightRenderSystem() {
        super(OutputGraphicalLight.class);
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);
        if (component instanceof OutputGraphicalLight) {
            OutputGraphicalLight light = (OutputGraphicalLight) component;
            if (!light.entity.isPrototype && !lights.contains(light)) {
                lights.add(light);
            }
        }
        return component;
    }

    @Override
    public void collect(RenderFrame frame, RenderServices services) {
        frame.ambient = 0.45f;
        float maxAmbient = -1;

        for (int i = 0; i < lights.size(); i++) {
            OutputGraphicalLight light = lights.get(i);
            if (!light.isVisible.value || !light.isLocalVisible.value) {
                continue;
            }
            LightState state = frame.obtainLight();
            if (state == null) {
                break;
            }

            transform.compute(light);
            state.position.set(transform.x, transform.y, transform.z);
            state.red = light.red.value / 255.0f;
            state.green = light.green.value / 255.0f;
            state.blue = light.blue.value / 255.0f;
            state.intensity = light.intensity.value;
            state.radius = Math.max(1.0f, light.radius.value);
            state.diffuse = light.diffuse.value;
            state.castsShadows = light.castsShadows.value;

            if (light.ambient.value >= 0) {
                maxAmbient = Math.max(maxAmbient, light.ambient.value);
            }
        }

        if (frame.lightCount() == 0) {
            frame.ambient = 1.0f;
        } else if (maxAmbient >= 0) {
            frame.ambient = maxAmbient;
        }
    }
}
