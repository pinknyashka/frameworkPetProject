package ru.framework2d.graphics.render;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Entity;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.graphics.cameras.OutputGraphicalCamera;
import ru.framework2d.graphics.render.camera.CameraRenderSystem;
import ru.framework2d.graphics.render.light.LightRenderSystem;
import ru.framework2d.graphics.render.relief.ReliefRenderSystem;
import ru.framework2d.graphics.render.shadow.ShadowRenderSystem;
import ru.framework2d.graphics.render.sprite.SpriteRenderSystem;
import ru.framework2d.graphics.render.text.TextRenderSystem;
import ru.framework2d.touches.WorldPointer;

public class HandlerRenderHQ extends ComponentsHandler {

    private OutputRenderHQ output;

    private CameraRenderSystem cameraSystem;
    private LightRenderSystem lightSystem;
    private SpriteRenderSystem spriteSystem;
    private ReliefRenderSystem reliefSystem;
    private ShadowRenderSystem shadowSystem;
    private TextRenderSystem textSystem;
    private boolean systemsReady;

    public HandlerRenderHQ() {
        super();

        for (Engine engine : enginesManager.lstEngines) {
            if (engine instanceof OutputRenderHQ) {
                output = (OutputRenderHQ) engine;
                break;
            }
        }

        getContexts();
        loadEntities(entityStorage);
    }

    private void initSystems() {
        for (int i = 0; i < lstSubHandlers.size(); i++) {
            ComponentsHandler handler = lstSubHandlers.get(i);
            if (handler instanceof CameraRenderSystem) {
                cameraSystem = (CameraRenderSystem) handler;
            } else if (handler instanceof LightRenderSystem) {
                lightSystem = (LightRenderSystem) handler;
            } else if (handler instanceof SpriteRenderSystem) {
                spriteSystem = (SpriteRenderSystem) handler;
            } else if (handler instanceof ReliefRenderSystem) {
                reliefSystem = (ReliefRenderSystem) handler;
            } else if (handler instanceof ShadowRenderSystem) {
                shadowSystem = (ShadowRenderSystem) handler;
            } else if (handler instanceof TextRenderSystem) {
                textSystem = (TextRenderSystem) handler;
            }
        }
        systemsReady = true;
    }

    @Override
    public Component connectComponent(Component component) {
        super.connectComponent(component);

        if (component instanceof OutputGraphicalInterface
                && !(component instanceof OutputGraphicalCamera)
                && !(component instanceof WorldPointer)
                && component.entity != null
                && !component.entity.isPrototype
                && output != null) {
            output.addGraphical((OutputGraphicalInterface) component);
        }
        return component;
    }

    @Override
    public boolean transferData(Entity entity, DataInterface data) {
        boolean transferred = false;
        if (entity == null) {
            return false;
        }
        for (int i = 0; i < entity.lstGraphicals.size(); i++) {
            if (applyGraphicalData(entity.lstGraphicals.get(i), data)) {
                transferred = true;
            }
        }
        return transferred;
    }

    private boolean applyGraphicalData(OutputGraphicalInterface graphical, DataInterface data) {
        if (data.getContext() == graphical.position.getContext()) {
            if (data instanceof Position3) {
                graphical.position.set((Position3) data);
            } else if (data instanceof Point2) {
                if (data instanceof Position2) {
                    graphical.position.set((Position2) data);
                } else {
                    graphical.position.set((Point2) data);
                }
            } else {
                return false;
            }
            return true;
        }
        if (data.getContext() == graphical.localPos.getContext()) {
            if (data instanceof Vector2) {
                graphical.localPos.setAlpha((float) ((Vector2) data).computeAngle());
            } else if (data instanceof Position3) {
                graphical.localPos.set((Position3) data);
            } else if (data instanceof Point2) {
                if (data instanceof Position2) {
                    graphical.localPos.set((Position2) data);
                } else {
                    graphical.localPos.set((Point2) data);
                }
            } else {
                return false;
            }
            return true;
        }
        if (data instanceof Fractional) {
            if (data.getContext() == graphical.transparency.getContext()) {
                graphical.transparency.value = ((Fractional) data).value;
                return true;
            }
            if (data.getContext() == graphical.localTransparency.getContext()) {
                graphical.localTransparency.value = ((Fractional) data).value;
                return true;
            }
        } else if (data instanceof Bool) {
            if (data.getContext() == graphical.isVisible.getContext()) {
                graphical.isVisible.value = ((Bool) data).value;
                return true;
            }
            if (data.getContext() == graphical.isLocalVisible.getContext()) {
                graphical.isLocalVisible.value = ((Bool) data).value;
                return true;
            }
        }
        return false;
    }

    public void renderFrame(RenderFrame frame, RenderServices services) {
        if (!systemsReady) {
            initSystems();
        }

        if (cameraSystem != null) {
            cameraSystem.collect(frame, services);
        }
        if (lightSystem != null) {
            lightSystem.collect(frame, services);
        }
        if (spriteSystem != null) {
            spriteSystem.collect(frame, services);
        }
        if (reliefSystem != null) {
            reliefSystem.collect(frame, services);
        }
        if (textSystem != null) {
            textSystem.collect(frame, services);
        }

        frame.sortItems();

        if (shadowSystem != null) {
            shadowSystem.render(frame, services);
        }

        services.beginScene(frame);
        for (int i = 0; i < frame.items().size(); i++) {
            RenderItem item = frame.items().get(i);
            if (item.kind != RenderItem.KIND_TEXT) {
                services.drawItem(item);
            }
        }
        services.endScene();

        if (textSystem != null) {
            textSystem.render(frame, services);
        }
    }

    @Override
    public boolean startWork(int delay) {
        return false;
    }
}
