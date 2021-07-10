package ru.framework2d.graphics.draw.shadows;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import ru.framework2d.data.Vector3;
import ru.framework2d.graphics.GraphicalPeriphery;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class SubRelief extends ComponentsHandler {
	
	private static final String LOG_TAG = "Output";
	
	private Paint paint;
	private GraphicalPeriphery outputDirect;
	
	public SubRelief() {
		super(1, (Class <Component>) OutputGraphicalRelief.class.asSubclass(Component.class), 
				(Class <Component>) OutputGraphicalLight.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			if (engine instanceof GraphicalPeriphery) {
				outputDirect = (GraphicalPeriphery) engine; 
				paint = outputDirect.getPaint();
				break;
			}
		}
	}
	
	private ArrayList <OutputGraphicalRelief> lstReliefs = new ArrayList <OutputGraphicalRelief>();
	
	private OutputGraphicalLight light;
	
	@Override
	public Component connectComponent(Component component) {
		
		if (component instanceof OutputGraphicalRelief) {
			
			OutputGraphicalRelief newRelief = (OutputGraphicalRelief) component;
			
			if (!lstReliefs.contains(newRelief)) {
				lstReliefs.add(newRelief);
				if (light != null) {
					setRelief(newRelief, light);
				}
			}
			
			newRelief.reliefSprite = resources.getSprite(newRelief.reliefName.value,
					newRelief.spriteCollumnsNum.value, 
					newRelief.spriteRowsNum.value); 
		} 
		else if (component instanceof OutputGraphicalLight) {
			
			light = (OutputGraphicalLight) component;
		}
		return component;
	}
	
	private Canvas canvas;
	
	private long currentTick = -1;
	
	@Override
	public boolean toHandleComponent(Component reflection) {

		boolean handled = false;
		
		if (currentTick != enginesScheduler.currentTick) {
			canvas = outputDirect.getCanvas();
			currentTick = enginesScheduler.currentTick;
		}
		
		if (canvas != null) {
			
			drawRelief((OutputGraphicalRelief) reflection, canvas);
			
			handled = true;
		} 
		return handled;
	}
	
	private float px, py, sx, sy, angle;
	private int stateCount, currentAlpha, savedAlpha;
	
	private Rect drawingArea = new Rect();

	private ColorFilter bufferingFilter;
	
	private void drawRelief(OutputGraphicalRelief relief, Canvas canvas) {
//		px = (float) relief.position.x.value; 		py = (float) relief.position.y.value;
//		angle = 0;
//		if (relief.isRotatable.value) {
//			angle += (float) Math.toDegrees(relief.position.getAlpha());
//		}
		sx = (float) relief.size.x.value / 2;		sy = (float) relief.size.y.value / 2;
//		
//		px += relief.localPos.x.value;	py += relief.localPos.y.value;
//		if (relief.isLocalRotatable.value) {
//			angle += Math.toDegrees(relief.localPos.getAlpha());
//		}
		
		savedAlpha = paint.getAlpha();
		//currentAlpha = (int) ((float) savedAlpha * relief.transparency.value * relief.localTransparency.value * (1.0f - light.diffuse.value));
		currentAlpha = (int) ((float) savedAlpha * (1.0f - light.diffuse.value));
			
		if (currentAlpha > 0) {
			
			if (currentAlpha != savedAlpha) paint.setAlpha(currentAlpha);
			
//			stateCount = canvas.save();
//			{
//				canvas.translate(px, py);
//				if (angle != 0) canvas.rotate(angle);
				drawingArea.set((int)-sx, (int)-sy, (int) sx, (int) sy);
				bufferingFilter = paint.getColorFilter();
				paint.setColorFilter(light.diffuseFilter);
				canvas.drawBitmap(relief.reliefSprite.spriteImage, 
									relief.reliefSprite.lstSpritesRects.get(
											relief.currentSpriteNo.value), drawingArea, paint);
				paint.setColorFilter(bufferingFilter);
//				
//			}
//			canvas.restoreToCount(stateCount);
			
			if (currentAlpha != savedAlpha) paint.setAlpha(savedAlpha);

		}
	}
	
	boolean transfered;
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		
		transfered = false;
		if (reflection instanceof OutputGraphicalLight) {
			OutputGraphicalLight light = (OutputGraphicalLight) reflection;
			if (data.getContext() == light.position.getContext()) {
				transfered = true;
		
				for (OutputGraphicalRelief relief : lstReliefs) {
					setRelief(relief, light);
				}
			}
		} 
		else if (reflection instanceof OutputGraphicalRelief) {
		
			OutputGraphicalRelief relief = (OutputGraphicalRelief) reflection;
			
			if (data.getContext() == relief.position.getContext()) {
				transfered = true;
				if (data instanceof Position3) relief.position.set((Position3) data);
				else if (data instanceof Point2) {
					if (data instanceof Position2) relief.position.set((Position2) data);
					else relief.position.set((Point2) data);
				} 
				
				if (light != null) {
					setRelief(relief, light);
				}
				
			} 
			else if (data.getContext() == relief.localPos.getContext()) {
				transfered = true;
				if (data instanceof Vector2) {
					relief.localPos.setAlpha((float)((Vector2) data).computeAngle());
					Log.d(LOG_TAG, fullName + ": set " + relief.getShortClassName() + ".angle <" 
									+ data.getContext().name + "> to " 
									+ ((Vector2) data).getX() + " : " + ((Vector2) data).getY());
					outputDirect.update();
				} 
				else if (data instanceof Position3) {
					relief.localPos.set((Position3) data);
				} 
				else if (data instanceof Point2) {
					if (data instanceof Position2) relief.localPos.set((Position2) data);
					else relief.localPos.set((Point2) data);
				} 
			}
			if (data instanceof Bool) {
				if (data.getContext() == relief.isVisible.getContext()) {
					transfered = true;
					relief.isVisible.value = ((Bool) data).value;
					Log.d(LOG_TAG, fullName + ": set " + relief.entity.name + ".isVisible <" 
									+ data.getContext().name + "> to " + relief.isVisible.value);
					outputDirect.update();
				} 
				else if (data.getContext() == relief.isLocalVisible.getContext()) {
					transfered = true;
					relief.isLocalVisible.value = ((Bool) data).value;
					Log.d(LOG_TAG, fullName + ": set " + relief.entity.name + ".isLocalVisible <" 
									+ data.getContext().name + "> to " + relief.isVisible.value);
					outputDirect.update();
				}
			} 
			else if (data instanceof Fractional) {
				if (data.getContext() == relief.transparency.getContext()) {
					transfered = true;
					relief.transparency.value = ((Fractional) data).value;
					Log.d(LOG_TAG, fullName + ": set " + relief.entity.name + ".transparancy <" 
									+ data.getContext().name + "> to " + relief.transparency.value);
					outputDirect.update();
				} 
				else if (data.getContext() == relief.localTransparency.getContext()) {
					transfered = true;
					relief.localTransparency.value = ((Fractional) data).value;
					Log.d(LOG_TAG, fullName + ": set " + relief.entity.name 
									+ ".localTransparancy <" + data.getContext().name + "> to " 
									+ relief.transparency.value);
					outputDirect.update();
				}
			}
		}
		
		return transfered;
	}
	
	private Vector3 toLight = new Vector3();
	
	private double length;
	private double lightAngle;
	
	private void setRelief(OutputGraphicalRelief relief, OutputGraphicalLight light) {
		toLight.set(light.position, relief.position);
		length = toLight.length3();
		if (length > 0.001d) {
			lightAngle = Math.asin((light.position.z.value - relief.position.z.value) / length);
		} else lightAngle = 0;
		relief.currentSpriteNo.value = 2 + Math.abs((int) 
									Math.round((relief.spriteCollumnsNum.value * 
											relief.spriteRowsNum.value - 3) *
											(lightAngle * 2) / Math.PI));
		
		relief.localPos.setAlpha((float)(((Vector2) toLight).computeAngle() - Math.PI / 2));
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
