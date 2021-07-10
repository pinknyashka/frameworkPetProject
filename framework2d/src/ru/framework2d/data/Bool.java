package ru.framework2d.data;


public class Bool extends DataInterface {
	
	public boolean value = false;

	
	
	public Bool() {}
	
	public Bool(boolean value) {
		this.value = value;
	}

	

	public void setValue(DataInterface data) {
		if (data instanceof Bool) {
			this.value = ((Bool) data).value;
		} 
		else if (data instanceof Text) {
			
			String text = ((Text) data).value;
			
			if (Character.isLetter(text.charAt(0))) {
				
				this.value = text.equalsIgnoreCase("true");
			} 
			else {
				this.value = ((int) Float.parseFloat(text) == 1);
			}
		} 
		else if (data instanceof Numeral) {
			this.value = (((Numeral) data).value == 1);
		} 
		else if (data instanceof Fractional) {
			this.value = (((Fractional) data).value == 1.0);
		} 
		else if (data instanceof DoubleFractional) {
			this.value = (((DoubleFractional) data).value == 1.0);
		}
	}

	public DataInterface getInnerData(String propertyName) {
		//there is no inner data
		return null;
	}
	
	public boolean setProperty(String name, String value) {
		if (this.getName().contentEquals(name)) {
			
			if (Character.isLetter(value.charAt(0))) {
				
				this.value = value.equalsIgnoreCase("true");
			} 
			else {
				this.value = ((int) Float.parseFloat(value) == 1);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "" + value;
	}
	
}
