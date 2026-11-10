package ru.framework2d.graphics.render;

import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.graphics.cameras.OutputGraphicalCamera;

/**
 * Resolves the world-space transform of a graphical component by walking the
 * parent graph. Camera nodes are skipped: the HQ renderer applies the camera
 * projection globally instead of nesting it in every sprite transform.
 */
public class RenderTransform {

    public float x;
    public float y;
    public float z;
    public float angle;
    public float alpha = 1.0f;

    public boolean compute(OutputGraphicalInterface graphical) {
        x = 0;
        y = 0;
        z = 0;
        angle = 0;
        alpha = 1.0f;

        OutputGraphicalInterface current = graphical;
        while (current != null) {
            if (current instanceof OutputGraphicalCamera) {
                break;
            }
            if (!current.isVisible.value || !current.isLocalVisible.value) {
                return false;
            }

            float currentAngle = 0;
            if (current.isRotatable.value) {
                currentAngle += current.position.getAlpha();
            }
            if (current.isLocalRotatable.value) {
                currentAngle += current.localPos.getAlpha();
            }

            float localX = (float) current.position.x.value + (float) current.localPos.x.value;
            float localY = (float) current.position.y.value + (float) current.localPos.y.value;

            float cos = (float) Math.cos(currentAngle);
            float sin = (float) Math.sin(currentAngle);
            float rotatedX = cos * x - sin * y;
            float rotatedY = sin * x + cos * y;

            x = localX + rotatedX;
            y = localY + rotatedY;
            z += (float) current.position.z.value + (float) current.localPos.z.value;
            angle += currentAngle;
            alpha *= current.transparency.value * current.localTransparency.value;

            current = current.parentGraphical;
        }

        return alpha > 0.0001f;
    }
}
