package ru.framework2d.graphics.draw.shadows;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.graphics.GraphicalPeriphery;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public class SubShadowLayers extends ComponentsHandler {
	private Paint paint;
	private GraphicalPeriphery outputDirect;
	
	public SubShadowLayers() {
		super(1, (Class <Component>) OutputGraphicalShadowLayer.class.asSubclass(Component.class), 
				(Class <Component>) OutputGraphicalLight.class.asSubclass(Component.class), 
				(Class <Component>) OutputGraphicalShadow.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			if (engine instanceof GraphicalPeriphery) {
				outputDirect = (GraphicalPeriphery) engine; 
				paint = outputDirect.getPaint();
				break;
			}
		}
	}
	
	private Canvas canvas;
	
	private int stateCount, currentAlpha, savedAlpha;
	private long currentTick = -1;
	
	private ArrayList <OutputGraphicalShadow> lstShadows = new ArrayList <OutputGraphicalShadow>();
	private ArrayList <OutputGraphicalShadow> lstStaticShadows = new ArrayList <OutputGraphicalShadow>();
	private ArrayList <OutputGraphicalShadow> lstActiveShadows = new ArrayList <OutputGraphicalShadow>();
	private ArrayList <OutputGraphicalShadow> lstCurrentActiveShadows = new ArrayList <OutputGraphicalShadow>();
	private ArrayList <OutputGraphicalShadowLayer> lstShadowLayers = new ArrayList <OutputGraphicalShadowLayer>();
	
	private OutputGraphicalLight light;
	
	@Override
	public Component connectComponent(Component component) {
		
		if (component instanceof OutputGraphicalShadow) {
			
			OutputGraphicalShadow newShadow = (OutputGraphicalShadow) component;
			
			if (!newShadow.entity.isPrototype) {
				if (!lstShadows.contains(newShadow)) {
					lstShadows.add(newShadow);
				}
				if (!lstStaticShadows.contains(newShadow)) {
					lstStaticShadows.add(newShadow);
				}
				if (!lstShadowLayers.isEmpty()) computeShadowArea(lstShadowLayers.get(0));
			}
		} 
		else if (component instanceof OutputGraphicalShadowLayer) {
			
			OutputGraphicalShadowLayer newShadowLayer = (OutputGraphicalShadowLayer) component;
			
			if (!lstShadowLayers.contains(newShadowLayer)) {
				lstShadowLayers.add(newShadowLayer);
				newShadowLayer.drawingArea = new Path();
				newShadowLayer.shadowArea = new Path();
				computeDrawingArea(newShadowLayer);
			}
		} 
		else if (component instanceof OutputGraphicalLight) {
			
			light = (OutputGraphicalLight) component;
			
			for (OutputGraphicalShadowLayer shadowLayer : lstShadowLayers) {
				computeShadowArea(shadowLayer);
			}
		}
		return component;
	}
	
	
	
	private ColorFilter bufferFilter;
	
	private float px, py, angle;
	
	long logTimerMark;
	
	//private Path sumPath = new Path();
	
	@Override
	public boolean toHandleComponent(Component reflection) {

		boolean handled = false;
		
		if (currentTick != enginesScheduler.currentTick) {
			canvas = outputDirect.getCanvas();
			currentTick = enginesScheduler.currentTick;
		}
		 
		if (canvas != null) {
			logTimerMark = System.currentTimeMillis();

			OutputGraphicalShadowLayer shadowLayer = (OutputGraphicalShadowLayer) reflection;
			
			px = (float) shadowLayer.position.x.value; 		py = (float) shadowLayer.position.y.value;
			angle = 0;
			if (shadowLayer.isRotatable.value) {
				angle += (float) Math.toDegrees(shadowLayer.position.getAlpha());
			}
			//sx = shadow.size.x.value / 2;		sy = shadow.size.y.value / 2;
			
			px += shadowLayer.localPos.x.value;	py += shadowLayer.localPos.y.value;
			if (shadowLayer.isLocalRotatable.value) {
				angle += Math.toDegrees(shadowLayer.localPos.getAlpha());
			}
			
			savedAlpha = paint.getAlpha();
			currentAlpha = (int) ((float) savedAlpha * shadowLayer.transparency.value * shadowLayer.localTransparency.value * (1.0f - light.diffuse.value));
				
			if (currentAlpha > 0) {
				
				if (currentAlpha != savedAlpha) paint.setAlpha(currentAlpha);
				
				stateCount = canvas.save();
				{
					canvas.clipPath(shadowLayer.drawingArea);
					/*canvas.translate(px, py);
					if (angle != 0) canvas.rotate(angle);*/
					paint.setColor(Color.BLACK);  //setColor killing setAlpha
					paint.setAlpha(currentAlpha);
					bufferFilter = paint.getColorFilter();
					paint.setColorFilter(light.diffuseFilter);
					canvas.drawPath(shadowLayer.shadowArea, paint);
					//sumPath.rewind();
					for (OutputGraphicalShadow shadow : lstActiveShadows) {
						if (shadow.isVisible.value 
								&& shadow.localTransparency.value > 0 && shadow.transparency.value > 0) {
							canvas.drawPath(shadow.drawingArea, paint);
							//sumPath.addPath(shadow.drawingArea);
						}
					}
					//canvas.drawPath(sumPath, paint);					
					
					paint.setColorFilter(bufferFilter);
				}
				canvas.restoreToCount(stateCount);
				
				if (currentAlpha != savedAlpha) paint.setAlpha(savedAlpha);
				handled = true;
				Log.d("Output", "Shadows: draw layer in " + (System.currentTimeMillis() - logTimerMark) + " ms;");
			}
		} 
		return handled;
	}
	
	private boolean transfered;
	
	private long currentTransferTick;

	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		
		transfered = false;
		if (reflection instanceof OutputGraphicalLight) {
			OutputGraphicalLight light = (OutputGraphicalLight) reflection;
			if (data.getContext() == light.position.getContext()) {
				transfered = true;
				for (OutputGraphicalShadowLayer shadowLayer : lstShadowLayers) {
					computeShadowArea(shadowLayer);
				}
			}
		} 
		else if (reflection instanceof OutputGraphicalShadow) {
			long tick = enginesScheduler.currentTick; 
			if (tick != currentTransferTick) {
				currentTransferTick = tick;
				
				if (!lstCurrentActiveShadows.isEmpty()) {
					for (OutputGraphicalShadow shadow : lstCurrentActiveShadows) {
						shadow.isDynamic = false;
					}
					lstStaticShadows.addAll(lstCurrentActiveShadows);
					lstActiveShadows.removeAll(lstCurrentActiveShadows);
					lstCurrentActiveShadows.clear();
					for (OutputGraphicalShadowLayer shadowLayer : lstShadowLayers) {
						computeShadowArea(shadowLayer);
					}
				}
				lstCurrentActiveShadows.addAll(lstActiveShadows);
				//Log.e("shadow", "static: " + lstStaticShadows.size() + "; active: " + lstActiveShadows.size());
			}
			OutputGraphicalShadow shadow = (OutputGraphicalShadow) reflection;
			if (data.getContext() == shadow.position.getContext()) {
				transfered = true;
				lstCurrentActiveShadows.remove(shadow);
				if (!shadow.isDynamic) {
					shadow.isDynamic = true;
					lstStaticShadows.remove(shadow);
					lstActiveShadows.add(shadow);
					for (OutputGraphicalShadowLayer shadowLayer : lstShadowLayers) {
						computeShadowArea(shadowLayer);
					}
				}
			}
		}
		else if (reflection instanceof OutputGraphicalShadowLayer) {
			OutputGraphicalShadowLayer shadowLayer = (OutputGraphicalShadowLayer) reflection;
			
			if (data.getContext() == shadowLayer.position.getContext()) {
				transfered = true;
				if (data instanceof Position3) shadowLayer.position.set((Position3) data);
				else if (data instanceof Point2) {
					if (data instanceof Position2) shadowLayer.position.set((Position2) data);
					else shadowLayer.position.set((Point2) data);
				} 
				
				if (light != null) {
					computeShadowArea(shadowLayer);
					outputDirect.update();
				}
				
			} 
		}
		return transfered;
	}
	
	private void computeShadowArea(OutputGraphicalShadowLayer shadowLayer) {
		shadowLayer.shadowArea.reset();
		//shadowLayer.shadowArea = new Path();
		Log.e("Shadow", "start draw reset area");
		for (OutputGraphicalShadow shadow : lstStaticShadows) {
			if (shadow.isVisible.value 
					&& shadow.localTransparency.value > 0 
					&& shadow.transparency.value > 0)
				
				shadowLayer.shadowArea.addPath(shadow.drawingArea);
		}
	}
	
	private void computeDrawingArea(OutputGraphicalShadowLayer shadowLayer) {
		float sx = (float) shadowLayer.size.x.value / 2, sy = (float) shadowLayer.size.y.value / 2; 
		shadowLayer.drawingArea.moveTo(-sx, -sy);
		shadowLayer.drawingArea.lineTo(-sx, sy);
		shadowLayer.drawingArea.lineTo(sx, sy);
		shadowLayer.drawingArea.lineTo(sx, -sy);
		shadowLayer.drawingArea.close();
		Matrix transMatrix = new Matrix();
		transMatrix.setRotate((float) shadowLayer.position.alpha.value);
		transMatrix.postTranslate((float) shadowLayer.position.x.value, (float) shadowLayer.position.y.value);
		shadowLayer.drawingArea.transform(transMatrix);	
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
