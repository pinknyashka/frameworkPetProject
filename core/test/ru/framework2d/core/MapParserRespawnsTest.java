package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.data.Trigger;
import ru.framework2d.logic.grids.LogicalCellItem;
import ru.framework2d.logic.respawns.HandlerRespawns;
import ru.framework2d.logic.respawns.LogicalRespawn;
import ru.framework2d.logic.respawns.LogicalRespawner;
import ru.framework2d.testsupport.TestSupport;

/**
 * Prototype respawning driven through a parsed map: &lt;prototype&gt; + &lt;l_respawn&gt; +
 * &lt;respawner number=...&gt;, the clone top-up performed by HandlerRespawns.iterate, and the
 * context seams the shipped map relies on (cellPosition -&gt; respawningPosition, and a Trigger
 * published as "finishRound" -&gt; the respawn teleport).
 *
 * This is the map-level counterpart of the handler-level tests in
 * ru/framework2d/logic/respawns/HandlerRespawnsTest.java.
 */
public class MapParserRespawnsTest {

	private static final String LOGIC_TREE =
			"<direct package = \"ru.framework2d.logic\" engineName = \"DirectLogicStandart\">"
			+ "<handler subpackage = \"grids\" engineName = \"HandlerGrids\">"
			+ "<context name = \"cellPosition\" value = \"lposition\"/>"
			+ "</handler>"
			+ "<handler subpackage = \"respawns\" engineName = \"HandlerRespawns\">"
			+ "<context name = \"respawningPosition\" value = \"lposition\"/>"
			+ "<context name = \"respawningTrigger\" value = \"finishRound\"/>"
			+ "</handler>"
			+ "</direct>";

	private static final String BOARD =
			"<entity name = \"board\" single = \"true\">"
			+ "<grid name = \"board\" position:x = \"0\" position:y = \"0\""
			+ " size:x = \"8\" size:y = \"8\" columnsNumber = \"8\" rowsNumber = \"8\"/>"
			+ "<cell_group name = \"white_row\" gridName = \"board\" itemName = \"piece\""
			+ " leaderColumn = \"0\" leaderRow = \"0\" relativeCells = \"%s0,0,1,0\"/>"
			+ "</entity>";

	// HandlerRespawns.connectComponent links a prototype only to respawners that are registered
	// already (HandlerRespawns.java:177-183), so <respawner> must precede <prototype>.
	private static final String SPAWNERS =
			"<entity name = \"spawner\" single = \"true\">"
			+ "<respawner name = \"white_respawner\" number = \"2\"/>"
			+ "</entity>";

	private static final String PROTOTYPE =
			"<prototype name = \"checkerwhite\">"
			+ "<item name = \"piece\"/>"
			+ "<l_respawn respawnerName = \"white_respawner\"/>"
			+ "</prototype>";

	private static final String PIECE =
			"<entity name = \"piece1\" single = \"true\">"
			+ "<item name = \"piece\"/>"
			+ "</entity>";

	private static final String MAP =
			"<map version = \"1.0\" name = \"respawn test\" parallel = \"false\">"
			+ LOGIC_TREE + BOARD + SPAWNERS + PIECE + PROTOTYPE + "</map>";

	private static final String MAP_SWAPPED_ORDER =
			"<map version = \"1.0\" name = \"respawn order test\" parallel = \"false\">"
			+ LOGIC_TREE + BOARD + PROTOTYPE + PIECE + SPAWNERS + "</map>";

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

	private void parse(String xml) {

		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
		scheduler = Engine.enginesScheduler;

		try {
			XmlPullParser parser = TestSupport.xmlParser(xml);
			new MapParser(storage, enginesManager, null).parseMap(parser);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Entity entityNamed(String name) {

		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(name)) return entity;
		}
		throw new AssertionError("no entity named " + name);
	}

	private boolean hasEntity(String name) {

		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(name)) return true;
		}
		return false;
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

	private HandlerRespawns respawnsHandler() {

		Engine found = findEngine(scheduler, HandlerRespawns.class);
		if (found == null) throw new AssertionError("no HandlerRespawns in the parsed engine tree");
		return (HandlerRespawns) found;
	}

	private LogicalRespawner respawner() {

		return componentOf(entityNamed("spawner"), LogicalRespawner.class);
	}

	private LogicalCellItem piece() {

		return componentOf(entityNamed("piece1"), LogicalCellItem.class);
	}

	/** Pumps the real scheduler loop while the respawner is still short of its target count. */
	private int pumpUntilFilled(int maxTicks) {

		LogicalRespawner respawner = respawner();
		int ticks = 0;

		while (respawner.lstRespawns.size() < respawner.number.value && ticks < maxTicks) {
			scheduler.overDraw();
			ticks++;
		}
		return ticks;
	}

	// ------------------------------------------------------------------- registration

	@Test
	public void mapBuildsTheLogicEngineTreeWithBothHandlers() {

		parse(MAP);

		assertNotNull(findEngine(scheduler, HandlerRespawns.class));
		assertNotNull(findEngine(scheduler, ru.framework2d.logic.grids.HandlerGrids.class));
		assertSame(respawnsHandler().masterEngine,
				findEngine(scheduler, ru.framework2d.logic.DirectLogicStandart.class));
	}

	@Test
	public void prototypeIsRegisteredAndBoundToTheRespawnerParsedBeforeIt() {

		parse(MAP);

		LogicalRespawner respawner = respawner();
		assertNotNull(respawner.prototype);
		assertEquals("checkerwhite", respawner.prototype.name);
		assertTrue(respawner.prototype.isPrototype);
		assertEquals(2, respawner.number.value);

		// the prototype carries the l_respawn component that made it a spawn source
		assertNotNull(componentOf(respawner.prototype, LogicalRespawn.class));

		// parsing alone never spawns: the top-up is the handler's work
		assertTrue(respawner.lstRespawns.isEmpty());
		assertFalse(hasEntity("checkerwhite1"));
	}

	@Test
	public void cellPositionAndRespawningPositionAreTheSameContext() {

		parse(MAP);

		LogicalRespawn prototypeRespawn = componentOf(respawner().prototype, LogicalRespawn.class);

		assertEquals("lposition", prototypeRespawn.respawningPosition.getContext().name);
		assertSame(piece().cellPosition.getContext(),
				prototypeRespawn.respawningPosition.getContext());
	}

	// ------------------------------------------------------------------------ spawning

	@Test
	public void schedulerTickTopsTheRespawnerUpToItsConfiguredNumber() {

		parse(MAP);

		int ticks = pumpUntilFilled(200);
		LogicalRespawner respawner = respawner();

		assertEquals("no clone was spawned within " + ticks + " tick(s)",
				2, respawner.lstRespawns.size());
		assertTrue(hasEntity("checkerwhite1"));
		assertTrue(hasEntity("checkerwhite2"));

		for (LogicalRespawn respawn : respawner.lstRespawns) {
			assertSame(respawner, respawn.respawner);
			assertNotNull(respawn.entity);
			assertFalse(respawn.entity.isPrototype);
		}
	}

	@Test
	public void furtherTicksDoNotSpawnMoreThanTheConfiguredNumber() {

		parse(MAP);

		pumpUntilFilled(200);

		for (int i = 0; i < 30; i++) scheduler.overDraw();

		assertEquals(2, respawner().lstRespawns.size());
		assertFalse(hasEntity("checkerwhite3"));
	}

	@Test
	public void pins_prototypeParsedBeforeItsRespawnerIsNeverUsedAsASpawnSource() {

		parse(MAP_SWAPPED_ORDER);

		// HandlerRespawns.java:177-183 only looks at already registered respawners, and the
		// respawner branch never scans lstPrototypes, so the link is simply never made
		assertNull(respawner().prototype);

		for (int i = 0; i < 100; i++) scheduler.overDraw();

		assertTrue(respawner().lstRespawns.isEmpty());
		assertFalse(hasEntity("checkerwhite1"));
	}

	// ------------------------------------------------------------------- context seams

	@Test
	public void committingTheCellPositionOfASpawnedPieceMovesItsRespawningPosition() {

		parse(MAP);
		pumpUntilFilled(200);

		LogicalRespawn respawn = respawner().lstRespawns.get(0);
		LogicalCellItem item = componentOf(respawn.entity, LogicalCellItem.class);
		assertNotNull("a spawned piece must be placed into a grid cell", item.cell);
		assertSame(item.cellPosition.getContext(), respawn.respawningPosition.getContext());

		item.cellPosition.x.value = 100;
		item.cellPosition.y.value = 50;
		item.cellPosition.commit(null, item.entity);

		assertEquals(100.0, respawn.respawningPosition.getX(), 0.0001);
		assertEquals(50.0, respawn.respawningPosition.getY(), 0.0001);

		// HandlerRespawns.java:63-64 is commented out: the seam moves the target position
		// only, never the respawn's own position
		assertNotEquals(100.0, respawn.position.getX(), 0.0001);
	}

	@Test
	public void pins_theCellPositionSeamRequiresBothComponentsOnTheSameEntity() {

		parse(MAP);
		pumpUntilFilled(200);

		// piece1 carries only an <item>. HandlerRespawns walks the logicals of the SENDING entity
		// (:54-77), so a cellPosition commit from an entity that has no respawn moves nothing,
		// even though both fields share the merged "lposition" context
		LogicalCellItem piece = piece();
		piece.cellPosition.x.value = 77;
		piece.cellPosition.y.value = 77;
		piece.cellPosition.commit(null, piece.entity);

		LogicalRespawn respawn = respawner().lstRespawns.get(0);
		assertNotEquals(77.0, respawn.respawningPosition.getX(), 0.0001);
	}

	@Test
	public void triggerOnTheMergedContextTeleportsEveryLiveRespawn() {

		parse(MAP);
		pumpUntilFilled(200);

		LogicalRespawn respawn = respawner().lstRespawns.get(0);
		respawn.respawningPosition.set(7, 9, 0);

		Trigger roundOver = new Trigger();
		roundOver.setName("finishRound");
		roundOver.setContext(respawner().respawningTrigger.getContext());

		assertTrue(respawnsHandler().transferData(entityNamed("spawner"), roundOver));

		assertEquals(7.0, respawn.position.getX(), 0.0001);
		assertEquals(9.0, respawn.position.getY(), 0.0001);
	}

	@Test
	public void pins_respawnerTriggerFieldIsABoolSoTheSeamNeedsSomebodyElsesTrigger() {

		parse(MAP);
		pumpUntilFilled(200);

		LogicalRespawner respawner = respawner();
		LogicalRespawn respawn = respawner.lstRespawns.get(0);
		respawn.respawningPosition.set(7, 9, 0);
		respawn.position.set(-1, -1, 0);

		// LogicalRespawner.respawningTrigger is a Bool, while HandlerRespawns.java:81-82 only
		// accepts a Trigger, so committing the map's own trigger field changes nothing
		respawner.respawningTrigger.value = true;
		respawner.respawningTrigger.commit(null, entityNamed("spawner"));

		assertEquals(-1.0, respawn.position.getX(), 0.0001);
		// ... and that false answer unsubscribes the handler from that field for good
		assertTrue(respawner.respawningTrigger.lstLinkedHandlers.isEmpty());
	}

	@Test
	public void pins_theHandlerRefusesTheItemPositionButAcceptsTheRespawnOne() {

		parse(MAP);

		HandlerRespawns handler = respawnsHandler();
		Entity pieceEntity = entityNamed("piece1");

		// the handler only reacts to LogicalRespawn/LogicalRespawner reflections
		assertFalse(handler.transferData(pieceEntity, piece().cellPosition));
		assertTrue(handler.transferData(respawner().prototype,
				componentOf(respawner().prototype, LogicalRespawn.class).respawningPosition));
	}
}
