package ru.framework2d.logic.respawns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.DataContext;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.data.Trigger;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link HandlerRespawns} registration, the prototype top up of {@code iterate()} and the
 * teleport path of {@code transferData}.
 *
 * Prototypes and clones go through the real cloning path ({@code Entity(Entity prototype)}
 * called by {@code EntityStorage.createEntityByPrototype}), so the way a clone re-enters the
 * handler lists is exercised too.
 *
 * No main source was changed, so every odd behaviour below is asserted exactly as it is.
 */
public class HandlerRespawnsTest {

	private EntityStorage storage;
	private HandlerRespawns respawns;
	private TestSupport.PlainEngine master;

	@Before
	public void setUp() {
		storage = TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		respawns = new HandlerRespawns();
		master = new TestSupport.PlainEngine();
		// iterate() ends by handing the tick back to masterEngine (HandlerRespawns.java:43)
		master.addSubengine(respawns);
	}

	@After
	public void tearDown() {
		storage = null;
		respawns = null;
		master = null;
		TestSupport.resetIntervalPhysics();
	}

	private TagOptions tagOptions(String tag, Property... properties) {
		TagOptions options = new TagOptions(tag);
		for (Property property : properties) {
			options.lstPropertys.add(property);
		}
		return options;
	}

	private LogicalRespawner respawner(ComponentsHandler handler, String name, int number) {
		return (LogicalRespawner) handler.createComponent(new Entity(name),
				tagOptions(LogicalRespawner.SHORT_CLASS_NAME,
						new Property("name", new Text(name)),
						new Property("number", new Numeral(number))));
	}

	private LogicalRespawner respawner(String name, int number) {
		return respawner(respawns, name, number);
	}

	private LogicalRespawn respawn(ComponentsHandler handler, String entityName, String respawnerName) {
		return (LogicalRespawn) handler.createComponent(new Entity(entityName),
				tagOptions(LogicalRespawn.SHORT_CLASS_NAME,
						new Property("respawnerName", new Text(respawnerName))));
	}

	private LogicalRespawn respawn(String entityName, String respawnerName) {
		return respawn(respawns, entityName, respawnerName);
	}

	private Entity prototype(ComponentsHandler handler, String name, String respawnerName) {
		Entity proto = new Entity(tagOptions("prototype", new Property("name", new Text(name))));
		handler.createComponent(proto, tagOptions(LogicalRespawn.SHORT_CLASS_NAME,
				new Property("respawnerName", new Text(respawnerName))));
		return proto;
	}

	private Entity prototype(String name, String respawnerName) {
		return prototype(respawns, name, respawnerName);
	}

	private <T extends DataInterface> T signal(T value, DataInterface field) {
		value.setContext(field.getContext());
		return value;
	}

	@Test
	public void registeringARespawnAndARespawnerQueuesWork() {
		LogicalRespawner white = respawner("white", 3);
		assertEquals(1, respawns.lstRespawners.size());
		assertTrue(respawns.haveWork());

		LogicalRespawn item = respawn("checker", "");
		assertEquals(1, respawns.lstRespawns.size());
		// an empty respawnerName never enters the name matching block (:116)
		assertNull(item.respawner);
		assertTrue(white.lstRespawns.isEmpty());
		assertNull(white.prototype);
	}

	@Test
	public void startWorkIteratesOnlyWhileWorkIsQueued() {
		respawner("white", 3);

		assertTrue(respawns.startWork(0));
		assertFalse(respawns.haveWork());
		assertEquals(1, master.subDoneCount);

		// iterate() cleared the work flag, so a second start is refused (:31-43)
		assertFalse(respawns.startWork(0));
		assertTrue(storage.lstEntities.isEmpty());
	}

	@Test
	public void respawnIsLinkedToTheRespawnerByName() {
		LogicalRespawner white = respawner("white", 1);
		LogicalRespawn item = respawn("checker", "white");

		assertSame(white, item.respawner);
		assertEquals(1, white.lstRespawns.size());
		assertSame(item, white.lstRespawns.get(0));
		assertEquals("white", item.respawnerName.value);

		// connecting the same components again changes nothing
		respawns.connectComponent(item);
		respawns.connectComponent(white);
		assertEquals(1, respawns.lstRespawns.size());
		assertEquals(1, respawns.lstRespawners.size());
		assertEquals(1, white.lstRespawns.size());
	}

	@Test
	public void respawnParsedBeforeTheRespawnerIsLinkedByTheBackFillPass() {
		// the back fill of :144-164 covers a respawner created after its respawns
		LogicalRespawn item = respawn("checker", "white");
		assertNull(item.respawner);

		LogicalRespawner white = respawner("white", 1);

		assertSame(white, item.respawner);
		assertEquals(1, white.lstRespawns.size());
	}

	@Test
	public void respawnWithAnUnknownRespawnerNameStaysUnlinked() {
		LogicalRespawn item = respawn("checker", "black");
		LogicalRespawner white = respawner("white", 1);

		assertNull(item.respawner);
		assertTrue(white.lstRespawns.isEmpty());
	}

	@Test
	public void detachedComponentWithoutAnEntityThrowsNullPointerException() {
		// HandlerRespawns.java:104 dereferences component.entity with no guard
		try {
			respawns.connectComponent(new LogicalRespawn());
			fail("a detached LogicalRespawn must not be connectable");
		} catch (NullPointerException expected) {
			// the handler cannot tell a prototype from an implementation without the entity
		}

		try {
			respawns.connectComponent(new LogicalRespawner());
			fail("a detached LogicalRespawner must not be connectable");
		} catch (NullPointerException expected) {
			// the same dereference on the respawner side
		}
	}

	@Test
	public void prototypeIsRegisteredAndAdoptedByTheRespawner() {
		LogicalRespawner white = respawner("white", 3);
		Entity proto = prototype("checker", "white");

		assertEquals(1, respawns.lstPrototypes.size());
		assertSame(proto, respawns.lstPrototypes.get(0));
		assertSame(proto, white.prototype);
		// a prototype respawn is never an implementation of its respawner
		assertTrue(respawns.lstRespawns.isEmpty());
		assertTrue(white.lstRespawns.isEmpty());
	}

	@Test
	public void prototypeParsedBeforeItsRespawnerNeverGetsATopUp() {
		// the back fill pass repairs respawner links only, never the prototype (:168-186)
		prototype("checker", "white");
		assertEquals(1, respawns.lstPrototypes.size());

		LogicalRespawner white = respawner("white", 3);

		assertNull(white.prototype);
		respawns.iterate();
		assertEquals(0, respawns.lstRespawns.size());
		assertTrue(storage.lstEntities.isEmpty());
		assertFalse(respawns.haveWork());
	}

	@Test
	public void iterateTopsUpClonesUntilTheNumberIsReached() {
		LogicalRespawner white = respawner("white", 3);
		Entity proto = prototype("checker", "white");
		assertEquals(0, white.lstRespawns.size());

		respawns.iterate();

		assertEquals(3, white.lstRespawns.size());
		assertEquals(3, respawns.lstRespawns.size());
		assertEquals(3, storage.lstEntities.size());
		assertFalse(respawns.haveWork());
		assertEquals(1, master.subDoneCount);

		// a clone is a real entity, not a prototype, and it re-registers itself into the
		// respawner through enginesManager.loadEntity inside the top up loop (:35-36)
		LogicalRespawn first = white.lstRespawns.get(0);
		assertFalse(first.entity.isPrototype);
		assertEquals("checker1", first.entity.name);
		assertEquals("white", first.respawnerName.value);
		assertSame(white, first.respawner);
		assertSame(first, respawns.lstRespawns.get(0));
		assertNotSame(proto, first.entity);

		// the top up is idempotent: nothing grows once the number is satisfied
		respawns.iterate();
		assertEquals(3, white.lstRespawns.size());
		assertEquals(3, respawns.lstRespawns.size());
		assertEquals(3, storage.lstEntities.size());
	}

	@Test
	public void iterateCrashesAfterTheTopUpWhenTheHandlerHasNoMasterEngine() {
		HandlerRespawns orphan = new HandlerRespawns();
		assertNull(orphan.masterEngine);
		LogicalRespawner white = respawner(orphan, "white", 2);
		prototype(orphan, "checker", "white");

		try {
			orphan.iterate();
			fail("HandlerRespawns.java:43 dereferences masterEngine without a guard");
		} catch (NullPointerException expected) {
			// the clones and the work flag were already produced above the crash (:34-41)
			assertEquals(2, white.lstRespawns.size());
			assertFalse(orphan.haveWork());
		}

		// once a master engine exists the loop does not top up again: the clones of the
		// crashed pass are still registered, so the number is already satisfied
		TestSupport.PlainEngine owner = new TestSupport.PlainEngine();
		owner.addSubengine(orphan);
		orphan.iterate();
		assertEquals(2, white.lstRespawns.size());
		assertEquals(2, storage.lstEntities.size());
	}

	@Test
	public void respawnPositionTransferMovesTheTargetAndNotTheComponent() {
		LogicalRespawn item = respawn("checker", "");

		assertTrue(respawns.transferData(item.entity,
				signal(new Position3(10, 20, 0), item.respawningPosition)));
		assertEquals(10d, item.respawningPosition.getX(), 0d);
		assertEquals(20d, item.respawningPosition.getY(), 0d);
		// the assignment that would move the component is commented out (:63-64)
		assertEquals(0d, item.position.getX(), 0d);

		// the same vector under the component position context is not ours
		assertFalse(respawns.transferData(item.entity,
				signal(new Position3(50, 60, 0), item.position)));
		assertEquals(10d, item.respawningPosition.getX(), 0d);
		assertEquals(0d, item.position.getX(), 0d);
	}

	@Test
	public void triggerOnTheRespawnTeleportsItToTheTarget() {
		LogicalRespawn item = respawn("checker", "");
		item.respawningPosition.set(7, 9, 0);
		item.position.set(1, 1, 0);

		assertTrue(respawns.transferData(item.entity, signal(new Trigger(), item.respawningTrigger)));
		assertEquals(7d, item.position.getX(), 0d);
		assertEquals(9d, item.position.getY(), 0d);
	}

	@Test
	public void respawnerTriggerTeleportsEveryOwnedRespawn() {
		LogicalRespawner white = respawner("white", 0);
		LogicalRespawn first = respawn("c1", "white");
		LogicalRespawn second = respawn("c2", "white");

		// both components share one respawningTrigger context, so a single aliased trigger
		// lands in the respawn branch and the respawner branch at once
		assertSame(first.respawningTrigger.getContext(), white.respawningTrigger.getContext());

		first.respawningPosition.set(1, 2, 0);
		second.respawningPosition.set(3, 4, 0);

		assertTrue(respawns.transferData(white.entity, signal(new Trigger(), white.respawningTrigger)));
		assertEquals(1d, first.position.getX(), 0d);
		assertEquals(2d, first.position.getY(), 0d);
		assertEquals(3d, second.position.getX(), 0d);
	}

	@Test
	public void boolDataNeverFiresTheRespawnTrigger() {
		// the respawningTrigger fields are Bool, while the handler matches a Trigger by type
		LogicalRespawner white = respawner("white", 0);
		LogicalRespawn item = respawn("c1", "white");
		item.respawningPosition.set(5, 6, 0);

		assertFalse(respawns.transferData(white.entity, signal(new Bool(true), white.respawningTrigger)));
		assertFalse(respawns.transferData(item.entity, signal(new Bool(true), item.respawningTrigger)));
		assertEquals(0d, item.position.getX(), 0d);
	}

	@Test
	public void unrelatedDataCommitUnsubscribesTheHandler() {
		LogicalRespawner white = respawner("white", 1);
		assertTrue(white.number.lstLinkedHandlers.contains(respawns));

		// there is no Numeral branch, and DataInterface.java:134-137 drops every handler
		// that answers a commit with false
		white.number.commit(null, white.entity);

		assertFalse(white.number.lstLinkedHandlers.contains(respawns));
	}

	@Test
	public void transferDataRequiresAContextAndAnEntity() {
		LogicalRespawn item = respawn("checker", "");

		try {
			respawns.transferData(item.entity, new Trigger());
			fail("a data without a context must not reach the handler");
		} catch (NullPointerException expected) {
			// HandlerRespawns.java:48-49 dereferences data.getContext() to build the log line
		}

		try {
			respawns.transferData(null, signal(new Trigger(), item.respawningTrigger));
			fail("a null entity must not reach the handler");
		} catch (NullPointerException expected) {
			// the same log line dereferences object.name
		}
	}

	@Test
	public void sharedPositionContextLetsARespawnTriggerMoveAnOwnership() {
		// the logic branch of the shipped map wires these two handlers side by side
		// (assets/xml/checkers_tree.xml:71-91)
		EntityStorage local = TestSupport.bootstrapEngine();
		EnginesManager enginesManager = new EnginesManager(local, Engine.getResources());

		enginesManager.connectEngine(handlerOptions("ru.framework2d.logic.players", "HandlerPlayers"));
		enginesManager.engineCreationIsOver();
		enginesManager.connectEngine(handlerOptions("ru.framework2d.logic.respawns", "HandlerRespawns"));
		enginesManager.engineCreationIsOver();

		assertEquals(2, enginesManager.lstHandlers.size());
		ComponentsHandler playersHandler = enginesManager.lstHandlers.get(0);
		ComponentsHandler respawnHandler = enginesManager.lstHandlers.get(1);

		Entity piece = new Entity("piece");
		LogicalOwnership owned = (LogicalOwnership) enginesManager.createComponentInEntity(piece,
				tagOptions("ownership", new Property("ownerName", new Text("p1"))));
		LogicalRespawn item = (LogicalRespawn) enginesManager.createComponentInEntity(piece,
				tagOptions("l_respawn", new Property("respawnerName", new Text(""))));
		assertNotNull(owned);
		assertNotNull(item);

		// both handlers declare a position data, so engineCreationIsOver merged them into one
		// DataContext and subscribed the components of each other to that field
		DataContext shared = owned.position.getContext();
		assertSame(shared, item.position.getContext());
		assertTrue(item.position.lstLinkedHandlers.contains(playersHandler));

		item.respawningPosition.set(12, 34, 0);
		item.position.set(0, 0, 0);
		owned.position.set(9, 9, 0);

		Trigger finishRound = signal(new Trigger(), item.respawningTrigger);
		// stands for the shared finishRound context of the map (checkers_tree.xml:74 and :89)
		finishRound.lstLinkedHandlers.add(respawnHandler);
		finishRound.commit(null, piece);

		assertEquals(12d, item.position.getX(), 0d);
		// HandlerPlayers.java:47-49 moved the ownership of the same entity
		assertEquals(12d, owned.position.getX(), 0d);
		assertEquals(34d, owned.position.getY(), 0d);
	}

	private TagOptions handlerOptions(String packageName, String engineName) {
		return tagOptions("handler",
				new Property("package", new Text(packageName)),
				new Property("engineName", new Text(engineName)));
	}
}
