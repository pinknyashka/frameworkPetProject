package ru.framework2d.graphics.cameras;

import ru.framework2d.touches.WorldPointer;

public class WorldPointerCamera extends WorldPointer {

	public final static String SHORT_CLASS_NAME = "cam_pointer";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	
	public OutputGraphicalCamera linkedCamera;
	
	
	
	public WorldPointerCamera() {}
	
	public WorldPointerCamera(OutputGraphicalCamera camera) {
		linkedCamera = camera; 
	}
	
	

}
