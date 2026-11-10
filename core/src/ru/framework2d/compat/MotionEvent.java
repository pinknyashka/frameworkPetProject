package ru.framework2d.compat;

import com.badlogic.gdx.utils.Array;

/**
 * Replacement for android.view.MotionEvent. The engine consumes touch as MotionEvent
 * objects (getAction/getActionMasked/getActionIndex/getPointerCount/getX/getY). An
 * InputProcessor bridge builds these from libGDX pointer callbacks.
 */
public class MotionEvent {

    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_POINTER_DOWN = 5;
    public static final int ACTION_POINTER_UP = 6;

    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_POINTER_INDEX_MASK = 0xff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    private int action;
    private final Array<float[]> pointers = new Array<float[]>(); // {x, y, pointerId}

    public MotionEvent(int action) {
        this.action = action;
    }

    public MotionEvent addActionPointer(float x, float y, int pointerId) {
        pointers.add(new float[] { x, y, pointerId });
        return this;
    }

    public int getAction() {
        return action;
    }

    public int getActionMasked() {
        return action & ACTION_MASK;
    }

    public int getActionIndex() {
        return (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    public int getPointerCount() {
        return pointers.size;
    }

    public float getX(int index) {
        return pointers.get(index)[0];
    }

    public float getY(int index) {
        return pointers.get(index)[1];
    }

    public int getPointerId(int index) {
        return (int) pointers.get(index)[2];
    }

    public float getX() {
        return getX(getActionIndex());
    }

    public float getY() {
        return getY(getActionIndex());
    }
}
