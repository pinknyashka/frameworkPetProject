package ru.framework2d.graphics.draw.shadows;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;

import ru.framework2d.data.DataInterface;
import ru.framework2d.data.DoubleFractional;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;

import ru.framework2d.graphics.GraphicalPeriphery;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;

import android.util.Log;

public class SubLight extends ComponentsHandler {
	private static final String LOG_TAG = "Output";
	
	private GraphicalPeriphery outputDirect;
	
	public SubLight() {
		super(1, (Class <Component>) OutputGraphicalLight.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			if (engine instanceof GraphicalPeriphery) {
				outputDirect = (GraphicalPeriphery) engine; 
				paint = outputDirect.getPaint();
				break;
			}
		}
	}
	
	
	private OutputGraphicalLight light;
	
	@Override
	public Component connectComponent(Component component) {
		if (component instanceof OutputGraphicalLight) {
			light = (OutputGraphicalLight) component;
			
			light.drawingRect = new RectF((float)-light.size.x.value/2, (float)-light.size.x.value/2, 
					(float)light.size.x.value/2, (float)light.size.y.value/2);
			light.drawingArea = new Path();
			light.drawingArea.addRect(light.drawingRect, Path.Direction.CW);
			
			light.position.localCommit(null, light, light.position);
			
			
			gradient = new GradientDrawable(GradientDrawable.Orientation.TR_BL, new int [] {
					light.color.value, 
					Color.argb(0, 255, 255, 255)});
			gradient.mutate();
		    gradient.setShape(GradientDrawable.OVAL); 
		    gradient.setGradientType(GradientDrawable.RADIAL_GRADIENT); 
		    gradient.setGradientRadius((int)light.size.x.value / 2); 
			gradient.setGradientCenter(0.5f, 0.5f);
		    gradient.setSize((int)(light.size.x.value / 4), (int) (light.size.y.value / 4));
		    gradient.setAlpha(128); 
	        paint.setColorFilter(light.colorFilter);
			
		}
		return component;
	}
	
	
	private Paint paint;
	private Canvas canvas;
	private float px, py;
	private int savedAlpha, currentAlpha;
	private int stateCount;
	
	private GradientDrawable gradient;
	
	@Override
	public boolean toHandleComponent(Component reflection) {
		boolean handled = false;
		
//		canvas = outputDirect.getCanvas();
//		if (canvas != null && light.glow.value) {
//			
//			px = (float) light.position.x.value; 		py = (float) light.position.y.value;
//			
//			savedAlpha = paint.getAlpha();
//			currentAlpha = (int) ((float) savedAlpha * light.transparency.value * light.localTransparency.value * light.intensity.value);
//				
//			if (currentAlpha > 0) {
//				
//				if (currentAlpha != savedAlpha) paint.setAlpha(currentAlpha);
//				
//				stateCount = canvas.save();
//				{
//					canvas.translate(px, py);
//					canvas.clipPath(light.drawingArea);
//					paint.setColor(light.color.value);  //setColor killing setAlpha
//					paint.setAlpha(currentAlpha);
//					
//					gradient.setBounds((int) light.drawingRect.left, (int) light.drawingRect.top, 
//							(int) light.drawingRect.right, (int) light.drawingRect.bottom);
//					
//					
//					gradient.draw(canvas);
//					paint.setAlpha(savedAlpha);
//				}
//				canvas.restoreToCount(stateCount);
//				
//				if (currentAlpha != savedAlpha) paint.setAlpha(savedAlpha);
//				handled = true;
//			}
//		} 
		return handled;
	}
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		
		boolean transfered = false;
		
		OutputGraphicalLight light = (OutputGraphicalLight) reflection;
		
		if (data.getContext() == light.position.getContext()) {
			transfered = true;
			
			if (data instanceof Position3) {
				light.position.set((Position3) data);
			}
			else if (data instanceof Point2) {
				if (data instanceof Position2) {
					light.position.set((Position2) data);
				}
				else light.position.set((Point2) data);
			} 
			
			long timerMark7 = System.currentTimeMillis();
			//light.position.localCommit(null, light, light.position);
			Log.d("Shadows", "Move light to " + light.position.x.value + ":" + light.position.y.value + "; time spent to move : " + (System.currentTimeMillis() - timerMark7) + " ms; ");
			
			((Engine) outputDirect).setWorkState(true);
		} 
		else if (data.getContext() == light.localPos.getContext()) {
			
			transfered = true;
			
			if (data instanceof Vector2) {
				light.localPos.setAlpha((float)((Vector2) data).computeAngle());
				Log.d(LOG_TAG, fullName + ": set " + light.getShortClassName() + ".angle <" 
								+ data.getContext().name + "> to " 
								+ ((Vector2) data).getX() + " : " + ((Vector2) data).getY());
				((Engine) outputDirect).setWorkState(true);
			} 
			else if (data instanceof Position3) {
				light.localPos.set((Position3) data);
			} 
			else if (data instanceof Point2) {
				if (data instanceof Position2) light.localPos.set((Position2) data);
				else light.localPos.set((Point2) data);
			} 
		} 
		else if (data.getContext() == light.color.getContext()) {
			
			if (data instanceof Numeral) {
				transfered = true;
				light.setColor(((Numeral) data).value);
			} 
			else if (data instanceof Fractional) {
				transfered = true;
				light.setColor((int) ((Fractional) data).value);
			} 
			else if (data instanceof DoubleFractional) {
				transfered = true;
				light.setColor((int) ((DoubleFractional) data).value);
			} 
			
			if (transfered) {
				Log.d(LOG_TAG, fullName + ": set " + light.entity.name + ".color <" 
						+ data.getContext().name + "> to " + light.color.value + ";");
		        paint.setColorFilter(light.colorFilter);
		        ((Engine) outputDirect).setWorkState(true);
			}
			
		} 
		else if (data.getContext() == light.intensity.getContext()) {
			
			if (data instanceof Numeral) {
				transfered = true;
				light.setIntensity(((Numeral) data).value);
			} 
			else if (data instanceof Fractional) {
				transfered = true;
				light.setIntensity((int) ((Fractional) data).value);
			} 
			else if (data instanceof DoubleFractional) {
				transfered = true;
				light.setIntensity((int) ((DoubleFractional) data).value);
			} 
			
			if (transfered) {
				Log.d(LOG_TAG, fullName + ": set " + light.entity.name + ".intensity <" 
						+ data.getContext().name + "> to " + light.intensity.value + ";");
		        paint.setColorFilter(light.colorFilter);
		        ((Engine) outputDirect).setWorkState(true);
			}
			
		} 
		else if (data.getContext() == light.diffuse.getContext()) {
			
			if (data instanceof Numeral) {
				transfered = true;
				light.setDiffuse(((Numeral) data).value);
			} 
			else if (data instanceof Fractional) {
				transfered = true;
				light.setDiffuse((int) ((Fractional) data).value);
			} 
			else if (data instanceof DoubleFractional) {
				transfered = true;
				light.setDiffuse((int) ((DoubleFractional) data).value);
			} 
			
			if (transfered) {
				Log.d(LOG_TAG, fullName + ": set " + light.entity.name + ".diffuse <" 
						+ data.getContext().name + "> to " + light.diffuse.value + ";");
		        paint.setColorFilter(light.colorFilter);
		        ((Engine) outputDirect).setWorkState(true);
			}
			
		} 
		else if (data.getContext() == light.red.getContext()) {
			if (data instanceof Numeral) {
				transfered = true;
				light.setRed(((Numeral) data).value);
			} 
			else if (data instanceof Fractional) {
				transfered = true;
				light.setRed((int) ((Fractional) data).value);
			} 
			else if (data instanceof DoubleFractional) {
				transfered = true;
				light.setRed((int) ((DoubleFractional) data).value);
			} 
			if (transfered) {
				Log.d(LOG_TAG, fullName + ": set " + light.entity.name + ".red <" 
						+ data.getContext().name + "> to " + light.red.value + ";");
		        paint.setColorFilter(light.colorFilter);
		        ((Engine) outputDirect).setWorkState(true);
			}
			
		} 
		else if (data.getContext() == light.green.getContext()) {
			
			if (data instanceof Numeral) {
				transfered = true;
				light.setGreen(((Numeral) data).value);
			} 
			else if (data instanceof Fractional) {
				transfered = true;
				light.setGreen((int) ((Fractional) data).value);
			} 
			else if (data instanceof DoubleFractional) {
				transfered = true;
				light.setGreen((int) ((DoubleFractional) data).value);
			} 
			
			if (transfered) {
				Log.d(LOG_TAG, fullName + ": set " + light.entity.name + ".green <" 
						+ data.getContext().name + "> to " + light.green.value + ";");
		        paint.setColorFilter(light.colorFilter);
		        ((Engine) outputDirect).setWorkState(true);
			}
			
		} 
		else if (data.getContext() == light.blue.getContext()) {
			
			if (data instanceof Numeral) {
				transfered = true;
				light.setBlue(((Numeral) data).value);
			} 
			else if (data instanceof Fractional) {
				transfered = true;
				light.setBlue((int) ((Fractional) data).value);
			} 
			else if (data instanceof DoubleFractional) {
				transfered = true;
				light.setBlue((int) ((DoubleFractional) data).value);
			} 
			
			if (transfered) {
				Log.d(LOG_TAG, fullName + ": set " + light.entity.name + ".blue <" 
						+ data.getContext().name + "> to " + light.blue.value + ";");
		        paint.setColorFilter(light.colorFilter);
		        ((Engine) outputDirect).setWorkState(true);
			}
			
		}
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
