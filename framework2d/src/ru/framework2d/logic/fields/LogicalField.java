package ru.framework2d.logic.fields;

import java.util.ArrayList;

import ru.framework2d.core.Property;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;


public class LogicalField extends LogicalInterface {
	
	public final static String SHORT_CLASS_NAME = "field";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	
	public Text name = new Text("");
	public Point2 size = new Point2(0, 0);
	
	public Bool gotUnitInField = new Bool(false);
	public Bool gotMovingUnits = new Bool(false);
	
	public Fractional radius = new Fractional(0);
	public Bool isCircle = new Bool(false);
	public Bool isEnabled = new Bool(true);
	
	public ArrayList <LogicalUnit> lstUnitsInField = new ArrayList <LogicalUnit> (); 
	public ArrayList <LogicalUnit> lstUnits = new ArrayList <LogicalUnit> ();
	
	
	
	public LogicalField() {};
	
	
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("radius")) {
			radius.setValue(property.data);
			isCircle.value = true;
			size.set(radius.value * 2, radius.value * 2);
		}
	}
	
	public boolean checkEscape(Position3 place) {
		if (isCircle.value) {
			return place.dist(this.position) < radius.value;
		} else {
			return !place.checkField(
					(int) (position.x.value - size.getX() / 2), 
					(int) (position.y.value - size.getY() / 2), 
					(int) (position.x.value + size.getX() / 2), 
					(int) (position.y.value + size.getY() / 2), 0);
		}
	}	

}
