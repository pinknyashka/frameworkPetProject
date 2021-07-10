package ru.framework2d.touches.areas.direct;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Vector3;
import ru.framework2d.touches.areas.TouchableArea;

public class TouchableAreaDirect extends TouchableArea {

	public final static String SHORT_CLASS_NAME = "d_touch";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Bool isInScreenProportion = new Bool(false);
	public Fractional lenght = new Fractional(0);
	public Fractional proportion = new Fractional(1024);
	
	public Vector3 shotDirect = new Vector3();
	public Vector3 rotateDirect = new Vector3();
	
	public TouchableAreaDirect() { }


}
