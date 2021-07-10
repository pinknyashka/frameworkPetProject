package ru.framework2d.touches.areas.drag;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Vector2;
import ru.framework2d.touches.areas.TouchableArea;

public class TouchableAreaDrag extends TouchableArea {
	
	public final static String SHORT_CLASS_NAME = "drag";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Bool draged = new Bool(false);

	public Vector2 touchMove = new Vector2();
	
}
