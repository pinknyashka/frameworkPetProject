package ru.framework2d.data;


public class Point2 extends DataInterface {

	public DoubleFractional x = new DoubleFractional();
	
	public double getX() {
		return x.value;
	}
	
	public void setX(double x) {
		this.x.value = x;
	}
	
	
	
	public DoubleFractional y = new DoubleFractional();
	
	public double getY() {
		return y.value;
	}
	
	public void setY(double y) {
		this.y.value = y;
	}
	
	
	
	public Point2 () {};
	
	public Point2 (double x, double y) {
		set(x, y);	
	}
	
	public Point2 (Point2 p2) {
		set(p2);	
	}
	
	
		
	public void set(double x, double y) {
		
		this.x.value = x; this.y.value = y; //faster
	}
	
	public void set(Point2 p2d) {
		
		this.x.value = p2d.x.value; this.y.value = p2d.y.value; //faster
	}
	
	
	
	public double dist(Point2 p) {
		
		return Math.sqrt((x.value - p.x.value) * (x.value - p.x.value) 
						+ (y.value - p.y.value) * (y.value - p.y.value));	
	}
	
	
						
	public void move(Vector2 direct, double range) {
		
		this.x.value += direct.x.value * range;	
		this.y.value += direct.y.value * range;	
	}
	
	
	public void move(Vector2 offset) {
		
		this.x.value += offset.x.value;	
		this.y.value += offset.y.value;	
	}
		
	
	
	public boolean checkLineX(int x1, int x2, float size) {
		
		return (x.value - size > x1 && x.value + size < x2);
	}
	
	public boolean checkLineY(int y1, int y2, float size) {
		
		return (y.value - size > y1 && y.value + size < y2);
	}
	
	public boolean checkField(int x, int y, float size) {
		
		return ((this.x.value - size > 0 && this.x.value + size < x) && 
				(this.y.value - size > 0 && this.y.value + size < y));
	}
	
	public boolean checkField(int x1, int y1, int x2, int y2, float size) {
		
		return ((x.value - size > x1 && x.value + size < x2) &&
				(y.value - size > y1 && y.value + size < y2));
	}
	
	
	
	public void setValue(DataInterface data) {
		if (data instanceof Point2)	set((Point2) data);
	}
	
	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		
		if (name.contains(this.getName())) {
			if (name.contains(":x")) {
				x.setValue(data);
				return true;
			} 
			else if (name.contains(":y")) {
				y.setValue(data);
				return true;
			}
		}
		return false;
	}
	
	public boolean setProperty(String name, String value) {
		
		if (name.contains(this.getName())) {
			if (name.contains(":x")) {
				this.x.value = Double.parseDouble(value);
				return true;
			} 
			else if (name.contains(":y")) {
				this.y.value = Double.parseDouble(value);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "" + this.x.toString() + ":" + this.y.toString();  
	}
	
}
