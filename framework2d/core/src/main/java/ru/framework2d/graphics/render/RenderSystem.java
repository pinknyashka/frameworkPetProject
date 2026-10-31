package ru.framework2d.graphics.render;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.graphics.draw.shadows.OutputGraphicalShadow;

/**
 * Base class for the small systems that collect renderable state. A system is
 * also a component handler, so normal entity/prototype connection rules apply.
 */
public abstract class RenderSystem extends ComponentsHandler {

    protected OutputRenderHQ output;

    @SuppressWarnings("unchecked")
    public RenderSystem(Class<? extends Component>... classes) {
        super(classes.length, (Class<Component>[]) classes);

        for (Engine engine : enginesManager.lstEngines) {
            if (engine instanceof OutputRenderHQ) {
                output = (OutputRenderHQ) engine;
                break;
            }
        }
    }

    protected boolean ownsShadowCaster(OutputGraphicalInterface graphical) {
        if (graphical.entity == null) {
            return false;
        }
        for (int i = 0; i < graphical.entity.lstGraphicals.size(); i++) {
            if (graphical.entity.lstGraphicals.get(i) instanceof OutputGraphicalShadow) {
                return true;
            }
        }
        return false;
    }

    public abstract void collect(RenderFrame frame, RenderServices services);

    public void render(RenderFrame frame, RenderServices services) {
    }

    @Override
    public boolean startWork(int delay) {
        return false;
    }

    @Override
    public boolean loadEntities(EntityStorage objectManager) {
        return false;
    }
}
