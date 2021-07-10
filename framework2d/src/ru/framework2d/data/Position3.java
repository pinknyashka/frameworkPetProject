package ru.framework2d.data;


public class Position3 extends Point3 {
	
	public DoubleFractional alpha = new DoubleFractional();
	
	public double getAlpha() {
		return alpha.value;
	}
	
	public void setAlpha(double alpha) {
		this.alpha.value = alpha;
	}
	
	
	
	public DoubleFractional beta = new DoubleFractional();
	
	public double getBeta() {
		return beta.value;
	}
	
	public void setBeta(double beta) {
		this.beta.value = beta;
	}
	
	
	
	public DoubleFractional gamma = new DoubleFractional();
	
	public double getGamma() {
		return gamma.value;
	}
	
	public void setGamma(double gamma) {
		this.gamma.value = gamma;
	}
	
	

	public Position3() {};
	
	public Position3(double x, double y, double z) { set(x, y, z);};
	
	public Position3(double x, double y, double z, double alpha, double beta, double gamma) {
		set(x, y, z);
		this.setAlpha(alpha); this.setBeta(beta); this.setGamma(gamma);
	}
	
	public Position3 (Point3 pos3) { 
		set(pos3); 
	} 
	
	public Position3 (Position3 pos3) {
		set(pos3); 
	}
	
	
	
	public void set(double x, double y, double z, double alpha, double beta, double gamma) {
		
		set(x, y, z); setAlpha(alpha); setBeta(beta); setGamma(gamma);
	}
	
	@Override
	public void set(Point3 p3) {
		super.set(p3);
		setAlpha(0); setBeta(0); setGamma(0);
	}
	
	public void set(Position2 pos2) {
		
		x.value = pos2.x.value;
		y.value = pos2.y.value;
		z.value = 0;
		
		alpha.value = pos2.getAlpha();
		beta.value = 0;
		gamma.value = 0;
	}
	
	public void set(Position3 pos3) {
		
		x.value = pos3.x.value;
		y.value = pos3.y.value;
		z.value = pos3.z.value;
		
		alpha.value = pos3.alpha.value;
		beta.value = pos3.beta.value;
		gamma.value = pos3.gamma.value;
	}
	
	
	
	public void setValue(DataInterface data) {
		if (data instanceof Position3)	set((Position3) data);
	}
	
	public DataInterface getInnerData(String propertyName) {
		
		if (propertyName.contentEquals("alpha")) {
			return alpha;
		} 
		else if (propertyName.contentEquals("beta")) {
			return beta;
		} 
		else if (propertyName.contentEquals("gamma")) {
			return gamma;
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
				else if (name.contains(":beta")) {
					beta.setValue(data);
					return true;
				} 
				else if (name.contains(":gamma")) {
					gamma.setValue(data);
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
				else if (name.contains(":beta")) {
					this.beta.value = Double.parseDouble(value);
					return true;
				} 
				else if (name.contains(":gamma")) {
					this.gamma.value = Double.parseDouble(value);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		
		return "" + this.x.toString() + ":" + this.y.toString() + ":" + this.z.toString() + ":" + 
					this.alpha.toString() + ":" + this.beta.toString() + ":" + this.gamma.toString();  
	}
}
