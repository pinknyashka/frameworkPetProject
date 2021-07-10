package ru.framework2d.graphics.cameras;

import ru.framework2d.core.Property;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Point3;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.data.Vector2;

import ru.framework2d.graphics.OutputGraphicalInterface;

public class OutputGraphicalCamera extends OutputGraphicalInterface {
	
	public Text name = new Text("");
		
	public Bool isEnabled = new Bool(true);
	
	public Position3 eyePos = new Position3();

	public Point3 recordingAreaSize = new Point3();

	public Point3 outputAreaSize = new Point3();
	
	public Bool isTouchable = new Bool(true);
	
	public Fractional zoom = new Fractional(1.0f);
	public Fractional zoomMin = new Fractional(0.1f);
	public Fractional zoomMax = new Fractional(10.0f);
	
	
	
	public final static String SHORT_CLASS_NAME = "camera";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	
	private Vector2 vToPoint = new Vector2();
	
	private Position2 localPosition = new Position2();
	
	public Position2 getLocaleCoordinate(Position2 pos) {
		vToPoint.set(pos, position); //взял в координатах экрана
		vToPoint.rotate(-position.getAlpha());//повернул на угол камеры
		localPosition.set(	eyePos.getX() + (float) vToPoint.getX() / zoom.value, 
							eyePos.getY() + (float) vToPoint.getY() / zoom.value,
							pos.getAlpha() - (float) position.getAlpha());
		return localPosition;
	}
	
	public Position2 getLocaleCoordinate(Position3 pos) {
		vToPoint.set(pos, position); //взял в координатах экрана
		vToPoint.rotate(-position.getAlpha());//повернул на угол камеры
		localPosition.set(	eyePos.getX() + (float) vToPoint.getX() / zoom.value, 
							eyePos.getY() + (float) vToPoint.getY() / zoom.value,
							pos.getAlpha() - (float) position.getAlpha());
		return localPosition;
	}
	
	private Point2 localPoint = new Point2();
	
	public Point2 getLocaleCoordinate(Point2 point) {
		vToPoint.set(point, position); 
		vToPoint.rotate(-position.getAlpha()); //повернул на угол камеры rotate to camera angle
		localPoint.set(	eyePos.getX() + (float) vToPoint.getX() / zoom.value, 
						eyePos.getY() + (float) vToPoint.getY() / zoom.value);
		return localPoint;
	}
	
	
	
	private Position2 globalPosition = new Position2();
	
	public Position2 getScreenCoordinate(Position2 pos) {
		vToPoint.set(pos, eyePos);
		vToPoint.rotate(position.getAlpha());
		globalPosition.set(	position.x.value + (float) vToPoint.getX() * zoom.value, 
							position.y.value + (float) vToPoint.getY() * zoom.value, 
							pos.getAlpha() + (float) position.getAlpha());
		return globalPosition;
	}
	
	private Point2 globalPoint = new Point2();
	
	public Point2 getScreenCoordinate(Point2 point) {
		vToPoint.set(point, eyePos);
		vToPoint.rotate(position.getAlpha());
		globalPoint.set(position.x.value + (float) vToPoint.getX() * zoom.value, 
						position.y.value + (float) vToPoint.getY() * zoom.value);
		return globalPoint;
	}
	
	
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("position:x")) {
			
			position.x.setValue(property.data);
			eyePos.x.setValue(property.data);
		} 
		else if (property.isNamed("position:y")) {
			
			position.y.setValue(property.data);
			eyePos.y.setValue(property.data);
		} 
	}

}
