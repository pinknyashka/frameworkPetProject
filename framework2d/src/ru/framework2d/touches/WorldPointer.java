package ru.framework2d.touches;

import java.util.ArrayList;

import ru.framework2d.data.Bool;
import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.touches.areas.TouchableArea;
import ru.framework2d.touches.screen.TouchFinger;


public class WorldPointer extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "pointer";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	public Bool isTouches = new Bool(false);
	public Bool isCurrentlyMoving = new Bool(false);
	
	public TouchFinger linkedFinger;
	
	
	
	public ArrayList <TouchableArea> lstSelectedAreas = new ArrayList <TouchableArea>();  

	public WorldPointer() {}
	
}
