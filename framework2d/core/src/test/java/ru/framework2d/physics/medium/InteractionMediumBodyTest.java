package ru.framework2d.physics.medium;

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
 * Drag of a {@link PhysicalBody} inside a {@link PhysicalMedium}.
 *
 * The factor is {@code k = sin((mediumFlat + slowing * speedReduce * bodyFlat) * PI / 2)}
 * where {@code mediumFlat = 1 - slowingEffect}, {@code bodyFlat = 1 - roughness} and
 * {@code speedReduce = min(speed, IMPULSE_LIMIT) / IMPULSE_LIMIT}. The squared part of
 * {@link InteractionMediumBody#toHandleInteraction} folds an interval longer than
 * {@code BASE_PHYSICAL_INTERVAL} into the same factor, so the residual fraction is
 * always below 1 and k can never become negative.
 */
public class InteractionMediumBodyTest {

	private static final double EPS = 1e-3;

	private InteractionMediumBody interaction;

	@Before
	public void setUp() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		interaction = new InteractionMediumBody();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	private PhysicalMedium medium(double slowingEffect, double size) {
		PhysicalMedium m = new PhysicalMedium();
		m.entity = new Entity("medium");
		m.position.set(0, 0, 0);
		// size:x / size:y are handled explicitly by PhysicalMedium.setPropertyData,
		// slowingEffect is a plain data field: a detached component has no lstData,
		// so it must be assigned directly (the parser wires it through setContextToComponent)
		m.setPropertyData(new Property("size:x", new Fractional((float) size)));
		m.setPropertyData(new Property("size:y", new Fractional((float) size)));
		m.slowingEffect.value = (float) slowingEffect;
		return m;
	}

	private PhysicalBody body(double x, double y, double roughness) {
		PhysicalBody b = new PhysicalBody();
		b.entity = new Entity("body");
		b.position.set(x, y, 0);
		b.roughness.value = (float) roughness;
		return b;
	}

	@Test
	public void constructorRegistersItselfIntoMediumRegistry() {
		assertSame(interaction, new PhysicalMedium()
				.getInteractionHandler(TestSupport.componentClass(PhysicalBody.class)));
	}

	@Test
	public void containmentIsTheOnlyBroadphaseTest() {
		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody inside = body(10, 10, 0.5);
		PhysicalBody outside = body(200, 0, 0.5);

		assertTrue(interaction.isInteractionHappened(m, inside));
		assertFalse(interaction.isInteractionHappened(m, outside));

		// the medium handler never calls the broadphase
		assertFalse(interaction.isInteractionPossible(m, inside));
	}

	@Test
	public void fullyStillBodyStaysStill() {
		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);

		assertTrue(interaction.toHandleInteraction(m, b));
		assertEquals(0f, b.movement.currentSpeed.value, 0f);
		assertEquals(0f, b.movement.alphaRotation.value, 0f);
	}

	@Test
	public void spinAloneCountsAsMovementForTheDrag() {
		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);
		b.movement.alphaRotation.value = 3;

		// s = sqrt(currentSpeed^2 + alphaRotation^2), so a spinning but untranslating
		// body is damped as well instead of hitting the "s < 0.01" short circuit
		interaction.toHandleInteraction(m, b);

		assertEquals(0f, b.movement.currentSpeed.value, 0f);
		assertEquals(1.5707f, b.movement.alphaRotation.value, 0.001f);
	}

	@Test
	public void slowdownMatchesTheFlatnessFormula() {
		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);
		b.setImpulse(new Vector2(1, 0), 25);

		// interval == 1: k = sin(0.625 * PI / 2) blended once with the interval
		interaction.toHandleInteraction(m, b);
		assertEquals(17.284f, b.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void longerIntervalCompoundsTheSlowdown() {
		IntervalPhysics.PROCESSED_INTERVAL = 34;

		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);
		b.setImpulse(new Vector2(1, 0), 25);

		interaction.toHandleInteraction(m, b);
		assertEquals(11.949f, b.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void speedIsClampedToImpulseLimitBeforeTheFormula() {
		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);
		b.setImpulse(new Vector2(1, 0), 100);

		// s is clamped to IMPULSE_LIMIT (50) for speedReduce, but the raw speed is scaled
		interaction.toHandleInteraction(m, b);
		assertEquals(85.355f, b.movement.currentSpeed.value, 0.02f);
	}

	@Test
	public void spinIsDampedWithTheSameFactor() {
		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);
		b.setImpulse(new Vector2(1, 0), 25);
		b.movement.alphaRotation.value = 10;

		// the factor comes from sqrt(speed^2 + spin^2) = 26.93, i.e. a slightly
		// stronger drag than for the linear speed alone, and both are scaled by it
		interaction.toHandleInteraction(m, b);

		assertEquals(17.631f, b.movement.currentSpeed.value, 0.01f);
		assertEquals(7.052f, b.movement.alphaRotation.value, 0.01f);
	}

	@Test
	public void veryLongIntervalStopsTheBodyInsteadOfReversingIt() {
		IntervalPhysics.PROCESSED_INTERVAL = IntervalPhysics.INTERVAL_LIMIT;

		PhysicalMedium m = medium(0.5, 100);
		PhysicalBody b = body(0, 0, 0.5);
		b.setImpulse(new Vector2(1, 0), 25);

		interaction.toHandleInteraction(m, b);

		// the squaring loop drives k to ~0.002 and the leftover fraction (always < 1)
		// keeps the result positive, so a long interval never flips the direction
		assertTrue(b.movement.currentSpeed.value > 0);
		assertTrue(b.movement.currentSpeed.value < 0.05);
	}

	@Test
	public void defaultsGiveTheStrongestDrag() {
		// slowingEffect and roughness both default to 1: mediumFlat and bodyFlat are 0,
		// so k collapses to sin(0) and the body stops in a single interval
		PhysicalMedium m = medium(1.0, 100);
		PhysicalBody b = body(0, 0, 1.0);
		b.setImpulse(new Vector2(1, 0), 25);

		interaction.toHandleInteraction(m, b);
		assertEquals(0f, b.movement.currentSpeed.value, EPS);
	}

	@Test
	public void frictionlessMediumKeepsTheBodyMoving() {
		PhysicalMedium m = medium(0.0, 100);
		PhysicalBody b = body(0, 0, 0.0);
		b.setImpulse(new Vector2(1, 0), 25);

		// mediumFlat = 1 -> k = sin(PI / 2) = 1, the residual blend keeps k at 1
		interaction.toHandleInteraction(m, b);
		assertEquals(25f, b.movement.currentSpeed.value, 1e-4f);
	}
}
