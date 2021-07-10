package ru.framework2d.graphics.draw.shadows;

import ru.framework2d.data.Geometry;
import ru.framework2d.data.GeometryBlock;
import ru.framework2d.data.GeometryVerticalCylinder;
import ru.framework2d.graphics.OutputGraphicalInterface;
import android.graphics.Path;
import android.graphics.Region;

public class OutputGraphicalShadowLayer extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "shadow_layer"; 
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Geometry geometry;
	
	public String geometryName = "";
	
	public Path drawingArea;
	public Path shadowArea;
	
	public boolean isCircle = false;
	public float radius = 0;
	
	public OutputGraphicalShadowLayer() { super(); };

}
