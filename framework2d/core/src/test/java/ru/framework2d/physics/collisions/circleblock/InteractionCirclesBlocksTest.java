package ru.framework2d.physics.collisions.circleblock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.blocks.PhysicalBlock;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * Mixed circle/block narrowphase.
 *
 * The circle branch of {@link InteractionCirclesBlocks#timeToCollision()} first
 * solves the block vertex against the circle rim, then the circle rim against the
 * nearest block border. InteractionCirclesBlocks.findCrossPoint divides by the
 * relative movement slope K2 without guarding K2 == 0, so a purely horizontal
 * approach against a vertical face collapses to NaN and is silently dropped; all
 * positive cases below therefore use a slightly tilted path.
 */
public class InteractionCirclesBlocksTest {

	private InteractionCirclesBlocks interaction;

	@Before
	public void setUp() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		interaction = new InteractionCirclesBlocks();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	private PhysicalCircle circle(String name, double x, double y, double radius) {
		PhysicalCircle c = new PhysicalCircle();
		c.entity = new Entity(name);
		c.position.set(x, y, 0);
		c.setPropertyData(new Property("radius", new Fractional((float) radius)));
		return c;
	}

	private PhysicalBlock block(String name, double x, double y, double w, double h) {
		PhysicalBlock b = new PhysicalBlock();
		b.entity = new Entity(name);
		b.position.set(x, y, 0);
		b.setPropertyData(new Property("width", new Fractional((float) w)));
		b.setPropertyData(new Property("height", new Fractional((float) h)));
		return b;
	}

	@Test
	public void constructorWiresBothShapeRegistries() {
		assertSame(interaction, PhysicalCircle.BROADPHASE_HANDLER);
		assertSame(interaction, PhysicalBlock.BROADPHASE_HANDLER);
		// only the circle side learns how to react to a block
		assertSame(interaction, new PhysicalCircle().getInteractionHandler(PhysicalBlock.REFLECTION_CLASS));
	}

	@Test
	public void broadphaseAddsHalfDiagonalRadiusAndTravel() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 200, 0, 10, 10);
		c.setImpulse(new Vector2(1, 0), 10);
		// reach = 5 + 7.07 + 10 * (17 / 17) = 22.07 < 200
		assertFalse(interaction.isInteractionPossible(c, b));

		PhysicalBlock near = block("near", 20, 0, 10, 10);
		assertTrue(interaction.isInteractionPossible(c, near));
	}

	@Test
	public void tiltedCircleHitsBlockFaceAndFillsBothContactPoints() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 10.5, 1, 10, 10);
		c.setImpulse(new Vector2(1, 0.2), 10);

		assertTrue(interaction.isInteractionHappened(c, b));

		// circle rim point and the nearest point on the block's left face
		assertEquals(5.0, c.contactPoint.getX(), 0.01);
		assertEquals(0.0, c.contactPoint.getY(), 0.01);
		assertEquals(5.5, b.contactPoint.getX(), 0.01);
		assertEquals(0.0, b.contactPoint.getY(), 0.01);
	}

	@Test
	public void tiltedFaceHitDeflectsTheCircleAndSpinsIt() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 10.5, 1, 10, 10);
		c.setImpulse(new Vector2(1, 0.2), 10);

		interaction.isInteractionHappened(c, b);
		interaction.toHandleInteraction(c, b);

		assertEquals(8.454f, c.movement.currentSpeed.value, 0.01f);
		assertEquals(0.973f, c.movement.getX(), 0.01f);
		assertTrue(c.movement.getY() > 0.23);
		assertTrue(c.movement.alphaRotation.value > 1.6);
	}

	@Test
	public void staticBlockIsNeverMovedByTheCircle() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock wall = block("wall", 10.5, 1, 10, 10);
		wall.isStatic.value = true;
		c.setImpulse(new Vector2(1, 0.2), 10);

		assertTrue(interaction.isInteractionHappened(c, wall));
		interaction.toHandleInteraction(c, wall);

		assertEquals(0f, wall.movement.currentSpeed.value, 1e-9);
		assertEquals(0f, wall.movement.alphaRotation.value, 1e-9);

		// Pinned current behaviour: with PHYSICAL_UNIT = 1 the inertia term
		// I = m*r^2/(2*PI^2) is close to the mass m for r = 5, so the linear part of
		// the reflection almost cancels and the circle keeps drifting towards the
		// wall while most of the impulse becomes spin.
		assertEquals(6.924f, c.movement.currentSpeed.value, 0.01f);
		assertTrue(c.movement.getX() > 0.95);
		assertTrue(c.movement.alphaRotation.value > 3.2);
	}

	@Test
	public void circleHitsBlockCornerAlongTheDiagonal() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 8, 8, 10, 10);
		c.setImpulse(new Vector2(1, 1), 10);

		assertTrue(interaction.isInteractionHappened(c, b));

		// the vertex branch wins: the block corner is the contact point of the block,
		// the rim point on the circle/vertex line is the contact point of the circle
		assertEquals(3.0, b.contactPoint.getX(), 0.01);
		assertEquals(3.0, b.contactPoint.getY(), 0.01);
		assertEquals(3.536, c.contactPoint.getX(), 0.01);
		assertEquals(3.536, c.contactPoint.getY(), 0.01);

		interaction.toHandleInteraction(c, b);

		assertEquals(8.824f, c.movement.currentSpeed.value, 0.01f);
		assertEquals(0.707f, c.movement.getX(), 0.01f);
		assertEquals(2.561f, b.movement.currentSpeed.value, 0.01f);
		assertTrue(b.movement.getX() > 0.7);
		// the corner hit transfers motion along the diagonal and does not spin the circle
		assertEquals(0f, c.movement.alphaRotation.value, 0.01f);
	}

	@Test
	public void approachingCollisionTightensTheInterval() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 14, 2, 10, 10);
		c.setImpulse(new Vector2(1, 0.2), 10);

		assertFalse(interaction.isInteractionHappened(c, b));
		assertEquals(6, IntervalPhysics.PROCESSED_INTERVAL);
	}

	@Test
	public void circleMovingAwayFromBlockNeverCollides() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 10.5, 1, 10, 10);
		c.setImpulse(new Vector2(-1, -0.2), 10);

		assertFalse(interaction.isInteractionHappened(c, b));
		assertTrue(IntervalPhysics.PROCESSED_INTERVAL <= 17);
	}

	@Test
	public void axisAlignedFaceApproachIsLostWithNaNContactMath() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		PhysicalBlock b = block("b", 10.5, 0, 10, 10);
		c.setImpulse(new Vector2(1, 0), 10);

		// vertical face + horizontal relative movement -> K2 == 0 -> NaN
		assertFalse(interaction.isInteractionHappened(c, b));
		assertEquals(17, IntervalPhysics.PROCESSED_INTERVAL);
		assertEquals(0.0, c.contactPoint.getX(), 0.0);
	}

	@Test
	public void circlePassingBesideTheBlockIsNotACollision() {
		PhysicalCircle c = circle("c", 0, 0, 5);
		// inside the broadphase reach (15.7 < 22.1) but far from the swept path
		PhysicalBlock b = block("b", 10.5, 14, 10, 10);
		c.setImpulse(new Vector2(1, 0), 10);

		assertTrue(interaction.isInteractionPossible(c, b));
		assertFalse(interaction.isInteractionHappened(c, b));
		assertTrue(IntervalPhysics.PROCESSED_INTERVAL <= 17);
	}
}
