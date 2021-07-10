package ru.framework2d.data;



public class DoubleFractional extends DataInterface {
	
	public double value = 0;

	
	
	public DoubleFractional() {};
	
	public DoubleFractional(double value) { 
		this.value = value;
	};

	
	
	public void setValue(DataInterface data) {
		if (data instanceof DoubleFractional) {
			this.value = ((DoubleFractional) data).value;
		} else if (data instanceof Fractional) {
			this.value = ((Fractional) data).value;
		} else if (data instanceof Numeral) {
			this.value = ((Numeral) data).value;
		} else if (data instanceof Text) {
			this.value = Double.parseDouble(((Text) data).value);
		} else if (data instanceof Bool) {
			this.value = ((Bool) data).value? 1.0 : 0.0;
		}
	}
	
	public boolean setProperty(String name, String value) {
		if (this.getName().contentEquals(name)) {
			this.value = Double.parseDouble(value);
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "" + value;
	}

}
