package ru.framework2d.graphics.draw.shadows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.compat.Path;
import ru.framework2d.core.Entity;
import ru.framework2d.core.agents.AgentTestScope;
import ru.framework2d.core.agents.EngineLocks;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.data.GeometryBlock;
import ru.framework2d.data.GeometryVerticalCylinder;
import ru.framework2d.testsupport.TestSupport;

public class SubShadowsSilhouetteTest {

	private static final double LIGHT_X = 120.0d;
	private static final double LIGHT_Y = 120.0d;
	private static final double LIGHT_Z = 200.0d;

	private SubShadows shadows;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		shadows = new SubShadows();
		addLight();
	}

	@After
	public void tearDown() {
		shadows = null;
	}

	@Test
	public void cylinderShadowIsAConvexProjectedSilhouette() {
		try (AgentTestScope scope = EngineLocks.beginTestScope(
				"compute legacy cylinder shadow silhouette", EngineResource.GRAPHICS)) {

			GeometryVerticalCylinder cylinder = new GeometryVerticalCylinder();
			cylinder.radius.value = 40.0f;
			cylinder.height.value = 30.0f;

			OutputGraphicalShadow shadow = addShadow(300.0d, 300.0d, cylinder);
			Path path = shadow.drawingArea;

			assertTrue(path.getNumPoints() > 3);
			assertConvexClosedPolygon(path);

			Projection projection = projection(path, 300.0d, 300.0d);
			double predictedFar = shadowRatio(30.0f) * (projection.distance + cylinder.radius.value);
			double predictedNear = projection.distance - cylinder.radius.value;

			assertEquals(predictedFar, projection.maximum, 1.0d);
			assertTrue(projection.minimum <= predictedNear + 1.0d);
		}
	}

	@Test
	public void blockShadowCoversBaseAndProjectedFootprint() {
		try (AgentTestScope scope = EngineLocks.beginTestScope(
				"compute legacy block shadow silhouette", EngineResource.GRAPHICS)) {

			GeometryBlock block = new GeometryBlock();
			block.width.value = 60.0f;
			block.depth.value = 100.0f;
			block.height.value = 10.0f;

			OutputGraphicalShadow shadow = addShadow(480.0d, 680.0d, block);
			Path path = shadow.drawingArea;

			assertTrue(path.getNumPoints() > 3);
			assertConvexClosedPolygon(path);

			Projection projection = projection(path, 480.0d, 680.0d);
			double halfExtent = halfBlockExtent(projection, block);
			double predictedFar = shadowRatio(block.height.value) * (projection.distance + halfExtent);
			double predictedNear = projection.distance - halfExtent;

			assertEquals(predictedFar, projection.maximum, 1.0d);
			assertTrue(projection.minimum <= predictedNear + 1.0d);
		}
	}

	@Test
	public void tallerCylinderProjectsFurtherThanShorterCylinder() {
		try (AgentTestScope scope = EngineLocks.beginTestScope(
				"compare legacy cylinder shadow lengths", EngineResource.GRAPHICS)) {

			GeometryVerticalCylinder shortCylinder = new GeometryVerticalCylinder();
			shortCylinder.radius.value = 40.0f;
			shortCylinder.height.value = 10.0f;

			GeometryVerticalCylinder tallCylinder = new GeometryVerticalCylinder();
			tallCylinder.radius.value = 40.0f;
			tallCylinder.height.value = 90.0f;

			OutputGraphicalShadow shortShadow = addShadow(500.0d, 500.0d, shortCylinder);
			OutputGraphicalShadow tallShadow = addShadow(500.0d, 500.0d, tallCylinder);

			double shortFar = projection(shortShadow.drawingArea, 500.0d, 500.0d).maximum;
			double tallFar = projection(tallShadow.drawingArea, 500.0d, 500.0d).maximum;

			assertTrue(tallFar > shortFar + 50.0d);
		}
	}

	private void addLight() {
		OutputGraphicalLight light = new OutputGraphicalLight();
		light.position.set(LIGHT_X, LIGHT_Y, LIGHT_Z);
		shadows.connectComponent(light);
	}

	private OutputGraphicalShadow addShadow(double x, double y, ru.framework2d.data.Geometry geometry) {
		OutputGraphicalShadow shadow = new OutputGraphicalShadow();
		shadow.entity = new Entity("caster");
		shadow.position.set(x, y, 0.0d);
		shadow.geometry = geometry;
		shadows.connectComponent(shadow);
		return shadow;
	}

	private double shadowRatio(float height) {
		return LIGHT_Z / (LIGHT_Z - (double) height);
	}

	private double halfBlockExtent(Projection projection, GeometryBlock block) {
		double halfWidth = Math.abs(block.width.value * 0.5d);
		double halfDepth = Math.abs(block.depth.value * 0.5d);
		return halfWidth * Math.abs(projection.unitX) + halfDepth * Math.abs(projection.unitY);
	}

	private Projection projection(Path path, double x, double y) {
		double dx = x - LIGHT_X;
		double dy = y - LIGHT_Y;
		double distance = Math.sqrt(dx * dx + dy * dy);
		double unitX = dx / distance;
		double unitY = dy / distance;

		double minimum = Double.MAX_VALUE;
		double maximum = -Double.MAX_VALUE;
		for (int i = 0; i < path.getNumPoints(); i++) {
			double projection = (path.pointX(i) - LIGHT_X) * unitX + (path.pointY(i) - LIGHT_Y) * unitY;
			minimum = Math.min(minimum, projection);
			maximum = Math.max(maximum, projection);
		}
		return new Projection(distance, unitX, unitY, minimum, maximum);
	}

	private void assertConvexClosedPolygon(Path path) {
		int points = path.getNumPoints();
		boolean closes = Math.abs(path.pointX(0) - path.pointX(points - 1)) < 0.0001d
				&& Math.abs(path.pointY(0) - path.pointY(points - 1)) < 0.0001d;
		int vertices = closes ? points - 1 : points;
		assertTrue(vertices >= 3);

		int sign = 0;
		for (int i = 0; i < vertices; i++) {
			double ax = path.pointX(i);
			double ay = path.pointY(i);
			double bx = path.pointX((i + 1) % vertices);
			double by = path.pointY((i + 1) % vertices);
			double cx = path.pointX((i + 2) % vertices);
			double cy = path.pointY((i + 2) % vertices);
			double cross = (bx - ax) * (cy - by) - (by - ay) * (cx - bx);
			if (Math.abs(cross) > 0.0001d) {
				if (sign == 0) {
					sign = cross > 0 ? 1 : -1;
				} else {
					assertTrue((cross > 0 ? 1 : -1) == sign);
				}
			}
		}
	}

	private static final class Projection {
		private final double distance;
		private final double unitX;
		private final double unitY;
		private final double minimum;
		private final double maximum;

		private Projection(double distance, double unitX, double unitY, double minimum, double maximum) {
			this.distance = distance;
			this.unitX = unitX;
			this.unitY = unitY;
			this.minimum = minimum;
			this.maximum = maximum;
		}
	}
}
