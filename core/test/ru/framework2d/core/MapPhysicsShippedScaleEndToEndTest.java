package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.physics.collisions.blocks.PhysicalBlock;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * End-to-end shipped-unit physics run through a parsed map.
 *
 * The map contains several circles plus a static block wall. The circle is launched into the
 * wall through the real scheduler and collision handlers. This locks the map-level wiring of
 * {@code ph_unit}, {@code impulse_limit} and static block interactions, not just the collision
 * formulas.
 */
public class MapPhysicsShippedScaleEndToEndTest {

	private static final String RADIUS = "4";
	private static final String WEIGHT = "0.1";

	private static final String MAP =
			"<map version = \"1.0\" name = \"shipped units end to end\" parallel = \"false\">"
			+ "<direct package = \"ru.framework2d.physics\" engineName = \"IntervalPhysics\""
			+ " ph_unit = \"DisplayWidth/10240\" interval_limit = \"70\" impulse_limit = \"5\">"
			+ "<handler subpackage = \"collisions\" engineName = \"HandlerShapesCollisions\">"
			+ "<interaction subpackage = \"circles\" engineName = \"InteractionCircles\"/>"
			+ "<interaction subpackage = \"blocks\" engineName = \"InteractionBlocks\"/>"
			+ "<interaction subpackage = \"circleblock\" engineName = \"InteractionCirclesBlocks\"/>"
			+ "<context name = \"impulse\" value = \"directImpulse\"/>"
			+ "</handler>"
			+ "</direct>"
			+ "<entity name = \"striker\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"" + RADIUS
			+ "\" weight = \"" + WEIGHT + "\"/>"
			+ "</entity>"
			+ "<entity name = \"passenger\" single = \"true\">"
			+ "<circle position:x = \"1000\" position:y = \"1000\" radius = \"" + RADIUS
			+ "\" weight = \"" + WEIGHT + "\"/>"
			+ "</entity>"
			+ "<entity name = \"wall\" single = \"true\">"
			+ "<block position:x = \"10.5\" position:y = \"1\" width = \"10\" height = \"10\""
			+ " isStatic = \"true\" weight = \"100\"/>"
			+ "</entity>"
			+ "</map>";

	private EntityStorage storage;
	private EnginesManager enginesManager;
	private EnginesScheduler scheduler;

	@Before
	public void setUp() {

		TestSupport.installFakeGraphics();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	@After
	public void tearDown() {

		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		TestSupport.clearFakeGraphics();
	}

	private void parseMap() {

		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
		scheduler = Engine.enginesScheduler;

		try {
			XmlPullParser parser = TestSupport.xmlParser(MAP);
			new MapParser(storage, enginesManager, null).parseMap(parser);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private PhysicalCircle circleOf(String entityName) {

		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(entityName)) {
				for (PhysicalInterface physical : entity.lstPhysicals) {
					if (physical instanceof PhysicalCircle) return (PhysicalCircle) physical;
				}
			}
		}
		throw new AssertionError("no circle entity named " + entityName);
	}

	private PhysicalBlock blockOf(String entityName) {

		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(entityName)) {
				for (PhysicalInterface physical : entity.lstPhysicals) {
					if (physical instanceof PhysicalBlock) return (PhysicalBlock) physical;
				}
			}
		}
		throw new AssertionError("no block entity named " + entityName);
	}

	@Test
	public void shippedUnitsStaticWallRunThroughParsedMapKeepsTheWallStill() {

		parseMap();

		assertEquals(0.1, IntervalPhysics.PHYSICAL_UNIT, 0.00000001);
		assertEquals(5f, IntervalPhysics.IMPULSE_LIMIT, 0.0001f);
		assertEquals(70, IntervalPhysics.INTERVAL_LIMIT);

		PhysicalCircle striker = circleOf("striker");
		PhysicalBlock wall = blockOf("wall");
		PhysicalCircle passenger = circleOf("passenger");

		double wallInitialX = wall.position.getX();
		double wallInitialY = wall.position.getY();
		double strikerInitialSpeed = striker.movement.currentSpeed.value;

		assertEquals(0f, passenger.movement.currentSpeed.value, 1e-9f);
		assertTrue("the wall must be registered as static", wall.isStatic.value);

		// A tilted approach avoids the known axis-aligned circle/block NaN case.
		striker.impulse.set(1, 0.2);
		striker.impulse.commit(null, striker.entity);

		assertEquals(Math.sqrt(1.04d) * IntervalPhysics.IMPULSE_LIMIT,
				striker.movement.currentSpeed.value, 0.01d);

		int ticks = 0;
		boolean hit = false;

		while (ticks < 2000 && !hit) {
			scheduler.overDraw();
			ticks++;

			hit = striker.movement.currentSpeed.value != strikerInitialSpeed
					|| Math.abs(striker.movement.alphaRotation.value) > 0.001d
					|| Math.abs(striker.position.getX() - wall.position.getX()) < 6d;
		}

		assertTrue("the striker never reached the static wall within " + ticks + " tick(s)", hit);

		assertEquals("static wall must not gain linear motion",
				0f, wall.movement.currentSpeed.value, 1e-9f);
		assertEquals("static wall must not gain rotation",
				0f, wall.movement.alphaRotation.value, 1e-9f);
		assertEquals("static wall x must not move", wallInitialX, wall.position.getX(), 1e-9);
		assertEquals("static wall y must not move", wallInitialY, wall.position.getY(), 1e-9);

		assertEquals("passenger must remain untouched",
				0f, passenger.movement.currentSpeed.value, 1e-9f);
		assertEquals(1000d, passenger.position.getX(), 1e-9);
		assertEquals(1000d, passenger.position.getY(), 1e-9);

		assertNotEquals("the striker response must change after the wall contact",
				strikerInitialSpeed, striker.movement.currentSpeed.value, 0.01d);
		assertTrue("the wall contact must make the striker move away or spin",
				striker.movement.getX() < 0.9d
						|| Math.abs(striker.movement.alphaRotation.value) > 0.5d);
	}
}
