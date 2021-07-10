package ru.framework2d.touches.areas;

import java.util.ArrayList;

import ru.framework2d.core.Property;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;

import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.WorldPointer;


public class TouchableArea extends InputTouchInterface {
	
	public final static String SHORT_CLASS_NAME = "touchable";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Point2 dragPoint = new Point2();

	public Bool isClikable = new Bool(true);

	public Bool isSelected = new Bool(false);
	
	public Point2 size = new Point2(0, 0);
	public Bool isCircle = new Bool(false);
	public Fractional radius = new Fractional(0);
	
	//ArrayList <WorldPointer> lstPointers = new ArrayList <WorldPointer> (); //possible duplicating with each clone
	public ArrayList <WorldPointer> lstPointers;
	
	public TouchableArea() {}

	public boolean touched(Point2 touch) {
		if (isClikable.value) {
			if (isCircle.value) {
				if (position.dist(touch) < radius.value) {
					this.dragPoint.set(	touch.getX() - position.x.value, 
										touch.getY() - position.y.value);
					return true;
				}
			} 
			else if (position.checkField(
									(int) (touch.getX() - size.getX() / 2), 
									(int) (touch.getY() - size.getY() / 2), 
									(int) (touch.getX() + size.getX() / 2), 
									(int) (touch.getY() + size.getY() / 2), 0)) {
				
				this.dragPoint.set(	touch.getX() - position.x.value, 
									touch.getY() - position.y.value);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("radius")) {
			radius.setValue(property.data);
			size.setX(radius.value * 2);
			size.setY(size.getX());
			isCircle.value = true;
		}
	}

}
