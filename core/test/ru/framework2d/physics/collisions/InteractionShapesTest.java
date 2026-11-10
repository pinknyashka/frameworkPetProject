package ru.framework2d.physics.collisions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Property;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

public class InteractionShapesTest {

	private static final double EPS = 1e-9;

	private InteractionShapes shapes;

	@Before
	public void setUp() {
		TestSupport.resetIntervalPhysics();
		shapes = new InteractionShapes();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	private PhysicalCircle circle(double x, double y, double radius) {
		PhysicalCircle c = new PhysicalCircle();
		c.position.set(x, y, 0);
		c.setPropertyData(new Property("radius", new Fractional((float) radius)));
		return c;
	}

	@Test
	public void broadphaseUsesRadiiPlusMotionSweep() {
		PhysicalCircle a = circle(0, 0, 10);
		PhysicalCircle b = circle(30, 0, 10);

		// gap 30 > 10+10: still out of reach with both idle
		assertFalse(shapes.isInteractionPossible(a, b));

		// a sweeps 12 px this interval: reach = 20 + 12 = 32 > 30
		a.setImpulse(new Vector2(1, 0), 12);
		assertTrue(shapes.isInteractionPossible(a, b));

		// a too-small sweep still misses
		a.movement.currentSpeed.value = 5;
		assertFalse(shapes.isInteractionPossible(a, b));
	}

	@Test
	public void broadphaseScalesWithProcessedIntervalRatio() {
		IntervalPhysics.PROCESSED_INTERVAL = 34; // ratio 2 -> every moving shape reaches twice as far
		PhysicalCircle a = circle(0, 0, 10);
		PhysicalCircle b = circle(40, 0, 10);

		a.setImpulse(new Vector2(1, 0), 12);
		// reach = 20 + 12*2 = 44 > 40
		assertTrue(shapes.isInteractionPossible(a, b));
	}

	@Test
	public void equalMassHeadOnSplitsSpeedBetweenTheTwoShapes() {
		PhysicalCircle a = circle(0, 0, 10);
		PhysicalCircle b = circle(20, 0, 10);

		a.setImpulse(new Vector2(1, 0), 10);
		a.contactPoint.set(10, 0);
		b.contactPoint.set(10, 0);

		shapes.toHandleInteraction(a, b);

		// InteractionShapes:179-224 resolves the contact point velocity and then
		// re-distributes it with (m - I) / (m + I), I = m*r^2/(2*PI^2*PHYSICAL_UNIT^2).
		// For r = 10 and PHYSICAL_UNIT = 1 the inertia term is 50.7 against m = 10,
		// so the textbook full exchange is not what the engine produces
		assertEquals(6.703, b.movement.currentSpeed.value, 0.01);
		assertEquals(1, b.movement.getX(), 0.01);
		assertEquals(0, b.movement.getY(), 0.01);

		assertEquals(3.297, a.movement.currentSpeed.value, 0.01);
		assertEquals(1, a.movement.getX(), 0.01);

		// both momenta stay positive: the follower is still faster, so they separate
		assertTrue(b.movement.currentSpeed.value > a.movement.currentSpeed.value);
		assertEquals(0, a.movement.alphaRotation.value, 0.01);
		assertEquals(0, b.movement.alphaRotation.value, 0.01);
	}

	@Test
	public void heavierShapeKnocksLighterOneFasterAndKeepsItsCourse() {
		PhysicalCircle heavy = circle(0, 0, 10);
		PhysicalCircle light = circle(20, 0, 10);
		heavy.weight = new Fractional(40);
		light.weight = new Fractional(10);

		heavy.setImpulse(new Vector2(1, 0), 10);
		heavy.contactPoint.set(10, 0);
		light.contactPoint.set(10, 0);

		shapes.toHandleInteraction(heavy, light);

		assertTrue("light shape must be launched faster than the impact speed",
				light.movement.currentSpeed.value > 10);
		assertTrue(heavy.movement.currentSpeed.value > 0);
		assertTrue(heavy.movement.getX() > 0);
	}

	@Test
	public void staticSecondOperandMirrorsTheMovingOne() {
		PhysicalCircle ball = circle(0, 0, 10);
		PhysicalCircle wall = circle(20, 0, 10);
		wall.isStatic.value = true;

		ball.setImpulse(new Vector2(1, 0), 10);
		ball.contactPoint.set(10, 0);
		wall.contactPoint.set(10, 0);

		shapes.toHandleInteraction(ball, wall);

		// InteractionShapes:182-184 turns the contact point velocity around, which is
		// the correct mirror image; the (m - I) / (m + I) factor then attenuates it
		assertEquals(-1, ball.movement.getX(), 0.01);
		assertEquals(3.406, ball.movement.currentSpeed.value, 0.01);
		assertEquals(0, wall.movement.currentSpeed.value, EPS);
		assertEquals(0, ball.movement.alphaRotation.value, 0.01);
	}

	@Test
	public void shippedUnitScaleRestoresAMirrorLikeBounce() {
		// the shipped maps set ph_unit = DisplayWidth/10240 (0.1 at 1024 px). With that
		// scale I = m*r^2/(2*PI^2*PHYSICAL_UNIT^2) = 5066 for m = 10, r = 10, so
		// (m - I)/(m + I) ~ -0.996 and the contact response becomes a near-perfect
		// reflection instead of the spin-heavy result produced by PHYSICAL_UNIT = 1
		IntervalPhysics.PHYSICAL_UNIT = 0.1d;

		PhysicalCircle ball = circle(0, 0, 10);
		PhysicalCircle wall = circle(20, 0, 10);
		wall.isStatic.value = true;

		ball.setImpulse(new Vector2(1, 0), 10);
		ball.contactPoint.set(10, 0);
		wall.contactPoint.set(10, 0);

		shapes.toHandleInteraction(ball, wall);

		assertEquals(-1, ball.movement.getX(), 0.01);
		assertEquals(9.921, ball.movement.currentSpeed.value, 0.02);
		assertEquals(0, ball.movement.alphaRotation.value, 0.01);
	}

	@Test
	public void staticFirstOperandDoesNotMirrorTheMovingOne() {
		PhysicalCircle wall = circle(0, 0, 10);
		PhysicalCircle ball = circle(20, 0, 10);
		wall.isStatic.value = true;

		ball.setImpulse(new Vector2(-1, 0), 10);
		wall.contactPoint.set(10, 0);
		ball.contactPoint.set(10, 0);

		shapes.toHandleInteraction(wall, ball);

		// Pinned asymmetry, not intended behaviour: InteractionShapes:195-197 mirrors
		// oldFirstShapeContactPointMove (the velocity of the *static* operand, i.e. 0)
		// instead of oldSecondShapeContactPointMove, so the bounce of the second shape
		// is lost and only the inertia coupling term survives.
		assertEquals(-1, ball.movement.getX(), 0.01);
		assertEquals(3.297, ball.movement.currentSpeed.value, 0.01);
		assertEquals(0, wall.movement.currentSpeed.value, EPS);
	}

	@Test
	public void grazingHitIntroducesSpin() {
		// contact far off-center on a: normal along x, contact point off-axis
		PhysicalCircle a = circle(0, 0, 10);
		PhysicalCircle b = circle(18, 6, 10);

		a.setImpulse(new Vector2(1, 0), 10);
		a.contactPoint.set(7.2, 5.4);
		b.contactPoint.set(7.2, 5.4);

		shapes.toHandleInteraction(a, b);

		assertTrue("off-center hit must spin at least one body",
				a.movement.alphaRotation.value != 0 || b.movement.alphaRotation.value != 0);
		assertTrue(b.movement.currentSpeed.value > 0);
	}
}
