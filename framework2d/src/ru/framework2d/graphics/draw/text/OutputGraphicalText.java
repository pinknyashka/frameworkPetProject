package ru.framework2d.graphics.draw.text;

import ru.framework2d.core.Property;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.graphics.OutputGraphicalInterface;
import android.graphics.Color;


public class OutputGraphicalText extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "text";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	
	public Text text = new Text("");
	
	public Fractional fontSize = new Fractional(14.0f);
	
	public Numeral color = new Numeral(Color.BLACK);

	
	
	@Override
	public void setPropertyData(Property property) {
		
		if (property.isNamed("color")) {
			color.value = Color.parseColor(property.data.toString()); 
		} 
		else {
			super.setPropertyData(property);
		}
	}

}
