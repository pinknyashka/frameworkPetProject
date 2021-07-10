package ru.framework2d.data;


public class Fractional extends DataInterface {

	public float value = 0;
	
	

	public Fractional() {}
	
	public Fractional(float value) { 
		this.value = value;
	}
	

	
	public void setValue(DataInterface data) {
		if (data instanceof Fractional) {
			this.value = ((Fractional) data).value;
		} else if (data instanceof DoubleFractional) {
			this.value = (float) ((DoubleFractional) data).value;
		} else if (data instanceof Numeral) {
			this.value = ((Numeral) data).value;
		} else if (data instanceof Text) {
			this.value = Float.parseFloat(((Text) data).value);
		} else if (data instanceof Bool) {
			this.value = ((Bool) data).value? 1.0f : 0.0f;
		}
	}
	
	public boolean setProperty(String name, String value) {
		if (this.getName().contentEquals(name)) {
			this.value = (float) Double.parseDouble(value);
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "" + value;
	}

}
