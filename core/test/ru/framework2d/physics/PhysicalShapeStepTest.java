package ru.framework2d.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Property;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.collisions.PhysicalShape;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

public class PhysicalShapeStepTest {

	private static final double EPS = 1e-9;

	@Before
	public void setUp() {
		TestSupport.resetIntervalPhysics();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
	}

	@Test
	public void setImpulseNormalizesDirectionAndStoresMagnitude() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.setImpulse(new Vector2(3, 4), 42);

		assertEquals(0.6, circle.movement.getX(), EPS);
		assertEquals(0.8, circle.movement.getY(), EPS);
		assertEquals(42, circle.movement.currentSpeed.value, EPS);
		assertTrue(circle.isActive());
	}

	@Test
	public void stepMovesPositionAlongMovementBySpeedTimesInterval() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.position.set(0, 0, 0);
		circle.setImpulse(new Vector2(1, 0), 5);

		boolean stepped = circle.step(17, 1.0d);

		assertTrue(stepped);
		assertEquals(5, circle.position.getX(), EPS);
		assertEquals(0, circle.position.getY(), EPS);
	}

	@Test
	public void stepScalesWithIntervalRatio() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.position.set(0, 0, 0);
		circle.setImpulse(new Vector2(0, -1), 10);

		circle.step(34, 2.0d);

		assertEquals(-20, circle.position.getY(), EPS);
	}

	@Test
	public void stepAddsRotationAndWrapsAnglesBeyondTwoTurns() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.movement.alphaRotation.value = 1.0;

		circle.step(17, 1.0d);
		assertEquals(1.0, circle.position.getAlpha(), EPS);

		// alpha is in radians and wraps at +/-12.57 (not 2*PI): the branch subtracts 4*PI
		circle.position.alpha.value = 11.0;
		circle.movement.alphaRotation.value = 1.0;
		circle.step(17, 1.0d);
		assertEquals(12.0, circle.position.getAlpha(), EPS);

		circle.position.alpha.value = 12.0;
		circle.movement.alphaRotation.value = 1.0;
		circle.step(17, 1.0d);
		assertEquals(13.0 - Math.PI * 4, circle.position.getAlpha(), EPS);

		// the step guard is "alphaRotation > 0", so a negative spin only steps
		// together with a positive linear speed
		circle.position.alpha.value = -12.0;
		circle.movement.alphaRotation.value = -1.0;
		circle.setImpulse(new Vector2(1, 0), 0);
		assertFalse(circle.step(17, 1.0d));

		circle.setImpulse(new Vector2(1, 0), 1);
		assertTrue(circle.step(17, 1.0d));
		assertEquals(-13.0 + Math.PI * 4, circle.position.getAlpha(), EPS);
	}

	@Test
	public void stepReturnsFalseForStillShape() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.position.set(7, 7, 0);
		assertFalse(circle.step(17, 1.0d));
		assertEquals(7, circle.position.getX(), EPS);
	}

	@Test
	public void isEnabledGatesActiveState() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.setImpulse(new Vector2(1, 0), 3);
		assertTrue(circle.isActive());

		circle.isEnabled.value = false;
		assertFalse(circle.isActive());
	}

	@Test
	public void stepKeepsAxisAlignedBoundingBoxOfOccupiedAreaInSync() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.setPropertyData(new Property("radius", new Fractional(10f)));

		circle.position.set(0, 0, 0);
		circle.occupiedArea.validateAabb();
		assertEquals(-10, circle.occupiedArea.left_top.getX(), EPS);

		circle.setImpulse(new Vector2(1, 0), 10);
		circle.step(17, 1.0d);

		assertEquals(10, circle.position.getX(), EPS);
		// AABB must follow the moving center (radius 10 around x=10)
		assertTrue(circle.occupiedArea.isAabb);
		assertEquals(0, circle.occupiedArea.left_top.getX(), EPS);
		assertEquals(20, circle.occupiedArea.right_bottom.getX(), EPS);
	}

	@Test
	public void collisionSignalFieldsExistWithNeutralDefaults() {
		PhysicalCircle circle = new PhysicalCircle();
		assertNotNull(circle.collision); // Trigger only propagates via commit()
		assertEquals(0f, circle.collisionSpeed.value, 0f);
		assertFalse(circle.isStatic.value);
		assertEquals(10f, circle.weight.value, 0f);
	}

	@Test
	public void weightAndStaticArePlainDataFields() {
		PhysicalCircle circle = new PhysicalCircle();
		circle.weight = new Fractional(25);
		circle.isStatic = new Bool(true);
		assertEquals(25f, circle.weight.value, 0f);
		assertTrue(circle.isStatic.value);

		PhysicalShape unused = circle;
		assertEquals(circle.position, unused.occupiedArea.center_d);
	}
}
