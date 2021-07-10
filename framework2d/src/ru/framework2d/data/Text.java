package ru.framework2d.data;


public class Text extends DataInterface {

	public String value = "";
	
	
	
	public Text() {}
	
	public Text(String text) { 
		value += text;
	}
	
	
	
	@Override
	public boolean setProperty(String name, String value) {
		if (this.getName().contentEquals(name)) {
			this.value = "" + value;
			return true;
		}
		return false;
	}

	
	
	public void setValue(DataInterface data) {
		if (data instanceof Text) {
			this.value = "" + ((Text) data).value;
		} else if (data instanceof Numeral) {
			this.value = "" + ((Numeral) data).value;
		} else if (data instanceof Fractional) {
			this.value = "" + ((Fractional) data).value;
		} else if (data instanceof DoubleFractional) {
			this.value = "" + ((DoubleFractional) data).value;
		} else if (data instanceof Bool) {
			this.value = "" + ((Bool) data).value;
		}
	}
	
	@Override
	public String toString() {
		return "" + value;
	}

}
