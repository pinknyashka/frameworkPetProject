package ru.framework2d.data;


public class Vector2 extends DataInterface {
	
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
	
	public Vector2 () {}
	public Vector2 (double x, double y) {
		set(x, y);	
	}
	public Vector2 (Vector2 v) {
		set(v);	
	}
	public Vector2 (Point2 toPoint, Point2 fromPoint) {
		set(toPoint, fromPoint);	
	}
		
	public void set(double x, double y) {
		//setX(x);	setY(y);
		this.x.value = x; this.y.value = y; //faster
	}
	public void set(Vector2 vector) {
		//set(v.getX(), v.getY());
		this.x.value = vector.x.value; this.y.value = vector.y.value; //faster
	}
	
	public void set(Point2 toPoint, Point2 fromPoint) {
		x.value = toPoint.x.value - fromPoint.x.value;
		y.value = toPoint.y.value - fromPoint.y.value; 
	}
	
	/**
	 * Compute length of this vector in plane XY 
	 * @return length of this vector
	 */
	public double length2() {
		return Math.sqrt(x.value * x.value + y.value * y.value);
	}
	
	/**
	 * Sets length of this vector to 1.0 saving angle by dividing on length. Do nothing if length is 0 
	 * @return old length of this vector before set
	 */
	public double restore() { 
		//double lenght = length2(); //slow
		double lenght = Math.sqrt(this.x.value * this.x.value + this.y.value * this.y.value);
		if (lenght > 0) { 
			this.x.value /= lenght;
			this.y.value /= lenght;
		} 
		return lenght;
	}
	
	/**
	 * Compute angle between this vector and normal vector (x=0, y=-1) in rads 
	 * @return angle between this vector and normal in the range [-PI ... PI]
	 */
	public double computeAngle() {
		double lenght = Math.sqrt(x.value * x.value + y.value * y.value);
		if (lenght > 0) return (x.value > 0)? Math.acos(-y.value / lenght): -Math.acos(-y.value / lenght);			
		else return 0; 
	}
	
	/**
	 * Compute angle between this vector and param vector in rads
	 * @param vector - vector to compute angle between 
	 * @return angle between this vector and param vector in the range [-PI ... PI]
	 */
	public double computeAngle(Vector2 vector) {
		double angle = vector.computeAngle() - computeAngle();
		if (angle > Math.PI) angle -= 2 * Math.PI;
		else if (angle < -Math.PI) angle += 2 * Math.PI;
		return angle;			
	}
	
	private double oldX, oldY, sinAngle, cosAngle;
	/**
	 * Rotate this vector by clockwise angle in rads
	 * @param angle - angle in rads to rotate   
	 * @return this vector after rotate
	 */
	public Vector2 rotate(double angle) { 	
		oldX = this.x.value; oldY = this.y.value;
		sinAngle = Math.sin(angle);
		cosAngle = Math.cos(angle);
		x.value = oldX * cosAngle - oldY * sinAngle;
		y.value = oldX * sinAngle + oldY * cosAngle;
		return this;
	}
	public Vector2 turnBack() { 
		x.value = -x.value; y.value = -y.value;
		return this;
	}
	public Vector2 toNormal() { 
		x.value = -y.value; y.value = x.value;
		return this;
	}
	public Vector2 rotateRightNinety() { 
		x.value = -y.value; y.value = x.value;
		return this;
	}
	public Vector2 rotateLeftNinety() { 
		x.value = y.value; y.value = -x.value;
		return this;
	}
	//returns new vector (bad)
	public Vector2 plus(Vector2 vector) {	//сложить вектора вернув новый 
		return new Vector2(getX() + vector.getX(), getY() + vector.getY()); 
	}
	public Vector2 minus(Vector2 vector) {	//вычесть вектора вернув новый 
		return new Vector2(getX() - vector.getX(), getY() - vector.getY()); 
	}
	public Vector2 multi(double c) {	//умножить вектор на число вернув новый 
		return new Vector2(getX() * c, getY() * c); 
	}
	
	/**
	 * Multiply all coordinates by c
	 * @param c - number to multiply  
	 * @return this
	 */
	public Vector2 selfMulti(double c) {
		x.value *= c;
		y.value *= c;
		return this; 
	}
	
	/**
	 * Addict a vector to this
	 * @param vector - vector to add  
	 * @return this
	 */
	public Vector2 addict(Vector2 vector) { 
		x.value += vector.x.value;
		y.value += vector.y.value;
		return this;
	}
	
	/**
	 * Same as addict but backward
	 * @param vector - vector to decrease  
	 * @return this
	 */
	public Vector2 decrease(Vector2 vector) { 
		x.value -= vector.x.value;
		y.value -= vector.y.value;
		return this;
	}
	
	/**
	 * Compute projection of this vector on vector (x, y). Returns 0 if length of (x, y) is 0
	 * @param x - x coordinate of axis vector  
	 * @param y - y coordinate of axis vector  
	 * @return signed length of projection of this vector on vector (x, y)
	 */
	public double myProectionOnVector(double x, double y) {
		double length = Math.sqrt(x * x + y * y);
		if (length > 0) return (this.x.value * x + this.y.value * y) / length;
		else return 0; 
	}
	/**
	 * Compute projection of this vector on identity (length=1.0) vector (x, y).
	 * @param v - axis vector  
	 * @return signed length of projection of this vector on vector v
	 */
	public double myProectionOnIdentityVector(Vector2 v) {
		return (this.x.value * v.x.value + this.y.value * v.y.value);
	}
	
	/**
	 * Compute projection of this vector on vector v. Returns 0 if length of (x, y) is 0
	 * @param v - axis vector  
	 * @return signed length of projection of this vector on vector v
	 */
	public double myProectionOnVector(Vector2 v) {
		//double length = v.length2(); //slow
		double length = Math.sqrt(v.x.value * v.x.value + v.y.value * v.y.value); //faster
		if (length > 0) return ((this.x.value * v.x.value + this.y.value * v.y.value) / length);
		else return 0; 
	}
	
	/**
	 * Compute absolute projection of this vector on vector v. Returns 0 if length of (x, y) is 0
	 * @param vector - axis vector  
	 * @return unsigned length of projection of this vector on vector v
	 */
	public double myAbsProectionOnVector(Vector2 vector) {	//проекция меня на вектор который передается параметром
		double k = myProectionOnVector(vector);
		if (k > 0) return k;
		else return -k;
	}
	
	/**
	 * Compute vector projection of this vector on param vector and return new instance of Vector2. 
	 * Returns (0, 0) if length of param vector is 0
	 * @param vector - axis vector  
	 * @return new instance of Vector2 which is projection of this on param vector  
	 */
	public Vector2 myVectorProectionOnVector(Vector2 vector) {
		//double length = vector.length2();
		double length = Math.sqrt(vector.x.value * vector.x.value + vector.y.value * vector.y.value);
		if (length > 0) return vector.multi( (x.value * vector.x.value + y.value * vector.y.value) / length); 
		else return new Vector2(0, 0); 
	}
	
	/**
	 * Compute vector projection of this vector on param vector and set this to result. 
	 * Sets this to (0, 0) if length of param vector is 0
	 * @param vector - axis vector  
	 * @return this
	 */	
	public Vector2 toVectorProectionOnVector(Vector2 vector) {
		double length = vector.length2();
		double vx = vector.x.value, vy = vector.y.value;
		if (length > 0) {
			double p = (x.value * vx + y.value * vy) / length;
			x.value = vx * p;
			y.value = vy * p;
		} else {
			x.value = 0; y.value = 0;
		}
		return this; 
	}
	
	public void setValue(DataInterface data) {
		if (data instanceof Vector2) set((Vector2) data);
	}
	
	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		if (name.contains(this.getName())) {
			if (name.contains(":x")) {
				x.setValue(data);
				return true;
			} else if (name.contains(":y")) {
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
			} else if (name.contains(":y")) {
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
