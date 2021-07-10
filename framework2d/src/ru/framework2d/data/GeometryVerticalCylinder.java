package ru.framework2d.data;

public class GeometryVerticalCylinder extends Geometry {
	
	public Fractional height = new Fractional();
	public Fractional radius = new Fractional();
	
	
	
	@Override
	public boolean setGeometry(Geometry geometry) {
		if (geometry instanceof GeometryVerticalCylinder) {
			GeometryVerticalCylinder verticalCylinder = (GeometryVerticalCylinder) geometry;
			height.value = verticalCylinder.height.value;
			radius.value = verticalCylinder.radius.value;
			return true;
		}
		return false;
	}
	
	
	
	public boolean setProperty(String name, String value) {
		if (name.contains(":height")) {
			this.height.value = (float) Double.parseDouble(value);
		} else if (name.contains(":radius")) {
			this.radius.value = (float) Double.parseDouble(value);
		} else return false;
		return true;
	}
	
	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		if (name.contains(":height")) {
			this.height.setValue(data);
		} else if (name.contains(":radius")) {
			this.radius.setValue(data);
		} else return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "" + this.height.toString() + ":" + this.radius.toString();  
	}
	
}
