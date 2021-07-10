package ru.framework2d.touches.areas.switcher;

import ru.framework2d.data.Bool;
import ru.framework2d.touches.areas.TouchableArea;

public class TouchableAreaSwitcher extends TouchableArea {
	
	public final static String SHORT_CLASS_NAME = "switcher";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Bool isOn = new Bool(false);

}
