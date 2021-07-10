package ru.framework2d.data;

public class GeometryBlock extends Geometry {

	public Fractional height = new Fractional();
	public Fractional width = new Fractional();
	public Fractional depth = new Fractional();
	
	
	
	@Override
	public boolean setGeometry(Geometry geometry) {
		if (geometry instanceof GeometryBlock) {
			GeometryBlock block = (GeometryBlock) geometry;
			height.value = block.height.value;
			width.value = block.width.value;
			depth.value = block.depth.value;
			return true;
		}
		return false;
	}
	
	

	public boolean setProperty(String name, String value) {
		if (name.contains(":height")) {
			this.height.value = (float) Double.parseDouble(value);
		} else if (name.contains(":width")) {
			this.width.value = (float) Double.parseDouble(value);
		} else if (name.contains(":depth")) {
			this.depth.value = (float) Double.parseDouble(value);
		} else return false;
		return true;
	}

	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		if (name.contains(":height")) {
			this.height.setValue(data);
		} else if (name.contains(":width")) {
			this.width.setValue(data);
		} else if (name.contains(":depth")) {
			this.depth.setValue(data);
		} else return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "" + this.height.toString() + ":" + this.width.toString() + ":" + this.height.toString();  
	}

}
