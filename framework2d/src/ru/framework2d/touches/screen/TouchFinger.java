package ru.framework2d.touches.screen;

import java.util.ArrayList;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Point2;
import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.areas.TouchableArea;


public class TouchFinger extends InputTouchInterface {
	
	public final static String SHORT_CLASS_NAME = "finger";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public int index = -1;
	public int pointerId = -1;
	
	public Bool touched = new Bool(false);
	
	public Bool isCurrentlyMoving = new Bool(false);
	
	public Point2 oldPosition = new Point2();
	
	public ArrayList <Point2> fingerPath = new ArrayList <Point2>();
	
	public TouchableArea linkedMotion;
	
	public TouchFinger(int index) {
		setName("InputTouchFinger");
		this.index = index;
	}
	
}
