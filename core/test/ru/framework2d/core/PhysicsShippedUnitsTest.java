package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * The same collision scenario at the unit scale the shipped maps use.
 *
 * assets/xml/checkers_tree.xml sets ph_unit = "DisplayWidth/10240" and impulse_limit = "5"
 * (0.1 world units per pixel at 1024 px). The elastic response of
 * ru.framework2d.physics.collisions.InteractionShapes compares the raw mass against an
 * inertia term that carries PHYSICAL_UNIT squared in the denominator, so the *feel* of a
 * hit depends on that attribute. This class locks the shipped-scale behaviour and shows the
 * same map behaving differently with the compiled-in ph_unit = 1.
 */
public class PhysicsShippedUnitsTest {

	private static final String RADIUS = "4";
	private static final String WEIGHT = "0.1";

	private static final String BALLS_TEMPLATE =
			"<map version = \"1.0\" name = \"shipped units\" parallel = \"false\">"
			+ "<direct package = \"ru.framework2d.physics\" engineName = \"IntervalPhysics\""
			+ " ph_unit = \"__PH_UNIT__\" interval_limit = \"70\" impulse_limit = \"5\">"
			+ "<handler subpackage = \"collisions\" engineName = \"HandlerShapesCollisions\">"
			+ "<interaction subpackage = \"circles\" engineName = \"InteractionCircles\"/>"
			+ "<context name = \"impulse\" value = \"directImpulse\"/>"
			+ "</handler>"
			+ "</direct>"
			+ "<entity name = \"ballA\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"" + RADIUS
			+ "\" weight = \"" + WEIGHT + "\"/>"
			+ "</entity>"
			+ "<entity name = \"ballB\" single = \"true\">"
			+ "<circle position:x = \"20\" position:y = \"0\" radius = \"" + RADIUS
			+ "\" weight = \"" + WEIGHT + "\"/>"
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

	private void parse(String phUnitAttribute) {

		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
		scheduler = Engine.enginesScheduler;

		String xml = BALLS_TEMPLATE.replace("__PH_UNIT__", phUnitAttribute);

		try {
			XmlPullParser parser = TestSupport.xmlParser(xml);
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

	/**
	 * Launches ball A straight at ball B and pumps the real scheduler loop until the hit is
	 * resolved, returning (speed of A, speed of B, movement x of B) measured on the tick the
	 * target starts moving. The interval counters are wall-clock based, so the tick count is not
	 * fixed and only the post-collision response is asserted numerically.
	 */
	private double[] launchAndCollide() {

		PhysicalCircle a = circleOf("ballA");
		PhysicalCircle b = circleOf("ballB");

		// |impulse| * impulse_limit = 2 * 5
		a.impulse.set(2, 0);
		a.impulse.commit(null, a.entity);
		assertEquals(10f, a.movement.currentSpeed.value, 0.01f);

		double[] snapshot = null;
		int ticks = 0;

		while (snapshot == null && ticks < 2000) {
			scheduler.overDraw();
			ticks++;

			if (b.movement.currentSpeed.value > 0) snapshot = new double[] {
					a.movement.currentSpeed.value, b.movement.currentSpeed.value, b.movement.getX() };
		}

		assertTrue("ball B was never hit within " + ticks + " tick(s)", snapshot != null);
		assertTrue("ball A must have advanced", a.position.getX() > 0);

		return snapshot;
	}

	@Test
	public void shippedPhUnitAttributeIsResolvedFromTheDisplayWidth() {

		parse("DisplayWidth/10240");

		// the attribute travels through a float Fractional, so it is not exactly 0.1
		assertEquals(0.1, IntervalPhysics.PHYSICAL_UNIT, 0.00000001);
		assertEquals(5f, IntervalPhysics.IMPULSE_LIMIT, 0.0001f);
		assertEquals(70, IntervalPhysics.INTERVAL_LIMIT);
	}

	@Test
	public void impulseLimitScalesACommittedImpulseVector() {

		parse("DisplayWidth/10240");

		PhysicalCircle a = circleOf("ballA");

		a.impulse.set(2, 0);
		a.impulse.commit(null, a.entity);

		assertEquals(10f, a.movement.currentSpeed.value, 0.01f);
		assertEquals(1f, a.movement.getX(), 0.0001f);
	}

	@Test
	public void shippedScaleMakesAnEqualMassHitNearlyExchangeVelocities() {

		parse("DisplayWidth/10240");

		double[] response = launchAndCollide();

		// the tuned game feel: the striker is stopped dead (0.24) and the target takes the hit
		// (9.76 of the incident 10) moving away from the striker
		assertTrue("striker keeps " + response[0] + " of 10, expected a near stop", response[0] < 1.0);
		assertTrue("target takes " + response[1] + " of 10, expected a near mirror", response[1] > 9.0);
		assertTrue("target must move away from the striker, movement x = " + response[2],
				response[2] > 0.9);
	}

	@Test
	public void pins_compiledInPhUnitTurnsTheSameHitIntoBackwardMotionAndExtraEnergy() {

		parse("1");

		double[] response = launchAndCollide();

		// Same map, same shapes, same impulse: only ph_unit differs. With the compiled-in unit the
		// inertia term of InteractionShapes collapses against these small light shapes, so the
		// target is knocked backwards through the striker and BOTH bodies leave the contact faster
		// than the 10 that was launched.
		assertTrue("striker should gain speed (10 -> " + response[0] + ")", response[0] > 10.0);
		assertTrue("target should also gain speed (10 -> " + response[1] + ")", response[1] > 10.0);
		assertTrue("target is knocked backwards, movement x = " + response[2], response[2] < 0.0);
	}

	@Test
	public void shippedScaleKeepsTheIntervalCountersInsideTheMapLimits() {

		parse("DisplayWidth/10240");
		launchAndCollide();

		assertTrue(IntervalPhysics.PROCESSED_INTERVAL >= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL);
		assertTrue(IntervalPhysics.REMAINING_PHYSICAL_INTERVAL <= IntervalPhysics.INTERVAL_LIMIT);
	}
}
