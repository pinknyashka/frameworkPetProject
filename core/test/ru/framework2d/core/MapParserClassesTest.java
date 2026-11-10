package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.logic.classes.Cavalry;
import ru.framework2d.logic.classes.HandlerUnitClasses;
import ru.framework2d.logic.classes.Infantry;
import ru.framework2d.logic.classes.Minigun;
import ru.framework2d.logic.classes.UnitClass;
import ru.framework2d.logic.fields.LogicalUnit;
import ru.framework2d.logic.players.HandlerPlayers;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;
import ru.framework2d.touches.areas.direct.TouchableAreaDirect;

/**
 * Map-level wiring for the unit-class handler (phase 2 of plans/turn-based-logics-plan.md).
 *
 * The XML tags are parsed exactly as the shipped map uses them: the classes handler aliases its
 * trigger/bool fields to the touch shot, shape collision and field-exist contexts, and it installs
 * itself as the players handler's turn rule.
 */
public class MapParserClassesTest {

	private static final String LOGIC_TREE =
			"<direct package = \"ru.framework2d.logic\" engineName = \"DirectLogicStandart\">"
			+ "<handler subpackage = \"fields\" engineName = \"HandlerFields\">"
			+ "<context name = \"inField\" value = \"exist\"/>"
			+ "</handler>"
			+ "<handler subpackage = \"players\" engineName = \"HandlerPlayers\">"
			+ "<context name = \"isCurrentlyControlled\" value = \"isSelected\"/>"
			+ "<context name = \"isControlled\" value = \"isClikable\"/>"
			+ "<context name = \"isExist\" value = \"exist\"/>"
			+ "</handler>"
			+ "<handler subpackage = \"classes\" engineName = \"HandlerUnitClasses\">"
			+ "<context name = \"actionStarted\" value = \"directImpulse\"/>"
			+ "<context name = \"wasAttacked\" value = \"collision\"/>"
			+ "<context name = \"remainsOnBoard\" value = \"exist\"/>"
			+ "</handler>"
			+ "</direct>";

	private static final String PHYSICS_TREE =
			"<direct package = \"ru.framework2d.physics\" engineName = \"IntervalPhysics\""
			+ " ph_unit = \"1\" interval_limit = \"70\" impulse_limit = \"2\">"
			+ "<handler subpackage = \"collisions\" engineName = \"HandlerShapesCollisions\">"
			+ "<interaction subpackage = \"circles\" engineName = \"InteractionCircles\"/>"
			+ "<context name = \"impulse\" value = \"directImpulse\"/>"
			+ "<context name = \"isEnabled\" value = \"exist\"/>"
			+ "</handler>"
			+ "</direct>";

	private static final String TOUCH_TREE =
			"<direct package = \"ru.framework2d.touches\" engineName = \"DirectTouches\">"
			+ "<handler subpackage = \"areas\" engineName = \"HandlerTouchSelect\">"
			+ "<sub engineName = \"SubPointer\"/>"
			+ "<sub subpackage = \"direct\" engineName = \"SubDirectedTouch\"/>"
			+ "<context name = \"shotDirect\" value = \"directImpulse\"/>"
			+ "</handler>"
			+ "</direct>";

	private static final String MAP =
			"<map version = \"1.0\" name = \"unit classes test\" parallel = \"false\">"
			+ PHYSICS_TREE
			+ TOUCH_TREE
			+ LOGIC_TREE
			+ "<replace name = \"r\" value = \"4\"/>"
			+ "<entity name = \"board\" single = \"true\">"
			+ "<field name = \"board\" position:x = \"0\" position:y = \"0\""
			+ " size:x = \"100\" size:y = \"100\"/>"
			+ "<player name = \"p1\" gotAccess = \"true\"/>"
			+ "<player name = \"p2\" gotAccess = \"false\"/>"
			+ "</entity>"
			+ "<entity name = \"white\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"r\" weight = \"10\"/>"
			+ "<d_touch radius = \"r\" isClikable = \"true\"/>"
			+ "<unit fieldName = \"board\"/>"
			+ "<ownership ownerName = \"p1\"/>"
			+ "<infantry/>"
			+ "</entity>"
			+ "<entity name = \"horse\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"r\" weight = \"10\"/>"
			+ "<unit fieldName = \"board\"/>"
			+ "<ownership ownerName = \"p1\"/>"
			+ "<cavalry/>"
			+ "</entity>"
			+ "<entity name = \"gun\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"r\" weight = \"10\"/>"
			+ "<unit fieldName = \"board\"/>"
			+ "<ownership ownerName = \"p1\"/>"
			+ "<minigun/>"
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

		try {
			XmlPullParser parser = TestSupport.xmlParser(MAP);
			new MapParser(storage, enginesManager, null).parseMap(parser);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		TestSupport.clearFakeGraphics();
	}

	private Entity entityNamed(String name) {
		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(name)) return entity;
		}
		throw new AssertionError("no entity named " + name);
	}

	private <T> T componentOf(Entity entity, Class<T> expected) {
		for (Component component : entity.lstComponents) {
			if (expected.isInstance(component)) return expected.cast(component);
		}
		throw new AssertionError("no " + expected.getSimpleName() + " on entity " + entity.name);
	}

	private Engine findEngine(Engine root, Class<?> expected) {
		if (expected.isInstance(root)) return root;

		for (Engine sub : root.lstSubEngines) {
			Engine found = findEngine(sub, expected);
			if (found != null) return found;
		}
		return null;
	}

	private HandlerUnitClasses classHandler() {
		Engine found = findEngine(scheduler, HandlerUnitClasses.class);
		if (found == null) throw new AssertionError("no HandlerUnitClasses in the parsed engine tree");
		return (HandlerUnitClasses) found;
	}

	private boolean hasClass(HandlerUnitClasses classes, Class <? extends UnitClass> expected) {
		for (UnitClass unitClass : classes.lstUnitClasses) {
			if (expected.isInstance(unitClass)) return true;
		}
		return false;
	}

	private HandlerPlayers playerHandler() {
		Engine found = findEngine(scheduler, HandlerPlayers.class);
		if (found == null) throw new AssertionError("no HandlerPlayers in the parsed engine tree");
		return (HandlerPlayers) found;
	}

	@Test
	public void mapCreatesTheClassHandlerAndInstallsItAsTheTurnRule() {
		HandlerUnitClasses classes = classHandler();
		HandlerPlayers players = playerHandler();

		assertSame(classes, players.turnRules);
		assertEquals("class-abilities", players.turnRules.rulesName());
	}

	@Test
	public void infantryTagCreatesAnInfantryComponentRegisteredByTheClassHandler() {
		UnitClass component = componentOf(entityNamed("white"), UnitClass.class);

		assertTrue(component instanceof Infantry);
		assertTrue(classHandler().lstUnitClasses.contains(component));
	}

	@Test
	public void phase3TagsCreateCavalryAndMinigunComponents() {
		HandlerUnitClasses classes = classHandler();

		assertTrue(hasClass(classes, Cavalry.class));
		assertTrue(hasClass(classes, Minigun.class));
	}

	@Test
	public void classSignalsShareTheProducingContextsFromTouchPhysicsAndFields() {
		Entity white = entityNamed("white");
		UnitClass unitClass = componentOf(white, UnitClass.class);
		PhysicalCircle circle = componentOf(white, PhysicalCircle.class);
		TouchableAreaDirect touch = componentOf(white, TouchableAreaDirect.class);
		LogicalUnit unit = componentOf(white, LogicalUnit.class);

		assertNotNull(unitClass.actionStarted.getContext());
		assertNotNull(unitClass.wasAttacked.getContext());
		assertNotNull(unitClass.remainsOnBoard.getContext());

		assertEquals("directImpulse", unitClass.actionStarted.getContext().name);
		assertSame(unitClass.actionStarted.getContext(), touch.shotDirect.getContext());

		assertEquals("collision", unitClass.wasAttacked.getContext().name);
		assertSame(unitClass.wasAttacked.getContext(), circle.collision.getContext());

		assertEquals("exist", unitClass.remainsOnBoard.getContext().name);
		assertSame(unitClass.remainsOnBoard.getContext(), unit.inField.getContext());
	}
}
