package ru.framework2d.touches.motion;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector3;
import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.WorldPointer;


public class TouchMotion extends InputTouchInterface {
	
	public static final String SHORT_CLASS_NAME = "motion";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Point2 globalTouchPointTap = new Point2();
	public Vector3 directTap = new Vector3();

	public Bool enabled = new Bool(true);
	
	public int latency = 200;
	public Bool isLongTap = new Bool(false);
	
	public long touchMoment = 0;
	
	public WorldPointer touchPointer;
	
}
