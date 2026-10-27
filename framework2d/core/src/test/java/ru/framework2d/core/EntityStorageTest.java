package ru.framework2d.core;

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

import ru.framework2d.data.Text;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link EntityStorage}: the two registries, the current-entity nesting stack, and what
 * {@code createEntityByPrototype} does with parents and implementation counters.
 *
 * Registration is done through {@code createEntity}/{@code createPrototype} directly and, in one
 * test, through a real map parse, which is the only production caller of {@code createPrototype}.
 */
public class EntityStorageTest {

	private static final String MAP =
			"<map version = \"1.0\" name = \"storage test\">"
			+ "<entity name = \"outer\">"
			+ "<prototype name = \"proto\"><entity name = \"kid\"/></prototype>"
			+ "</entity>"
			+ "</map>";

	private EntityStorage storage;
	private EnginesManager enginesManager;

	@Before
	public void setUp() {
		TestSupport.installFakeGraphics();
		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
	}

	@After
	public void tearDown() {
		storage = null;
		enginesManager = null;
		TestSupport.clearFakeGraphics();
	}

	private TagOptions tag(String name, String entityName) {
		TagOptions options = new TagOptions(name);
		options.lstPropertys.add(new Property("name", new Text(entityName)));
		return options;
	}

	@Test
	public void createEntityRegistersAndNestsUnderTheCurrentEntity() {
		Entity first = storage.createEntity(tag("entity", "first"));
		Entity second = storage.createEntity(tag("entity", "second"));

		assertEquals(2, storage.lstEntities.size());
		assertSame(first, storage.lstEntities.get(0));
		assertSame(second, storage.lstEntities.get(1));

		assertSame(first, second.parent);
		assertSame(second, first.lstChildren.get(0));
		assertSame(second, storage.currentCreatedEntity);
		assertNull(first.parent);
	}

	@Test
	public void entityCreationIsOverWalksBackUpTheChainAndStopsAtNull() {
		storage.createEntity(tag("entity", "first"));
		storage.createEntity(tag("entity", "second"));

		storage.entityCreationIsOver();
		assertEquals("first", storage.currentCreatedEntity.name);

		storage.entityCreationIsOver();
		assertNull(storage.currentCreatedEntity);

		// EntityStorage.java:32-34: a further call is a no-op on an empty stack
		storage.entityCreationIsOver();
		assertNull(storage.currentCreatedEntity);
		assertEquals(2, storage.lstEntities.size());
	}

	@Test
	public void topLevelEntityHasNeitherParentNorChildren() {
		Entity only = storage.createEntity(tag("entity", "only"));

		assertNull(only.parent);
		assertTrue(only.lstChildren.isEmpty());
		assertFalse(only.isPrototype);
		assertTrue(storage.lstPrototypes.isEmpty());
	}

	@Test
	public void pins_prototypeTagOnCreateEntityMakesAnOrdinaryRegisteredEntity() {
		// Entity.java:61 marks it by tag name, but EntityStorage.java:15-18 only ever puts
		// the entity into lstEntities
		Entity entity = storage.createEntity(tag("prototype", "hidden"));

		assertTrue(entity.isPrototype);
		assertEquals(1, storage.lstEntities.size());
		assertTrue(storage.lstPrototypes.isEmpty());
	}

	@Test
	public void createPrototypeIsMarkedEvenUnderAnotherTagName() {
		// EntityStorage.java:41 forces the flag regardless of the tag
		Entity prototype = storage.createPrototype(tag("entity", "checker"));

		assertTrue(prototype.isPrototype);
		assertEquals("checker", prototype.name);
		assertEquals(1, storage.lstPrototypes.size());
		assertSame(prototype, storage.lstPrototypes.get(0));
	}

	@Test
	public void pins_prototypeIsNeitherAnEntityNorAChildOfItsParent() {
		Entity parent = storage.createEntity(tag("entity", "outer"));
		Entity prototype = storage.createPrototype(tag("prototype", "proto"));

		// EntityStorage.java:44-46 assigns the parent but never adds the prototype to it
		assertSame(parent, prototype.parent);
		assertTrue(parent.lstChildren.isEmpty());
		assertEquals(1, storage.lstEntities.size());
		assertFalse(storage.lstEntities.contains(prototype));
		assertSame(prototype, storage.currentCreatedEntity);
	}

	@Test
	public void entitiesCreatedUnderAPrototypeNestUnderIt() {
		storage.createPrototype(tag("prototype", "proto"));
		Entity kid = storage.createEntity(tag("entity", "kid"));

		assertEquals("proto", storage.currentCreatedEntity.parent.name);
		assertSame(kid, storage.lstEntities.get(0));
		assertEquals(0, kid.implementationsNum);
	}

	@Test
	public void createEntityByPrototypeCountsCopiesTheParentAndRegistersTheClone() {
		Entity parent = storage.createEntity(tag("entity", "outer"));
		Entity prototype = storage.createPrototype(tag("prototype", "checker"));
		prototype.addComponent(new EntityCloneTest.Brain());

		Entity clone = storage.createEntityByPrototype(prototype);

		assertEquals("checker1", clone.name);
		assertEquals(1, prototype.implementationsNum);
		assertFalse(clone.isPrototype);
		assertEquals(2, storage.lstEntities.size());
		assertSame(clone, storage.lstEntities.get(1));

		assertSame(parent, clone.parent);
		assertEquals(1, parent.lstChildren.size());
		assertSame(clone, parent.lstChildren.get(0));

		assertEquals(1, clone.lstComponents.size());
		assertNotSame(prototype.lstComponents.get(0), clone.lstComponents.get(0));
		// EntityStorage.java:53-64 never touches the nesting stack
		assertSame(prototype, storage.currentCreatedEntity);
	}

	@Test
	public void createEntityByPrototypeWithoutAParentLeavesTheCloneDetached() {
		Entity prototype = storage.createPrototype(tag("prototype", "checker"));

		Entity clone = storage.createEntityByPrototype(prototype);

		assertNull(clone.parent);
		assertEquals("checker1", clone.name);
		assertEquals(1, storage.lstEntities.size());
		assertTrue(storage.lstPrototypes.get(0).lstChildren.isEmpty());
	}

	@Test
	public void implementationCountersAreIndependentPerPrototype() {
		Entity white = storage.createPrototype(tag("prototype", "white"));
		Entity black = storage.createPrototype(tag("prototype", "black"));

		assertEquals("white1", storage.createEntityByPrototype(white).name);
		assertEquals("black1", storage.createEntityByPrototype(black).name);
		assertEquals("white2", storage.createEntityByPrototype(white).name);

		assertEquals(2, white.implementationsNum);
		assertEquals(1, black.implementationsNum);
		assertEquals(3, storage.lstEntities.size());
	}

	@Test
	public void pins_cloneOfACloneIsProducedAndCountsOnTheClone() {
		// EntityStorage.java:55 and Entity.java:64-68 check neither isPrototype nor the origin
		Entity prototype = storage.createPrototype(tag("prototype", "checker"));
		Entity clone = storage.createEntityByPrototype(prototype);

		Entity grandClone = storage.createEntityByPrototype(clone);

		assertEquals("checker11", grandClone.name);
		assertEquals(1, clone.implementationsNum);
		assertFalse(grandClone.isPrototype);
		// neither the prototype nor its clone source is an ordinary entity
		assertEquals(2, storage.lstEntities.size());
	}

	@Test
	public void pins_createEntityByPrototypeOfNullCrashesOnTheCounter() {
		try {
			storage.createEntityByPrototype(null);
			fail("Entity.java:67 dereferences the prototype before anything else");
		} catch (NullPointerException expected) {
			assertTrue(storage.lstEntities.isEmpty());
		}
	}

	@Test
	public void pins_registrationHasNoDuplicateGuardAndNoNameLookup() {
		// EntityStorage.java:7-9: two plain lists, no lookup, no contains() anywhere
		storage.createEntity(tag("entity", "dup"));
		storage.createEntity(tag("entity", "dup"));
		storage.createPrototype(tag("prototype", "proto"));
		storage.createPrototype(tag("prototype", "proto"));

		assertEquals(2, storage.lstEntities.size());
		assertEquals(2, storage.lstPrototypes.size());
		assertEquals(storage.lstPrototypes.get(0).name, storage.lstPrototypes.get(1).name);
		assertNotSame(storage.lstPrototypes.get(0), storage.lstPrototypes.get(1));
	}

	@Test
	public void theParserRegistersPrototypesSeparatelyFromEntities() {
		new MapParser(storage, enginesManager, null).parseMap(checkedParser());

		assertEquals(2, storage.lstEntities.size());
		assertEquals(1, storage.lstPrototypes.size());

		Entity prototype = storage.lstPrototypes.get(0);
		assertTrue(prototype.isPrototype);
		assertEquals("proto", prototype.name);
		assertFalse(storage.lstEntities.contains(prototype));

		Entity outer = storage.lstEntities.get(0);
		Entity kid = storage.lstEntities.get(1);
		assertSame(outer, prototype.parent);
		assertSame(prototype, kid.parent);
		assertSame(kid, prototype.lstChildren.get(0));
		assertTrue(outer.lstChildren.isEmpty());
		assertNull(storage.currentCreatedEntity);

		// the first clone is adopted by the prototype parent, not by the prototype itself
		Entity clone = storage.createEntityByPrototype(prototype);
		assertEquals("proto1", clone.name);
		assertSame(outer, clone.parent);
		assertEquals(1, outer.lstChildren.size());
		assertSame(clone, outer.lstChildren.get(0));
		assertNotNull(storage.lstEntities.get(2));
	}

	private org.xmlpull.v1.XmlPullParser checkedParser() {
		try {
			return TestSupport.xmlParser(MAP);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
