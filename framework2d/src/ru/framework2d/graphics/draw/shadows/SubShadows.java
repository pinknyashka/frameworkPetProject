package ru.framework2d.graphics.draw.shadows;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.EntityStorage;

import ru.framework2d.data.DataInterface;
import ru.framework2d.data.GeometryBlock;
import ru.framework2d.data.GeometryVerticalCylinder;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Point3;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import ru.framework2d.data.Vector3;
import ru.framework2d.graphics.GraphicalPeriphery;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public class SubShadows extends ComponentsHandler {
	
	private Paint paint;
	private GraphicalPeriphery outputDirect;

	public SubShadows() {
		super(1, (Class <Component>) OutputGraphicalShadow.class.asSubclass(Component.class), 
				(Class <Component>) OutputGraphicalLight.class.asSubclass(Component.class), 
				(Class <Component>) OutputGraphicalShadowLayer.class.asSubclass(Component.class));
		
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
	private ArrayList <OutputGraphicalShadowLayer> lstShadowLayers = new ArrayList <OutputGraphicalShadowLayer>();
	
	private OutputGraphicalLight light;
	private OutputGraphicalShadowLayer layer;
	
	
	
	boolean drawThrowLayers = false;
	
	@Override
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof OutputGraphicalShadow) {
			
			OutputGraphicalShadow newShadow = (OutputGraphicalShadow) component;

			if (!lstShadows.contains(newShadow) && !newShadow.entity.isPrototype) {
				
				newShadow.drawingArea = new Path();

				lstShadows.add(newShadow);
				if (light != null) {
					computeDrawingArea(newShadow);
				}
			}
		} 
		else if (component instanceof OutputGraphicalShadowLayer) {
			
			OutputGraphicalShadowLayer newShadowLayer = (OutputGraphicalShadowLayer) component;
			
			if (!lstShadowLayers.contains(newShadowLayer)) {
				lstShadowLayers.add(newShadowLayer);
				drawThrowLayers = true;
				layer = newShadowLayer;
			}
		} 
		else if (component instanceof OutputGraphicalLight) {
			
			light = (OutputGraphicalLight) component;
			for (OutputGraphicalShadow shadow : lstShadows) {
				computeDrawingArea(shadow);
			}
		}
		return component;
	}
	
	
	
	private ColorFilter bufferFilter;
	private OutputGraphicalShadow shadow;
	
	@Override
	public boolean toHandleComponent(Component reflection) {

		boolean handled = false;
		if (!drawThrowLayers) {
			if (currentTick != enginesScheduler.currentTick) {
				canvas = outputDirect.getCanvas();
				currentTick = enginesScheduler.currentTick;
			}
			 
			if (canvas != null && light != null) {
				
				shadow = (OutputGraphicalShadow) reflection;
				
				//px = shadow.position.getXf(); 		py = shadow.position.getYf();
				//angle = 0;
				/*if (shadow.isRotatable.value) {
					angle += (float) Math.toDegrees(shadow.position.getAlpha());
				}*/
				//sx = shadow.size.x.value / 2;		sy = shadow.size.y.value / 2;
				
				//px += shadow.localPos.x.value;	py += shadow.localPos.y.value;
				/*if (shadow.isLocalRotatable.value) {
					angle += Math.toDegrees(shadow.localPos.getAlpha());
				}*/
				
				savedAlpha = paint.getAlpha();
					//currentAlpha = (int) ((float) savedAlpha * shadow.transparency.value * shadow.localTransparency.value * (1.0f - light.diffuse.value));
				currentAlpha = (int) ((float) savedAlpha * (1.0f - light.diffuse.value));
					
				if (currentAlpha > 0) {
					
					if (currentAlpha != savedAlpha) paint.setAlpha(currentAlpha);
					
//					stateCount = canvas.save();
//					{
						//canvas.translate(px, py);
						paint.setColor(Color.BLACK);  //setColor killing setAlpha
						paint.setAlpha(currentAlpha);
						bufferFilter = paint.getColorFilter();
						paint.setColorFilter(light.diffuseFilter);
						canvas.drawPath(shadow.drawingArea, paint);
						paint.setColorFilter(bufferFilter);
//					}
//					canvas.restoreToCount(stateCount);
					
					if (currentAlpha != savedAlpha) paint.setAlpha(savedAlpha);
					handled = true;
				}
			} 
		}
		return handled;
	}
	
	
	
	private boolean transfered;
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		
		transfered = false;
		
		if (reflection instanceof OutputGraphicalLight) {
			OutputGraphicalLight light = (OutputGraphicalLight) reflection;
			if (data.getContext() == light.position.getContext()) {
				transfered = true;
				for (OutputGraphicalShadow shadow : lstShadows) {
					computeDrawingArea(shadow);
				}
			}
		}
		else if (reflection instanceof OutputGraphicalShadow) {
			OutputGraphicalShadow shadow = (OutputGraphicalShadow) reflection;
			
			if (data.getContext() == shadow.position.getContext()) {
				transfered = true;
				if (data instanceof Position3) shadow.position.set((Position3) data);
				else if (data instanceof Point2) {
					if (data instanceof Position2) shadow.position.set((Position2) data);
					else shadow.position.set((Point2) data);
				} 
				
				if (light != null) {
					computeDrawingArea(shadow);
					//shadow.position.localCommit(null, shadow, shadow.position);
					//outputDirect.setWorkState(true);
				}
				
			} 
		}
		return transfered;
	}
	
	
	
	private Vector3 toTop = new Vector3();
	private Vector3 toShadow = new Vector3();
	private Point3 topPosition = new Point3();
	
	private double distanceToTop, fullHeight, shadowRatio, angle, distanceToShadow;
	private float px, py, cylinderRadius, shadowRadius;
	
	private float sizeX, sizeY;
	private GeometryVerticalCylinder cylinder;
	private GeometryBlock block;
	
	private double[] angles = new double[4];
	private double[] projections = new double[4];
	private Vector2[] vectors = {new Vector2(), new Vector2(), new Vector2(), new Vector2()};
	
	private void computeDrawingArea(OutputGraphicalShadow shadow) {
		
		if (shadow.geometry instanceof GeometryVerticalCylinder) {
			cylinder = (GeometryVerticalCylinder) shadow.geometry;
			topPosition.set(shadow.position.x.value, shadow.position.y.value, shadow.position.z.value + cylinder.height.value);
			toTop.set(topPosition, light.position);
			distanceToTop = toTop.length2();
			fullHeight = Math.abs(toTop.z.value) + cylinder.height.value;
			shadowRatio = fullHeight / Math.abs(toTop.z.value);
			toShadow.set(toTop);
			toShadow.selfMulti(shadowRatio);
			px = (float) light.position.x.value; py = (float) light.position.y.value;
			cylinderRadius = cylinder.radius.value;
			shadowRadius = cylinderRadius * (float) shadowRatio;
			
			shadow.drawingArea.rewind();
			shadow.drawingArea.addCircle(px + (float) toShadow.x.value, py + (float) toShadow.y.value, shadowRadius, Path.Direction.CCW);
			if (distanceToTop > cylinderRadius) {
				angle = Math.asin(cylinderRadius / distanceToTop); 
				((Vector2) toTop).rotate(angle);
				toTop.restore();
				toTop.selfMulti(Math.sqrt(distanceToTop * distanceToTop - cylinderRadius * cylinderRadius));
				shadow.drawingArea.moveTo(px + (float) toTop.x.value, py + (float) toTop.y.value);
				toTop.selfMulti(shadowRatio);
				shadow.drawingArea.lineTo(px + (float) toTop.x.value, py + (float) toTop.y.value);
				
				((Vector2) toShadow).rotate(-angle);
				distanceToShadow = ((Vector2) toShadow).restore();
				toShadow.selfMulti(Math.sqrt(distanceToShadow * distanceToShadow - shadowRadius * shadowRadius));
				shadow.drawingArea.lineTo(px + (float) toShadow.x.value, py + (float) toShadow.y.value);
				toShadow.selfMulti(1.0d / shadowRatio);
				shadow.drawingArea.lineTo(px + (float) toShadow.x.value, py + (float) toShadow.y.value);
				shadow.drawingArea.close();
			}
			
		} 
		else if (shadow.geometry instanceof GeometryBlock) {
			block = (GeometryBlock) shadow.geometry;
			topPosition.set(shadow.position.x.value, shadow.position.y.value, shadow.position.z.value + block.height.value);
			toTop.set(topPosition, light.position);
			fullHeight = Math.abs(toTop.z.value) + block.height.value;
			shadowRatio = fullHeight / Math.abs(toTop.z.value);
			toShadow.set(toTop);
			toShadow.selfMulti(shadowRatio);
			sizeX = (block.width.value * 0.5f); sizeY = (block.depth.value * 0.5f);
			vectors[0].set(-sizeX, -sizeY);
			vectors[1].set(-sizeX, sizeY);
			vectors[2].set(sizeX, sizeY);
			vectors[3].set(sizeX, -sizeY);
			boolean isLightInBlock = false;
			double angle = shadow.position.getAlpha();
			double currentLength = 0, shortestLength = 0, longestLength = 0;
			int shortestNo = 0, longestNo = 0; 
			for (int i = 0; i < 4; i++) { 
				vectors[i].rotate(angle);
				vectors[i].addict(toTop);
				projections[i] = vectors[i].myProectionOnIdentityVector(toTop);
				currentLength = vectors[i].length2();
				if (i == 0) {
					shortestLength = currentLength;
					longestLength = currentLength;
				} 
				else {
					if (projections[i] * projections[0] <= 0) {
						isLightInBlock = true;
					}
					if (currentLength < shortestLength) {
						shortestLength = currentLength; 
						shortestNo = i;
					} 
					else if (currentLength > longestLength) {
						longestLength = currentLength;
						longestNo = i;
					}
				}
			}
			
			shadow.drawingArea.rewind();
			
			if (!isLightInBlock) {
				double biggestAngle = 0, lowestAngle = 0;
				int biggestAngleNo = 0, lowestAngleNo = 0;
				for (int i = 0; i < 4; i++) {
					angles[i] = vectors[i].computeAngle(toTop);
					if (angles[i] > biggestAngle) {
						biggestAngle = angles[i];
						biggestAngleNo = i;
					} 
					if (angles[i] < lowestAngle) {
						lowestAngle = angles[i];
						lowestAngleNo = i;
					}
				}
				if (biggestAngleNo > lowestAngleNo) {
					int tmp = biggestAngleNo;
					biggestAngleNo = lowestAngleNo;
					lowestAngleNo = tmp;
				}
				
				if ((lowestAngleNo - biggestAngleNo) % 2 == 1) { //if closest border shadow
					shadow.drawingArea.moveTo((float) (light.position.x.value + vectors[biggestAngleNo].x.value),
							(float) (light.position.y.value + vectors[biggestAngleNo].y.value));
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
							(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					for (int i = 0; i < 4; i++) {
						vectors[i].selfMulti(shadowRatio);
					}
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
							(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					int direct = 1;
					int nextVertexNo = (lowestAngleNo + direct) % 4;
					if (nextVertexNo == biggestAngleNo) {
						direct = -1;
						nextVertexNo = (lowestAngleNo + direct) % 4;
					}
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[nextVertexNo].x.value),
							(float) (light.position.y.value + vectors[nextVertexNo].y.value));
					nextVertexNo = (nextVertexNo + direct) % 4;
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[nextVertexNo].x.value),
							(float) (light.position.y.value + vectors[nextVertexNo].y.value));
					nextVertexNo = (nextVertexNo + direct) % 4;
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[nextVertexNo].x.value),
							(float) (light.position.y.value + vectors[nextVertexNo].y.value));
					shadow.drawingArea.close();
					
				} 
				else { //if opposite borders shadow
					
					shadow.drawingArea.moveTo((float) (light.position.x.value + vectors[biggestAngleNo].x.value),
							(float) (light.position.y.value + vectors[biggestAngleNo].y.value));
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[shortestNo].x.value),
							(float) (light.position.y.value + vectors[shortestNo].y.value));
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
											(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					for (int i = 0; i < 4; i++) {
						vectors[i].selfMulti(shadowRatio);
					}
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
							(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[longestNo].x.value),
											(float) (light.position.y.value + vectors[longestNo].y.value));
					shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[biggestAngleNo].x.value),
											(float) (light.position.y.value + vectors[biggestAngleNo].y.value));
					
					shadow.drawingArea.close();
					
				}
			} 
			else {
				for (int i = 3; i >= 0; i--) {
					if (i == 3) {
						shadow.drawingArea.moveTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					} 
					else {
						shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					}
				}
				shadow.drawingArea.close();
				for (int i = 0; i < 4; i++) {
					vectors[i].selfMulti(shadowRatio);
					if (i == 0) {
						shadow.drawingArea.moveTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					} 
					else {
						shadow.drawingArea.lineTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					}
				}
				shadow.drawingArea.close();
			}
	
	
		}
		/*toHead.set(light.position, shadow.position);
		toHead.rotate(Math.PI / 4);
		shadow.localPos.setAlpha((float)(toHead.computeAngle()));*/
	}
	
	
	
	private void computeDrawingLayer(OutputGraphicalShadow shadow) {
		
		if (shadow.geometry instanceof GeometryVerticalCylinder) {
			cylinder = (GeometryVerticalCylinder) shadow.geometry;
			topPosition.set(shadow.position.x.value, shadow.position.y.value, shadow.position.z.value + cylinder.height.value);
			toTop.set(topPosition, light.position);
			distanceToTop = toTop.length2();
			fullHeight = Math.abs(toTop.z.value) + cylinder.height.value;
			shadowRatio = fullHeight / Math.abs(toTop.z.value);
			toShadow.set(toTop);
			toShadow.selfMulti(shadowRatio);
			px = (float) light.position.x.value; py = (float) light.position.y.value;
			cylinderRadius = cylinder.radius.value;
			shadowRadius = cylinderRadius * (float) shadowRatio;
			
			//layer.shadowArea.rewind();
			layer.shadowArea.addCircle(px + (float) toShadow.x.value, py + (float) toShadow.y.value, shadowRadius, Path.Direction.CCW);
			if (distanceToTop > cylinderRadius) {
				angle = Math.asin(cylinderRadius / distanceToTop); 
				((Vector2) toTop).rotate(angle);
				toTop.restore();
				toTop.selfMulti(Math.sqrt(distanceToTop * distanceToTop - cylinderRadius * cylinderRadius));
				layer.shadowArea.moveTo(px + (float) toTop.x.value, py + (float) toTop.y.value);
				toTop.selfMulti(shadowRatio);
				layer.shadowArea.lineTo(px + (float) toTop.x.value, py + (float) toTop.y.value);
				
				((Vector2) toShadow).rotate(-angle);
				distanceToShadow = ((Vector2) toShadow).restore();
				toShadow.selfMulti(Math.sqrt(distanceToShadow * distanceToShadow - shadowRadius * shadowRadius));
				layer.shadowArea.lineTo(px + (float) toShadow.x.value, py + (float) toShadow.y.value);
				toShadow.selfMulti(1.0d / shadowRatio);
				layer.shadowArea.lineTo(px + (float) toShadow.x.value, py + (float) toShadow.y.value);
				layer.shadowArea.close();
			}
			
		} 
		else if (shadow.geometry instanceof GeometryBlock) {
			block = (GeometryBlock) shadow.geometry;
			topPosition.set(shadow.position.x.value, shadow.position.y.value, shadow.position.z.value + block.height.value);
			toTop.set(topPosition, light.position);
			fullHeight = Math.abs(toTop.z.value) + block.height.value;
			shadowRatio = fullHeight / Math.abs(toTop.z.value);
			toShadow.set(toTop);
			toShadow.selfMulti(shadowRatio);
			sizeX = (block.width.value * 0.5f); sizeY = (block.depth.value * 0.5f);
			vectors[0].set(-sizeX, -sizeY);
			vectors[1].set(-sizeX, sizeY);
			vectors[2].set(sizeX, sizeY);
			vectors[3].set(sizeX, -sizeY);
			boolean isLightInBlock = false;
			double angle = shadow.position.getAlpha();
			double currentLength = 0, shortestLength = 0, longestLength = 0;
			int shortestNo = 0, longestNo = 0; 
			for (int i = 0; i < 4; i++) { 
				vectors[i].rotate(angle);
				vectors[i].addict(toTop);
				projections[i] = vectors[i].myProectionOnIdentityVector(toTop);
				currentLength = vectors[i].length2();
				if (i == 0) {
					shortestLength = currentLength;
					longestLength = currentLength;
				} 
				else {
					if (projections[i] * projections[0] <= 0) {
						isLightInBlock = true;
					}
					if (currentLength < shortestLength) {
						shortestLength = currentLength; 
						shortestNo = i;
					} 
					else if (currentLength > longestLength) {
						longestLength = currentLength;
						longestNo = i;
					}
				}
			}
			
			//shadow.drawingArea.rewind();
			
			if (!isLightInBlock) {
				double biggestAngle = 0, lowestAngle = 0;
				int biggestAngleNo = 0, lowestAngleNo = 0;
				for (int i = 0; i < 4; i++) {
					angles[i] = vectors[i].computeAngle(toTop);
					if (angles[i] > biggestAngle) {
						biggestAngle = angles[i];
						biggestAngleNo = i;
					} 
					if (angles[i] < lowestAngle) {
						lowestAngle = angles[i];
						lowestAngleNo = i;
					}
				}
				if (biggestAngleNo > lowestAngleNo) {
					int tmp = biggestAngleNo;
					biggestAngleNo = lowestAngleNo;
					lowestAngleNo = tmp;
				}
				
				if ((lowestAngleNo - biggestAngleNo) % 2 == 1) { //if closest border shadow
					layer.shadowArea.moveTo((float) (light.position.x.value + vectors[biggestAngleNo].x.value),
							(float) (light.position.y.value + vectors[biggestAngleNo].y.value));
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
							(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					for (int i = 0; i < 4; i++) {
						vectors[i].selfMulti(shadowRatio);
					}
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
							(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					int direct = 1;
					int nextVertexNo = (lowestAngleNo + direct) % 4;
					if (nextVertexNo == biggestAngleNo) {
						direct = -1;
						nextVertexNo = (lowestAngleNo + direct) % 4;
					}
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[nextVertexNo].x.value),
							(float) (light.position.y.value + vectors[nextVertexNo].y.value));
					nextVertexNo = (nextVertexNo + direct) % 4;
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[nextVertexNo].x.value),
							(float) (light.position.y.value + vectors[nextVertexNo].y.value));
					nextVertexNo = (nextVertexNo + direct) % 4;
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[nextVertexNo].x.value),
							(float) (light.position.y.value + vectors[nextVertexNo].y.value));
					layer.shadowArea.close();
					
				} 
				else { //if opposite borders shadow
					
					layer.shadowArea.moveTo((float) (light.position.x.value + vectors[biggestAngleNo].x.value),
							(float) (light.position.y.value + vectors[biggestAngleNo].y.value));
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[shortestNo].x.value),
							(float) (light.position.y.value + vectors[shortestNo].y.value));
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
											(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					for (int i = 0; i < 4; i++) {
						vectors[i].selfMulti(shadowRatio);
					}
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[lowestAngleNo].x.value),
							(float) (light.position.y.value + vectors[lowestAngleNo].y.value));
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[longestNo].x.value),
											(float) (light.position.y.value + vectors[longestNo].y.value));
					layer.shadowArea.lineTo((float) (light.position.x.value + vectors[biggestAngleNo].x.value),
											(float) (light.position.y.value + vectors[biggestAngleNo].y.value));
					
					layer.shadowArea.close();
					
				}
			} 
			else {
				for (int i = 3; i >= 0; i--) {
					if (i == 3) {
						layer.shadowArea.moveTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					} else {
						layer.shadowArea.lineTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					}
				}
				layer.shadowArea.close();
				for (int i = 0; i < 4; i++) {
					vectors[i].selfMulti(shadowRatio);
					if (i == 0) {
						layer.shadowArea.moveTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					} 
					else {
						layer.shadowArea.lineTo((float) (light.position.x.value + vectors[i].x.value),
								(float) (light.position.y.value + vectors[i].y.value));
					}
				}
				layer.shadowArea.close();
			}
	
	
		}
		/*toHead.set(light.position, shadow.position);
		toHead.rotate(Math.PI / 4);
		shadow.localPos.setAlpha((float)(toHead.computeAngle()));*/
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
