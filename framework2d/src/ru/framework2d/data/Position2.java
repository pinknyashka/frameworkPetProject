package ru.framework2d.data;


public class Position2 extends Point2 {
	
	private DoubleFractional alpha = new DoubleFractional();
	public double getAlpha() {
		return alpha.value;
	}
	public void setAlpha(double alpha) {
		this.alpha.value = alpha;
	}
	
	
	
	public Position2(){};
	public Position2(double x, double y, double alpha) {
		super(x, y);	setAlpha(alpha);
	}
	public Position2(double x, double y) {
		super(x, y);
	}
	
	
	
	public void set(double x, double y, double alpha) {
		this.x.value = x; this.y.value = y;	setAlpha(alpha);
	}
	
	public void set(Position2 pos2d) {
		this.x.value = pos2d.x.value; this.y.value = pos2d.y.value; 
		setAlpha(pos2d.getAlpha());
	}
	
	
	
	public void setValue(DataInterface data) {
		if (data instanceof Position2)	set((Position2) data);
	}
	
	public DataInterface getInnerData(String propertyName) {
		if (propertyName.contentEquals("alpha")) {
			return alpha;
		}
		return super.getInnerData(propertyName);
	}
	
	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		if (!super.setPropertyData(name, data)) {
			if (name.contains(this.getName())) {
				if (name.contains(":alpha")) {
					alpha.setValue(data);
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
				if (name.contains(":alpha")) {
					this.alpha.value = Double.parseDouble(value);
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "" + this.x.toString() + ":" + this.y.toString() + ":" + this.alpha.toString();  
	}
}
