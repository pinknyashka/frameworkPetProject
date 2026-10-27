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
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Movement3;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link HandlerMediums} registration, data transfer and drag dispatch.
 *
 * The handler keeps three lists (all bodies, all mediums, bodies that are still
 * moving); {@link #findInteractions()} is its whole tick. Note that every
 * {@code data.getContext() == ...} comparison in transferData succeeds when both
 * sides are null, so a component that was never wired into a DataContext still
 * transfers into a context-less body.
 */
public class HandlerMediumsTest {

	private HandlerMediums handler;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		new InteractionMediumBody();
		handler = new HandlerMediums();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	private PhysicalMedium medium(String name, double slowingEffect, double size) {
		PhysicalMedium m = new PhysicalMedium();
		Entity entity = new Entity(name);
		m.entity = entity;
		entity.lstPhysicals.add(m);
		m.position.set(0, 0, 0);
		m.setPropertyData(new Property("size:x", new Fractional((float) size)));
		m.setPropertyData(new Property("size:y", new Fractional((float) size)));
		m.slowingEffect.value = (float) slowingEffect;
		return m;
	}

	private PhysicalBody body(String name, double roughness) {
		PhysicalBody b = new PhysicalBody();
		Entity entity = new Entity(name);
		b.entity = entity;
		entity.lstPhysicals.add(b);
		b.position.set(0, 0, 0);
		b.roughness.value = (float) roughness;
		return b;
	}

	@Test
	public void connectComponentRegistersMediumsAndMovingBodies() {
		PhysicalBody moving = body("moving", 0.5);
		moving.setImpulse(new Vector2(1, 0), 20);
		PhysicalBody idle = body("idle", 0.5);

		handler.connectComponent(moving);
		handler.connectComponent(idle);
		handler.connectComponent(medium("water", 0.5, 100));

		assertEquals(2, handler.lstBody.size());
		assertEquals(1, handler.lstMediums.size());
		assertEquals(1, handler.lstActiveBody.size());
		assertSame(moving, handler.lstActiveBody.get(0));
		assertFalse(handler.haveWork());

		// registering twice must not duplicate the entries
		handler.connectComponent(moving);
		assertEquals(2, handler.lstBody.size());
		assertEquals(1, handler.lstActiveBody.size());
	}

	@Test
	public void tickSlowsDownEveryActiveBodyInsideEveryEnabledMedium() {
		PhysicalMedium water = medium("water", 0.5, 100);
		PhysicalBody moving = body("moving", 0.5);
		moving.setImpulse(new Vector2(1, 0), 25);

		handler.connectComponent(moving);
		handler.connectComponent(water);

		handler.findInteractions();

		assertEquals(17.284f, moving.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void bodyOutsideTheValidatedMediumAreaIsNotSlowed() {
		PhysicalMedium water = medium("water", 0.5, 10);

		PhysicalBody moving = body("moving", 0.5);
		moving.setImpulse(new Vector2(1, 0), 25);

		handler.connectComponent(moving);
		handler.connectComponent(water);

		water.position.set(500, 500, 0);
		water.occupiedArea.validateAabb();

		handler.findInteractions();

		assertEquals(25f, moving.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void movedMediumKeepsDraggingAtItsOldPlaceUntilTheAreaIsValidated() {
		PhysicalMedium water = medium("water", 0.5, 10);

		PhysicalBody moving = body("moving", 0.5);
		moving.setImpulse(new Vector2(1, 0), 25);

		handler.connectComponent(moving);
		handler.connectComponent(water);

		// Pinned current behaviour: unlike HandlerShapesCollisions, the Position3
		// branch of HandlerMediums.transferData never calls occupiedArea.validateAabb(),
		// so the cached AABB (and therefore the drag region) stays where it was built
		water.position.set(500, 500, 0);
		handler.transferData(water.entity, water.position);
		handler.findInteractions();

		assertEquals(17.284f, moving.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void disabledMediumIsSkipped() {
		PhysicalMedium water = medium("water", 0.5, 100);
		water.isEnabled.value = false;

		PhysicalBody moving = body("moving", 0.5);
		moving.setImpulse(new Vector2(1, 0), 25);

		handler.connectComponent(moving);
		handler.connectComponent(water);
		handler.findInteractions();

		assertEquals(25f, moving.movement.currentSpeed.value, 0.01f);
	}

	@Test
	public void stoppedBodyIsDroppedFromTheActiveList() {
		PhysicalBody idle = body("idle", 0.5);
		handler.connectComponent(idle);
		handler.lstActiveBody.add(idle);

		handler.findInteractions();

		assertTrue(handler.lstActiveBody.isEmpty());
	}

	@Test
	public void movementTransferMakesTheBodyActiveAndGivesTheHandlerWork() {
		PhysicalBody idle = body("idle", 0.5);
		handler.connectComponent(idle);

		Movement3 incoming = new Movement3(1, 0, 0);
		incoming.currentSpeed.value = 20;

		assertTrue(handler.transferData(idle.entity, incoming));
		assertEquals(20f, idle.movement.currentSpeed.value, 0.01f);
		assertEquals(1, handler.lstActiveBody.size());
		assertTrue(handler.haveWork());
	}

	@Test
	public void disablingTransferStopsAndRemovesTheBody() {
		PhysicalBody moving = body("moving", 0.5);
		moving.setImpulse(new Vector2(1, 0), 20);
		handler.connectComponent(moving);

		Bool gone = new Bool(false);
		gone.setName("exist");

		assertTrue(handler.transferData(moving.entity, gone));
		assertFalse(moving.isEnabled.value);
		assertFalse(moving.movement.isMoving());
		assertTrue(handler.lstActiveBody.isEmpty());
		assertFalse(handler.haveWork());
	}

	@Test
	public void positionTransferMovesTheBody() {
		PhysicalBody body = body("body", 0.5);
		handler.connectComponent(body);

		Position3 target = new Position3();
		target.set(40, 60, 0);

		assertTrue(handler.transferData(body.entity, target));
		assertEquals(40f, body.position.getX(), 0.01f);
		assertEquals(60f, body.position.getY(), 0.01f);
	}

	@Test
	public void transferIgnoresEntitiesWithoutBodies() {
		PhysicalMedium water = medium("water", 0.5, 100);
		handler.connectComponent(water);

		assertFalse(handler.transferData(water.entity, new Movement3(1, 0, 0)));
	}
}
