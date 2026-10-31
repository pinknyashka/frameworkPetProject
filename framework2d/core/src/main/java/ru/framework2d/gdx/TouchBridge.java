package ru.framework2d.gdx;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.Array;

import ru.framework2d.compat.MotionEvent;
import ru.framework2d.touches.TouchPeriphery;

/**
 * Bridges libGDX input callbacks into the engine's touch pipeline by building
 * shim MotionEvent objects and feeding them to the TouchPeriphery engine.
 * libGDX and the engine both use y-down screen coordinates, so they pass through unchanged.
 */
public class TouchBridge extends InputAdapter {

    private final TouchPeriphery periphery;
    private final Array<float[]> pointers = new Array<float[]>(); // {x, y(down), id}

    public TouchBridge(TouchPeriphery periphery) {
        this.periphery = periphery;
    }

    private int indexOf(int id) {
        for (int i = 0; i < pointers.size; i++) {
            if ((int) pointers.get(i)[2] == id) {
                return i;
            }
        }
        return -1;
    }

    private MotionEvent build(int action) {
        MotionEvent event = new MotionEvent(action);
        for (int i = 0; i < pointers.size; i++) {
            float[] p = pointers.get(i);
            event.addActionPointer(p[0], p[1], (int) p[2]);
        }
        return event;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (indexOf(pointer) < 0) {
            pointers.add(new float[] { screenX, screenY, pointer });
        }
        int index = indexOf(pointer);
        int action = (pointers.size == 1)
                ? MotionEvent.ACTION_DOWN
                : (MotionEvent.ACTION_POINTER_DOWN | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
        periphery.toHandleEvent(build(action));
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        int index = indexOf(pointer);
        if (index >= 0) {
            float[] p = pointers.get(index);
            p[0] = screenX;
            p[1] = screenY;
        }
        periphery.toHandleEvent(build(MotionEvent.ACTION_MOVE));
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        int index = indexOf(pointer);
        if (index < 0) {
            return false;
        }
        float[] p = pointers.get(index);
        p[0] = screenX;
        p[1] = screenY;

        int action = (pointers.size == 1)
                ? MotionEvent.ACTION_UP
                : (MotionEvent.ACTION_POINTER_UP | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
        periphery.toHandleEvent(build(action));
        pointers.removeIndex(index);
        return true;
    }
}
