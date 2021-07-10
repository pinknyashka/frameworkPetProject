package ru.framework2d.graphics.draw.shadows;

import ru.framework2d.core.Property;
import ru.framework2d.data.Geometry;
import ru.framework2d.data.GeometryBlock;
import ru.framework2d.data.GeometryVerticalCylinder;
import ru.framework2d.data.Text;
import ru.framework2d.graphics.OutputGraphicalInterface;
import android.graphics.Path;

public class OutputGraphicalShadow extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "shadow"; 
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Geometry geometry;
	
	public Text geometryName = new Text("");
	
	public Path drawingArea;
	
	public boolean isDynamic = false;
	
	public OutputGraphicalShadow() { super(); };
	
	@Override
	public void setPropertyData(Property property) {
		if (property.name.contains("geometry:")) {
			if (property.name.contains(":type")) {
				geometryName.value = ((Text)property.data).value; 
				if (geometryName.value.contentEquals("v_cylinder")) {
					geometry = new GeometryVerticalCylinder();
				} else if (geometryName.value.contentEquals("block")) {
					geometry = new GeometryBlock();
				}
			} else {
				if (geometry != null) {
					geometry.setPropertyData(property.name, property.data);
				}
			}
		} else {
			super.setPropertyData(property);
		}
		
	}

}
