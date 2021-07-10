package ru.framework2d.data;

public class Movement2 extends Vector2 {
	
	public DoubleFractional currentSpeed = new DoubleFractional(0);
	
	public DoubleFractional alphaRotation = new DoubleFractional(0);

	
	
	public Movement2() {}
	
	public Movement2(double vx, double vy) {
		super(vx, vy);
	}
	
	public Movement2(Vector2 vector) {
		this(vector.getX(), vector.getY());
	}
	
	
	
	public void setMovement(Movement2 move) {
		super.set((Vector2) move);
		this.currentSpeed.value = move.currentSpeed.value;
		this.alphaRotation.value = move.alphaRotation.value;
	}
	
	
	
	public void stop() {
		currentSpeed.value = 0;
		alphaRotation.value = 0;
	}
	
	public boolean isMoving() {
		return (currentSpeed.value != 0 || alphaRotation.value != 0); 
	}
	
	
	
	public void setValue(DataInterface data) {
		if (data instanceof Movement2) setMovement((Movement2) data);
	}
	
	
	
	@Override
	public boolean setPropertyData(String name, DataInterface data) {
		
		if (!super.setPropertyData(name, data)) {
			
			if (name.contains(this.getName())) {
				if (name.contains(":currentSpeed")) {
					currentSpeed.setValue(data);
					return true;
				} else if (name.contains(":alphaRotation")) {
					alphaRotation.setValue(data);
					return true;
				} 
			}
			return false;
			
		}
		return true;
	}
	
}
