package ru.framework2d.graphics.draw.shadows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
import ru.framework2d.graphics.render.RenderTransform;


import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Color;
import ru.framework2d.compat.ColorFilter;
import ru.framework2d.compat.Paint;
import ru.framework2d.compat.Path;

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
	
	private final RenderTransform transform = new RenderTransform();
	
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
				
				savedAlpha = paint.getAlpha();
				currentAlpha = (int) ((float) savedAlpha * (1.0f - light.diffuse.value));
					
				if (currentAlpha > 0) {
					
					if (currentAlpha != savedAlpha) paint.setAlpha(currentAlpha);
					
					//The drawing area is built around the light in world coordinates, while the canvas is
					//already moved to this graphical's position, so the shape must be pulled back by the
					//accumulated offset of the parent chain. Without it the shadow is drawn twice as far
					//from the light as its caster is.
					stateCount = 0;
					if (transform.compute(shadow)) {
						stateCount = canvas.save();
						canvas.translate(-transform.x, -transform.y);
					}

					paint.setColor(Color.BLACK);  //setColor killing setAlpha
						paint.setAlpha(currentAlpha);
						bufferFilter = paint.getColorFilter();
						paint.setColorFilter(light.diffuseFilter);
						canvas.drawPath(shadow.drawingArea, paint);
						paint.setColorFilter(bufferFilter);

					if (stateCount != 0) {
						canvas.restoreToCount(stateCount);
						stateCount = 0;
					}
					
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

	private static final int HULL_SEGMENTS = 24;
	private static final double GEOMETRY_EPSILON = 1.0e-6;
	
	void computeDrawingArea(OutputGraphicalShadow shadow) {
		
		if (shadow.drawingArea == null || light == null) return;
		shadow.drawingArea.rewind();
		
		if (shadow.geometry instanceof GeometryVerticalCylinder) {
			computeCylinderDrawingArea(shadow, (GeometryVerticalCylinder) shadow.geometry);
		} 
		else if (shadow.geometry instanceof GeometryBlock) {
			computeBlockDrawingArea(shadow, (GeometryBlock) shadow.geometry);
		}
	}

	private void computeCylinderDrawingArea(OutputGraphicalShadow shadow, GeometryVerticalCylinder geometry) {
		cylinder = geometry;
		topPosition.set(shadow.position.x.value, shadow.position.y.value,
				shadow.position.z.value + geometry.height.value);
		toTop.set(topPosition, light.position);
		distanceToTop = toTop.length2();

		double radius = Math.abs(geometry.radius.value);
		double height = Math.abs(geometry.height.value);
		double lightTopZ = Math.abs(toTop.z.value);
		if (lightTopZ < GEOMETRY_EPSILON) return;

		fullHeight = lightTopZ + height;
		shadowRatio = fullHeight / lightTopZ;

		toShadow.set(toTop);
		toShadow.selfMulti(shadowRatio);
		px = (float) light.position.x.value;
		py = (float) light.position.y.value;
		cylinderRadius = (float) radius;
		shadowRadius = (float) (radius * shadowRatio);

		ArrayList<double[]> points = new ArrayList<double[]>();
		addCirclePoints(points, (double) shadow.position.x.value, (double) shadow.position.y.value, radius, HULL_SEGMENTS);
		addCirclePoints(points, (double) light.position.x.value + toShadow.x.value,
				(double) light.position.y.value + toShadow.y.value, radius * shadowRatio, HULL_SEGMENTS);
		addHullToPath(shadow.drawingArea, points);
	}

	private void computeBlockDrawingArea(OutputGraphicalShadow shadow, GeometryBlock geometry) {
		block = geometry;
		topPosition.set(shadow.position.x.value, shadow.position.y.value,
				shadow.position.z.value + geometry.height.value);
		toTop.set(topPosition, light.position);

		double height = Math.abs(geometry.height.value);
		double lightTopZ = Math.abs(toTop.z.value);
		if (lightTopZ < GEOMETRY_EPSILON) return;

		fullHeight = lightTopZ + height;
		shadowRatio = fullHeight / lightTopZ;

		toShadow.set(toTop);
		toShadow.selfMulti(shadowRatio);

		sizeX = Math.abs(geometry.width.value * 0.5f);
		sizeY = Math.abs(geometry.depth.value * 0.5f);
		double angle = shadow.position.getAlpha();

		ArrayList<double[]> points = new ArrayList<double[]>();
		vectors[0].set(-sizeX, -sizeY);
		vectors[1].set(-sizeX, sizeY);
		vectors[2].set(sizeX, sizeY);
		vectors[3].set(sizeX, -sizeY);

		for (int i = 0; i < vectors.length; i++) {
			vectors[i].rotate(angle);
			addPoint(points, (double) shadow.position.x.value + vectors[i].x.value,
					(double) shadow.position.y.value + vectors[i].y.value);
			addPoint(points, (double) light.position.x.value + (toTop.x.value + vectors[i].x.value) * shadowRatio,
					(double) light.position.y.value + (toTop.y.value + vectors[i].y.value) * shadowRatio);
		}

		addHullToPath(shadow.drawingArea, points);
	}

	private void addCirclePoints(ArrayList<double[]> points, double x, double y, double radius, int segments) {
		if (radius < GEOMETRY_EPSILON) {
			addPoint(points, x, y);
			return;
		}
		for (int i = 0; i < segments; i++) {
			double pointAngle = 2.0d * Math.PI * (double) i / (double) segments;
			addPoint(points, x + Math.cos(pointAngle) * radius, y + Math.sin(pointAngle) * radius);
		}
	}

	private void addPoint(ArrayList<double[]> points, double x, double y) {
		for (int i = 0; i < points.size(); i++) {
			double[] point = points.get(i);
			if (Math.abs(point[0] - x) < GEOMETRY_EPSILON && Math.abs(point[1] - y) < GEOMETRY_EPSILON) return;
		}
		points.add(new double[] { x, y });
	}

	private void addHullToPath(Path path, ArrayList<double[]> points) {
		ArrayList<double[]> hull = convexHull(points);
		if (hull.size() < 3) return;

		for (int i = 0; i < hull.size(); i++) {
			double[] point = hull.get(i);
			if (i == 0) {
				path.moveTo((float) point[0], (float) point[1]);
			} 
			else {
				path.lineTo((float) point[0], (float) point[1]);
			}
		}
		path.close();
	}

	private ArrayList<double[]> convexHull(ArrayList<double[]> points) {
		if (points.size() < 2) return new ArrayList<double[]>(points);

		Collections.sort(points, new Comparator<double[]>() {
			@Override
			public int compare(double[] a, double[] b) {
				int xCompare = Double.compare(a[0], b[0]);
				return xCompare != 0 ? xCompare : Double.compare(a[1], b[1]);
			}
		});

		ArrayList<double[]> unique = new ArrayList<double[]>();
		for (int i = 0; i < points.size(); i++) {
			double[] point = points.get(i);
			if (unique.isEmpty()
					|| Math.abs(unique.get(unique.size() - 1)[0] - point[0]) >= GEOMETRY_EPSILON
					|| Math.abs(unique.get(unique.size() - 1)[1] - point[1]) >= GEOMETRY_EPSILON) {
				unique.add(point);
			}
		}

		if (unique.size() < 3) return unique;

		ArrayList<double[]> lower = new ArrayList<double[]>();
		for (int i = 0; i < unique.size(); i++) {
			double[] point = unique.get(i);
			while (lower.size() >= 2
					&& cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), point) <= 0.0d) {
				lower.remove(lower.size() - 1);
			}
			lower.add(point);
		}

		ArrayList<double[]> upper = new ArrayList<double[]>();
		for (int i = unique.size() - 1; i >= 0; i--) {
			double[] point = unique.get(i);
			while (upper.size() >= 2
					&& cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), point) <= 0.0d) {
				upper.remove(upper.size() - 1);
			}
			upper.add(point);
		}

		lower.remove(lower.size() - 1);
		upper.remove(upper.size() - 1);
		lower.addAll(upper);
		return lower;
	}

	private double cross(double[] origin, double[] a, double[] b) {
		return (a[0] - origin[0]) * (b[1] - origin[1]) - (a[1] - origin[1]) * (b[0] - origin[0]);
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
