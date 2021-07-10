package ru.framework2d.graphics.draw.text;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.DataInterface;
import ru.framework2d.graphics.GraphicalPeriphery;
import android.graphics.Canvas;
import android.graphics.Paint;

public class SubDrawText extends ComponentsHandler {
	
	private GraphicalPeriphery graphicalPeriphery;
	private Paint paint;
	
	public SubDrawText() {
		super(1, (Class <Component>) OutputGraphicalText.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			
			if (engine instanceof GraphicalPeriphery) {
				
				graphicalPeriphery = (GraphicalPeriphery) engine; 
				paint = graphicalPeriphery.getPaint();
				
				break;
			}
		}
	}
	
	private Canvas canvas;
	private long currentTick = -1;
	
	@Override
	public boolean toHandleComponent(Component component) {
		
		if (currentTick != enginesScheduler.currentTick) {
			canvas = graphicalPeriphery.getCanvas();
			currentTick = enginesScheduler.currentTick;
		}
		
		if (canvas != null) {
			
			OutputGraphicalText text = (OutputGraphicalText) component;
			
			float sx = (float) text.size.x.value / 2;		
			float sy = (float) text.size.y.value / 2;
			
			int alpha = paint.getAlpha();
			
			paint.setColor(text.color.value);  //setColor killing setAlpha
			paint.setAlpha(alpha);
			
			paint.setTextSize(text.fontSize.value);
			
			canvas.drawText(text.text.value, -sx, sy, paint); //don't ask me why -sx and +sy
			
			return true;
		}
		return false;
	}
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean transfered = false;
		return transfered;
	}

	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		return false;
	}

}
