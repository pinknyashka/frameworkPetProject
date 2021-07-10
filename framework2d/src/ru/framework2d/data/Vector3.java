package ru.framework2d.data;


public class Vector3 extends Vector2 {
	
	public DoubleFractional z = new DoubleFractional();
	public double getZ() {
		return z.value;
	}
	public void setZ(double z) {
		this.z.value = z;
	}
	
	
	
	public Vector3 () {};
	
	public Vector3 (double x, double y, double z) {
		set(x, y, z);	
	}
	
	public Vector3 (Vector3 vector) {
		set(vector.getX(), vector.getY(), vector.getZ());	
	}
	
	public Vector3 (Point3 toPoint, Point3 fromPoint) {
		set(toPoint, fromPoint);	
	}
	
	
	
	public void set(Point3 toPoint, Point3 fromPoint) {
		this.x.value = toPoint.x.value - fromPoint.x.value;
		this.y.value = toPoint.y.value - fromPoint.y.value; 
		this.z.value = toPoint.z.value - fromPoint.z.value; 
	}
		
	public void set(double x, double y, double z) {
		this.x.value = x; this.y.value = y;	this.z.value = z;	
	}
	
	public void set(Vector3 vector) {
		this.x.value = vector.x.value; this.y.value = vector.y.value;	this.z.value = vector.z.value;	
	}
	
	
	
	public double length3() {
		return Math.sqrt(this.x.value * this.x.value + this.y.value * this.y.value + this.z.value * this.z.value);	
	}
	
	@Override
	public double restore() {	//сделать единичным вернув прежнюю длину to identity returns old length 
		double length = Math.sqrt(this.x.value * this.x.value + this.y.value * this.y.value + this.z.value * this.z.value);
		if (length > 0) {
			this.x.value /= length; this.y.value /= length; this.z.value /= length; 
		}
		return length;
	}
		
	
	
	public Vector3 plus(Vector3 vector) {	//сложить вектора вернув новый 
		return new Vector3(getX() + vector.getX(), getY() + vector.getY(), this.z.value + vector.getZ()); 
	}
	
	public Vector3 minus(Vector3 vector) {	//сложить вектора вернув новый 
		return new Vector3(getX() - vector.getX(), getY() - vector.getY(), this.z.value - vector.getZ()); 
	}
	
	@Override
	public Vector3 multi(double c) {	//умножить вектор на число вернув новый 
		return new Vector3(getX() * c, getY() * c, this.z.value * c); 
	}
	
	
	
	public Vector3 selfMulti(double c) {	//умножить себя на число
		this.x.value *= c; this.y.value *= c; this.z.value *= c;
		return this; 
	}
	
	
	
	public Vector3 addict(Vector3 vector) {	//прирастить вектор 
		this.x.value += vector.x.value; this.y.value += vector.y.value; this.z.value += vector.z.value;
		return this;
	}
	
	
	
	public Vector3 decrease(Vector3 vector) {	//уменьшить вектор 
		this.x.value -= vector.x.value; this.y.value -= vector.y.value; this.z.value -= vector.z.value;
		return this;
	}
	
	
	
	public double myProectionOnVector(double x, double y, double z) {	//проекция меня на вектор который передается параметром
		double length = length3();
		if (length > 0) return (getX() * x + getY() * y + this.z.value * z) / length;
		else return 0; 
	}
	
	public double myProectionOnVector(Vector3 vector) {	//проекция меня на вектор который передается параметром
		return myProectionOnVector(vector.getX(), vector.getY(), vector.getZ()); 
	}
	
	public double myAbsProectionOnVector(Vector3 vector) {	//проекция меня на вектор который передается параметром
		double k = myProectionOnVector(vector.getX(), vector.getY(), vector.getZ());
		if (k > 0) return k;
		else return -k;
	}
	
	public Vector3 myVectorProectionOnVector(Vector3 vector) {	//проекция меня на вектор который передается параметром
		double length = length3();
		if (length > 0) return vector.multi((getX() * vector.getX() 
											+ getY() * vector.getY() 
											+ this.z.value * vector.getZ()) / length); 
		else return new Vector3(0, 0, 0); 
	}
	
	
	
	@Override
	public void setValue(DataInterface data) {
		if (data instanceof Vector3) this.set((Vector3) data);
	}
	
	@Override 
	public DataInterface getInnerData(String propertyName) {
		if (propertyName.contentEquals("z")) {
			return z;
		} else return super.getInnerData(propertyName);
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
