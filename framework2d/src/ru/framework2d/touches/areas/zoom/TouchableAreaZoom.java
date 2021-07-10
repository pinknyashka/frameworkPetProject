package ru.framework2d.touches.areas.zoom;

import ru.framework2d.data.Fractional;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Vector2;
import ru.framework2d.touches.areas.TouchableArea;

public class TouchableAreaZoom extends TouchableArea {

	public final static String SHORT_CLASS_NAME = "zoomer";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Vector2 betweenFingers = new Vector2();
	
	public Vector2 oldBetweenFingers = new Vector2();
	
	public Fractional zoom = new Fractional(1.0f);
	public Fractional zoomMin = new Fractional(0.1f);
	public Fractional zoomMax = new Fractional(10.0f);
	
	public Position2 realizePosition = new Position2();
	
	public TouchableAreaZoom() { }

}
