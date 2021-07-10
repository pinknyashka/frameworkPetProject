package ru.framework2d.data;


public class Numeral extends DataInterface {
	
	public int value = 0;
	
	

	public Numeral() {}
	
	public Numeral(int value) { 
		this.value = value;
	}
	
	
	
	public void setValue(DataInterface data) {
		if (data instanceof Numeral) {
			this.value = ((Numeral) data).value;
		} else if (data instanceof Fractional) {
			this.value = (int) ((Fractional) data).value;
		} else if (data instanceof DoubleFractional) {
			this.value = (int) ((DoubleFractional) data).value;
		} else if (data instanceof Text) {
			this.value = (int) Float.parseFloat(((Text) data).value);
		} else if (data instanceof Bool) {
			this.value = ((Bool) data).value? 1 : 0;
		}
	}
	
	public boolean setProperty(String name, String value) {
		if (this.getName().contentEquals(name)) {
			this.value = (int) Double.parseDouble(value);
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "" + value;
	}

}
