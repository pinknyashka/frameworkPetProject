package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.physics.collisions.HandlerShapesCollisions;
import ru.framework2d.physics.collisions.InteractionShapes;
import ru.framework2d.physics.collisions.circles.InteractionCircles;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * Parses a minimal physics-only map and runs the real engine loop headlessly.
 *
 * This is the closest thing to a smoke run that fits a unit test: it proves that
 * the XML schema (<direct> -> <handler> -> <interaction>, component tags by
 * SHORT_CLASS_NAME, <context> pub/sub, arithmetic attributes) produces a working
 * collision solver, and that a committed impulse vector actually moves entities.
 *
 * The loop is driven through {@link EnginesScheduler#overDraw()}: with
 * EnginesManager.PARALLEL_WORK = false every tick is a synchronous depth-first
 * Engine.iterate(), and the scheduler's draw handoff is a plain flag, so no GL
 * is involved.
 */
public class MapParserPhysicsTest {

	private static final String MAP =
			"<map version = \"1.0\" name = \"physics test\" parallel = \"false\">"
			+ "<direct package = \"ru.framework2d.physics\" engineName = \"IntervalPhysics\""
			+ " ph_unit = \"1\" interval_limit = \"70\" impulse_limit = \"2\">"
			+ "<handler subpackage = \"collisions\" engineName = \"HandlerShapesCollisions\">"
			+ "<interaction subpackage = \"circles\" engineName = \"InteractionCircles\"/>"
			+ "<context name = \"impulse\" value = \"directImpulse\"/>"
			+ "<context name = \"isEnabled\" value = \"exist\"/>"
			+ "</handler>"
			+ "</direct>"
			+ "<replace name = \"r\" value = \"10\"/>"
			+ "<entity name = \"ballA\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"r\" weight = \"10\"/>"
			+ "</entity>"
			+ "<entity name = \"ballB\" single = \"true\">"
			+ "<circle position:x = \"35\" position:y = \"0\" radius = \"20/2\" weight = \"10\"/>"
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

		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
		scheduler = Engine.enginesScheduler;

		new MapParser(storage, enginesManager, null).parseMap(checkedParser());
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		TestSupport.clearFakeGraphics();
	}

	private org.xmlpull.v1.XmlPullParser checkedParser() {
		try {
			return TestSupport.xmlParser(MAP);
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

	private Engine findSubEngine(Engine root, Class<?> expected) {
		for (Engine sub : root.lstSubEngines) {
			if (expected.isInstance(sub)) return sub;
		}
		throw new AssertionError("no " + expected.getSimpleName() + " under " + root.name);
	}

	@Test
	public void mapBuildsThePhysicsEngineTree() {
		Engine solver = findSubEngine(scheduler, IntervalPhysics.class);
		assertTrue(solver instanceof IntervalPhysics);

		Engine handler = findSubEngine(solver, HandlerShapesCollisions.class);
		Engine interaction = findSubEngine(handler, InteractionCircles.class);

		assertTrue(interaction instanceof InteractionShapes);
		// the interaction registered itself as the broadphase of both shape classes
		assertSame(interaction, PhysicalCircle.BROADPHASE_HANDLER);
		assertSame(handler, interaction.masterEngine);
	}

	@Test
	public void mapAttributesBecomeComponentData() {
		PhysicalCircle a = circleOf("ballA");
		PhysicalCircle b = circleOf("ballB");

		assertEquals(0f, a.position.getX(), 0.0);
		assertEquals(10f, a.radius.value, 0.0f);
		assertEquals(10f, a.weight.value, 0.0f);

		// <replace name="r"> and arithmetic attributes are both resolved
		assertEquals(10f, b.radius.value, 0.0f);
		assertEquals(35f, b.position.getX(), 0.0f);

		// radius reaches the broadphase area
		assertEquals(10f, a.occupiedArea.radius, 0.0f);
		assertTrue(a.occupiedArea.isSphere);
	}

	@Test
	public void contextWiringMakesTheShapeImpulseSubscribable() {
		PhysicalCircle a = circleOf("ballA");

		assertNotNull(a.impulse.getContext());
		assertEquals("directImpulse", a.impulse.getContext().name);
		assertTrue(a.impulse.lstLinkedHandlers.contains(
				findSubEngine(findSubEngine(scheduler, IntervalPhysics.class),
						HandlerShapesCollisions.class)));
	}

	@Test
	public void committedImpulseLaunchesTheShapeThroughTheSolver() {
		PhysicalCircle a = circleOf("ballA");

		a.impulse.set(3, 4);
		a.impulse.commit(null, a.entity);

		// |direct| * impulse_limit = 5 * 2
		assertEquals(10f, a.movement.currentSpeed.value, 0.01f);
		assertTrue(a.movement.getX() > 0.59);
		assertTrue(a.movement.getX() < 0.61);
	}

	@Test
	public void solverStepsTheMovingShapeAndResolvesTheCollision() {
		PhysicalCircle a = circleOf("ballA");
		PhysicalCircle b = circleOf("ballB");

		a.impulse.set(1, 0);
		a.impulse.commit(null, a.entity);

		double startX = a.position.getX();
		assertTrue(a.movement.isMoving());
		assertEquals(0f, b.movement.currentSpeed.value, 0.0f);

		// bounded pump of the real loop: interval counters are wall-clock based,
		// so the number of ticks needed to close the gap is not fixed
		int ticks = 0;
		while (b.movement.currentSpeed.value == 0 && ticks < 400) {
			scheduler.overDraw();
			ticks++;
		}

		assertTrue("ball A must advance, moved by " + (a.position.getX() - startX),
				a.position.getX() > startX);
		assertTrue("ball B must be knocked by ball A after " + ticks + " tick(s)",
				b.movement.currentSpeed.value > 0);
		// the hit is head-on along the x axis, so B is driven away from A
		assertTrue(b.movement.getX() > 0.9);
		assertTrue(b.position.getX() >= 35);
		assertTrue(IntervalPhysics.PROCESSED_INTERVAL >= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL);
		assertTrue(IntervalPhysics.REMAINING_PHYSICAL_INTERVAL <= IntervalPhysics.INTERVAL_LIMIT);
	}
}
