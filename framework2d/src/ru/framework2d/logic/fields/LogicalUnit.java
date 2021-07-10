package ru.framework2d.logic.fields;

import java.util.ArrayList;

import ru.framework2d.core.Property;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;


public class LogicalUnit extends LogicalInterface {

	public final static String SHORT_CLASS_NAME = "unit";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	
	public Text fieldName = new Text(""); 
	
	public Bool isEnabled = new Bool(false);
	public Bool inField = new Bool(true);
	
	public ArrayList <LogicalField> lstFieldsWhereIAm = new ArrayList <LogicalField> (); 
	public ArrayList <LogicalField> lstFieldsToCheck = new ArrayList <LogicalField> ();
	
	public ArrayList <String> lstFieldsNameToCheck = new ArrayList <String> ();

	
	
	public LogicalUnit() { }
	
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("fieldName")) {
			boolean gotField = false;
			for (String fieldName : lstFieldsNameToCheck) {
				if (fieldName.contentEquals(property.data.toString())) gotField = true; 	
			}
			if (!gotField) lstFieldsNameToCheck.add(property.data.toString());
			this.fieldName.setValue(property.data);
		} 
	}
}
