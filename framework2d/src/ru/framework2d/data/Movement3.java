package ru.framework2d.data;

public class Movement3 extends Vector3 {
	
	public DoubleFractional currentSpeed = new DoubleFractional(0);
	
	public DoubleFractional alphaRotation = new DoubleFractional(0);
	public DoubleFractional betaRotation = new DoubleFractional(0);
	public DoubleFractional gammaRotation = new DoubleFractional(0);
	
	
	
	public Movement3() {}
	
	public Movement3(double mx, double my, double mz) {
		super(mx, my, mz);
	}
	
	public Movement3(Vector3 v) {
		this(v.x.value, v.y.value, v.z.value);
	}
	
	public Movement3(Movement3 m3) {
		setMovement(m3);
	}
	
	
	
	public void setMovement(Movement3 move) {
		set((Vector3) move);
		currentSpeed.value = move.currentSpeed.value;
		
		alphaRotation.value = move.alphaRotation.value;
		betaRotation.value = move.betaRotation.value;
		gammaRotation.value = move.gammaRotation.value;
	}
	
	
	
	public void stop() {
		this.currentSpeed.value = 0; alphaRotation.value = 0; betaRotation.value = 0; gammaRotation.value = 0;
	}
	
	public boolean isMoving() {
		return (currentSpeed.value != 0 || alphaRotation.value != 0 || betaRotation.value != 0 || gammaRotation.value != 0); 
	}

	
	
	@Override
	public void setValue(DataInterface data) {
		if (data instanceof Movement3) setMovement((Movement3) data);
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
				} else if (name.contains(":betaRotation")) {
					betaRotation.setValue(data);
					return true;
				} else if (name.contains(":gammaRotation")) {
					gammaRotation.setValue(data);
					return true;
				} 
				
			}
			return false;
			
		}
		return true;
	}
	
	
	
	@Override
	public String toString() {
		return "" + this.x.toString() + ":" + this.y.toString() + ":" + this.z.toString() + ":" + 
					this.alphaRotation.toString() + ":" + this.betaRotation.toString() + ":" + this.gammaRotation.toString();  
	}
}
