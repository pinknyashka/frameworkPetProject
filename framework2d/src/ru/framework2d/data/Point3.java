package ru.framework2d.data;


public class Point3 extends Point2 {
	
	public DoubleFractional z = new DoubleFractional();
	public double getZ() {
		return z.value;
	}
	public void setZ(double z) {
		this.z.value = z;
	}
	
	
	
	public Point3() {};
	
	public Point3(double x, double y, double z) {
		set(x, y, z);
	}
	
	public Point3(Point3 p3) {
		set(p3); 
	}
	
	
	
	public void set(double x, double y, double z) {
		this.x.value = x; this.y.value = y; this.z.value = z;
	}
	
	public void set(Point3 p3d) {
		this.x.value = p3d.x.value; 
		this.y.value = p3d.y.value; 
		this.z.value = p3d.z.value;
	}
	
	
	
	double dist(Point3 p) {
		return Math.sqrt( (this.x.value - p.x.value) * (this.x.value - p.x.value) 
						+ (this.y.value - p.y.value) * (this.y.value - p.y.value) 
						+ (this.z.value - p.z.value) * (this.z.value - p.z.value) );	
	}
	
	
	
	public void move(Vector3 direct, double range) {
		this.x.value += direct.x.value * range;
		this.y.value += direct.y.value * range;
		this.z.value += direct.z.value * range;
	}
	
	public void move(Vector3 offset) {
		this.x.value += offset.x.value;
		this.y.value += offset.y.value;
		this.z.value += offset.z.value;
	}
	
	
	
	public void setValue(DataInterface data) {
		if (data instanceof Point3)	set((Point3) data);
	}
	
	public DataInterface getInnerData(String propertyName) {
		if (propertyName.contentEquals("z")) {
			return z;
		}
		return super.getInnerData(propertyName);
	}
	
	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		if (!super.setPropertyData(name, data)) {
			if (name.contains(this.getName())) {
				if (name.contains(":z")) {
					z.setValue(data);
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public boolean setProperty(String name, String value) {
		if (!super.setProperty(name, value)) {
			if (name.contains(this.getName())) {
				if (name.contains(":z")) {
					this.z.value = Double.parseDouble(value);
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "" + this.x.toString() + ":" + this.y.toString() + ":" + this.z.toString();  
	}
}