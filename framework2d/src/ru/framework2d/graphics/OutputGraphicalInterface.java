package ru.framework2d.graphics;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Property;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position3;
import android.graphics.Canvas;
import android.graphics.Paint;


public abstract class OutputGraphicalInterface extends Component {
	
	public Position3 localPos = new Position3(0, 0, 0);
	
	public Fractional layer = new Fractional(0);
	
	public Point2 size = new Point2(0, 0);
	public Bool isCircle = new Bool(false);
	public Fractional radius = new Fractional(0.0f);

	public Fractional transparency = new Fractional(1.0f);
	public Fractional localTransparency = new Fractional(1.0f);
	
	public Bool isVisible = new Bool(true); 
	public Bool isLocalVisible = new Bool(true); 

	public Bool isRotatable = new Bool(true);
	public Bool isLocalRotatable = new Bool(true);
	
	public ArrayList <ComponentsHandler> lstProcessingEngines = new ArrayList <ComponentsHandler> ();
	public ArrayList <ComponentsHandler> lstProcessingSubHandlers = new ArrayList <ComponentsHandler> ();
	
	public OutputGraphicalInterface parentGraphical;
	public ArrayList <OutputGraphicalInterface> lstChildren = new ArrayList <OutputGraphicalInterface> ();
	
	public int transformState = 0;
	
	

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
	
	
	
	private int currentAlpha, savedAlpha;

	public void draw(Canvas canvas, Paint paint) {
		
		savedAlpha = paint.getAlpha();
		currentAlpha = (int) ((float) savedAlpha * transparency.value * localTransparency.value);
			
		if (currentAlpha > 0) {
			
			if (currentAlpha != savedAlpha) paint.setAlpha(currentAlpha);
			
			int stateCount = canvas.save();
			{
				canvas.translate((float) position.x.value + (float) localPos.x.value, 
								(float) position.y.value + (float) localPos.y.value);
				
				float angle = 0;
				
				if (isRotatable.value) {
					
					angle += (float) Math.toDegrees(position.getAlpha());
				}
				
				if (isLocalRotatable.value) {
					
					angle += Math.toDegrees(localPos.getAlpha());
				}
				
				if (angle != 0) canvas.rotate(angle);
			}
			
			selfDraw();
			
			for (OutputGraphicalInterface graphical : lstChildren) {
				graphical.draw(canvas, paint);
			}
			
			canvas.restoreToCount(stateCount);
			
			if (currentAlpha != savedAlpha) paint.setAlpha(savedAlpha);	
		}
		
	}

	
	
	private int processedEnginesNum = 0;

	private void selfDraw() {
		
		processedEnginesNum = lstProcessingEngines.size();
		
		for (int engineNo = 0; engineNo < processedEnginesNum; engineNo++) {
			
			lstProcessingEngines.get(engineNo).toHandleComponent(this);
		}
	}
	
}

