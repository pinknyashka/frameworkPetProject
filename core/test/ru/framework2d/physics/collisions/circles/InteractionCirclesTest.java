package ru.framework2d.physics.collisions.circles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Property;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.testsupport.TestSupport;

public class InteractionCirclesTest {

	private static final double EPS = 1e-6;

	private InteractionCircles circles;

	@Before
	public void setUp() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		circles = new InteractionCircles();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	private PhysicalCircle circle(double x, double y, double radius, double speedX, double speed) {
		PhysicalCircle c = new PhysicalCircle();
		c.position.set(x, y, 0);
		c.setPropertyData(new Property("radius", new Fractional((float) radius)));
		if (speed != 0) {
			c.setImpulse(new Vector2(speedX, 0), speed);
		}
		return c;
	}

	@Test
	public void constructorRegistersItselfIntoCircleStatics() {
		assertSame(circles, PhysicalCircle.BROADPHASE_HANDLER);
		assertSame(circles, new PhysicalCircle().getInteractionHandler(PhysicalCircle.COMPONENT_CLASS));
	}

	@Test
	public void overlappingApproachingCirclesCollideThisInterval() {
		PhysicalCircle a = circle(0, 0, 10, 1, 10);
		PhysicalCircle b = circle(15, 0, 10, 0, 0);

		assertTrue(circles.isInteractionHappened(a, b));

		assertEquals(10, a.contactPoint.getX(), EPS);
		assertEquals(0, a.contactPoint.getY(), EPS);
		assertEquals(5, b.contactPoint.getX(), EPS);

		assertEquals(10f, a.collisionSpeed.value, 0.5f);
		assertEquals(10f, b.collisionSpeed.value, 0.5f);
	}

	@Test
	public void separatingCirclesDoNotReportCollision() {
		PhysicalCircle a = circle(0, 0, 10, -1, 10); // moving away from b
		PhysicalCircle b = circle(15, 0, 10, 0, 0);

		assertFalse(circles.isInteractionHappened(a, b));
		assertEquals(17, IntervalPhysics.PROCESSED_INTERVAL);
	}

	@Test
	public void futureCollisionTightensTheInterval() {
		PhysicalCircle a = circle(0, 0, 10, 1, 10);
		PhysicalCircle b = circle(24, 0, 10, 0, 0);

		assertFalse(circles.isInteractionHappened(a, b));

		// analytic solution puts the impact around t=6..7 of the 17 ms interval
		assertTrue(IntervalPhysics.PROCESSED_INTERVAL > 2);
		assertTrue(IntervalPhysics.PROCESSED_INTERVAL < 17);
	}

	@Test
	public void distantCirclesLeaveIntervalUntouched() {
		PhysicalCircle a = circle(0, 0, 10, 1, 10);
		PhysicalCircle b = circle(300, 0, 10, 0, 0);

		assertFalse(circles.isInteractionHappened(a, b));
		assertEquals(17, IntervalPhysics.PROCESSED_INTERVAL);
	}

	@Test
	public void bothHeadsOnCollisionThenElasticResponse() {
		PhysicalCircle a = circle(0, 0, 10, 1, 10);
		PhysicalCircle b = circle(15, 0, 10, 0, 0);

		assertTrue(circles.isInteractionHappened(a, b));
		circles.toHandleInteraction(a, b);

		// equal weights, equal radii and a contact on the centre line: the impulse is
		// split by (m - I) / (m + I) with I = m*r^2/(2*PI^2*PHYSICAL_UNIT^2), which is
		// 50.7 against m = 10, so the transfer is attenuated instead of complete
		assertEquals(6.703, b.movement.currentSpeed.value, 0.01);
		assertTrue(b.movement.getX() > 0.99);
		assertEquals(3.297, a.movement.currentSpeed.value, 0.01);
		assertTrue(a.movement.getX() > 0.99);

		// a centred hit stores nothing in rotation
		assertEquals(0, a.movement.alphaRotation.value, 0.01);
		assertEquals(0, b.movement.alphaRotation.value, 0.01);
	}

	@Test
	public void headOnFromOppositeDirectionsBothReverse() {
		PhysicalCircle a = circle(0, 0, 10, 1, 10);
		PhysicalCircle b = circle(15, 0, 10, -1, 10);

		assertTrue(circles.isInteractionHappened(a, b));
		assertNotNull(a.contactPoint);
		circles.toHandleInteraction(a, b);

		assertEquals(3.406, a.movement.currentSpeed.value, 0.01);
		assertEquals(3.406, b.movement.currentSpeed.value, 0.01);
		assertTrue(a.movement.getX() < -0.99);
		assertTrue(b.movement.getX() > 0.99);
	}

	@Test
	public void collisionSpeedRecordsClosingRate() {
		PhysicalCircle a = circle(0, 0, 10, 1, 20);
		PhysicalCircle b = circle(10, 0, 10, -1, 20);

		assertTrue(circles.isInteractionHappened(a, b));
		// closing speed of both circles: 20 + 20
		assertEquals(40f, a.collisionSpeed.value, 1f);
	}
}
