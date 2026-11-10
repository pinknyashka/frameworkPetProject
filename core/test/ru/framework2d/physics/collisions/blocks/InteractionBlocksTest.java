package ru.framework2d.physics.collisions.blocks;

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
import ru.framework2d.testsupport.TestSupport;

/**
 * Narrowphase of {@link InteractionBlocks}: a swept vertex-versus-border test.
 *
 * Two properties are pinned here because they decide every block setup:
 * <ul>
 * <li>only a vertex that crosses the <em>interior</em> of the opposite border during
 * the processed interval is a collision, so already overlapping blocks are never
 * detected (InteractionBlocks:337-342 rejects crossings outside the segment);</li>
 * <li>an axis-aligned approach path is lost: InteractionBlocks.findCrossPoint
 * divides by the movement slope K2 without guarding K2 == 0, so a horizontal
 * vertex path against an axis-aligned border evaluates to NaN and every border
 * test fails.</li>
 * </ul>
 */
public class InteractionBlocksTest {

	private InteractionBlocks blocks;

	@Before
	public void setUp() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		blocks = new InteractionBlocks();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	private PhysicalBlock block(String name, double x, double y, double w, double h) {
		PhysicalBlock b = new PhysicalBlock();
		// components always live inside an entity; the narrowphase log branch
		// (and toHandleInteraction->movement.commit) dereferences entity
		b.entity = new Entity(name);
		b.position.set(x, y, 0);
		b.setPropertyData(new Property("width", new Fractional((float) w)));
		b.setPropertyData(new Property("height", new Fractional((float) h)));
		return b;
	}

	@Test
	public void constructorRegistersItselfIntoBlockStatics() {
		assertSame(blocks, PhysicalBlock.BROADPHASE_HANDLER);
		assertSame(blocks, new PhysicalBlock().getInteractionHandler(PhysicalBlock.REFLECTION_CLASS));
	}

	@Test
	public void broadphaseUsesHalfDiagonalRadiusPlusTravel() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 200, 0, 10, 10);
		a.setImpulse(new Vector2(1, 0), 10);
		// reach = 7.07 + 7.07 + 10 * (17 / 17) = 24.1 < 200
		assertFalse(blocks.isInteractionPossible(a, b));

		PhysicalBlock near = block("near", 24, 0, 10, 10);
		assertTrue(blocks.isInteractionPossible(a, near));
	}

	@Test
	public void tiltedApproachCollidesAndFillsContactPoints() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 11, 2, 10, 10);
		a.setImpulse(new Vector2(1, 0.3), 10);

		assertTrue(blocks.isInteractionHappened(a, b));

		// the first shape receives the opposite block's vertex,
		// the second shape the crossing point on its own border
		assertEquals(6.0, a.contactPoint.getX(), 0.01);
		assertEquals(-3.0, a.contactPoint.getY(), 0.01);
		assertEquals(5.0, b.contactPoint.getX(), 0.01);
		assertEquals(-3.3, b.contactPoint.getY(), 0.01);
	}

	@Test
	public void tiltedCollisionDeflectsBothDynamicBlocks() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 11, 2, 10, 10);
		a.setImpulse(new Vector2(1, 0.3), 10);

		blocks.isInteractionHappened(a, b);
		blocks.toHandleInteraction(a, b);

		// equal weights: the mover keeps most of its speed and is deflected
		// sideways, the hit block is launched along the contact normal
		assertEquals(8.189f, a.movement.currentSpeed.value, 0.01f);
		assertTrue(a.movement.getY() > 0.28);
		assertTrue(a.movement.alphaRotation.value > 3.4 && a.movement.alphaRotation.value < 3.5);

		assertEquals(1.729f, b.movement.currentSpeed.value, 0.01f);
		assertTrue(b.movement.getX() > 0.7);
		assertTrue(b.movement.alphaRotation.value < -2.6);
	}

	@Test
	public void staticBlockStaysStillWhileMoverIsDeflectedAndSpun() {
		PhysicalBlock mover = block("mover", 0, 0, 10, 10);
		PhysicalBlock wall = block("wall", 11, 2, 10, 10);
		wall.isStatic.value = true;
		mover.setImpulse(new Vector2(1, 0.3), 10);

		assertTrue(blocks.isInteractionHappened(mover, wall));
		blocks.toHandleInteraction(mover, wall);

		assertEquals(0f, wall.movement.currentSpeed.value, 1e-9);
		assertEquals(0f, wall.movement.alphaRotation.value, 1e-9);

		// with PHYSICAL_UNIT = 1 the inertia term I = m*r^2/(2*PI^2) dominates the
		// mass, so the response is mostly rotation instead of a mirror reflection
		assertEquals(7.057f, mover.movement.currentSpeed.value, 0.01f);
		assertTrue(mover.movement.alphaRotation.value > 6.8);
	}

	@Test
	public void twoMovingBlocksBounceApart() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 11, 2, 10, 10);
		a.setImpulse(new Vector2(1, 0.3), 10);
		b.setImpulse(new Vector2(-1, -0.3), 10);

		assertTrue(blocks.isInteractionHappened(a, b));
		blocks.toHandleInteraction(a, b);

		assertTrue(a.movement.getX() > 0.6);
		assertTrue(b.movement.getX() < -0.9);
		assertEquals(7.057f, a.movement.currentSpeed.value, 0.01f);
		assertEquals(7.010f, b.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void futureCollisionTightensTheInterval() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 12, 2, 10, 10);
		a.setImpulse(new Vector2(1, 0.3), 10);

		assertFalse(blocks.isInteractionHappened(a, b));
		assertEquals(3, IntervalPhysics.PROCESSED_INTERVAL);
	}

	@Test
	public void separatingBlocksDoNotCollide() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 12, 0, 10, 10);
		a.setImpulse(new Vector2(-1, 0), 20); // a moves away from b

		assertFalse(blocks.isInteractionHappened(a, b));
	}

	@Test
	public void alreadyOverlappingBlocksAreNeverDetected() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 7, 3, 10, 10);
		a.setImpulse(new Vector2(1, 0.3), 10);

		// the swept test needs a vertex to enter the border segment from outside;
		// a map that spawns interpenetrating blocks gets no response at all
		assertFalse(blocks.isInteractionHappened(a, b));
		assertEquals(0.0, a.contactPoint.getX(), 0.0);
		assertEquals(0.0, b.contactPoint.getX(), 0.0);
		assertTrue(IntervalPhysics.PROCESSED_INTERVAL <= 17);
	}

	@Test
	public void axisAlignedFaceApproachIsLostWithNaNContactMath() {
		PhysicalBlock a = block("a", 0, 0, 10, 10);
		PhysicalBlock b = block("b", 11, 1, 10, 10);
		a.setImpulse(new Vector2(1, 0), 10);

		// K2 == 0 in findCrossPoint -> NaN, so every border test is rejected and
		// neither a collision nor an interval tightening happens
		assertFalse(blocks.isInteractionHappened(a, b));
		assertEquals(17, IntervalPhysics.PROCESSED_INTERVAL);
		assertEquals(0.0, a.contactPoint.getX(), 0.0);

		// there is no guard either: handling an unlocated contact point (0,0)
		// divides by a zero contact radius
		blocks.toHandleInteraction(a, b);
		assertTrue(Double.isNaN(a.movement.currentSpeed.value));
	}
}
