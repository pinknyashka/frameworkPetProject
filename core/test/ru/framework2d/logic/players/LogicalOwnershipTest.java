package ru.framework2d.logic.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Text;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link LogicalOwnership} defaults and the wiring the players handler needs before it can
 * recognise a field of this component at all.
 */
public class LogicalOwnershipTest {

	private HandlerPlayers players;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		players = new HandlerPlayers();
	}

	@After
	public void tearDown() {
		players = null;
		TestSupport.resetIntervalPhysics();
	}

	private LogicalOwnership create(Property... properties) {
		TagOptions options = new TagOptions(LogicalOwnership.SHORT_CLASS_NAME);
		for (Property property : properties) {
			options.lstPropertys.add(property);
		}
		return (LogicalOwnership) players.createComponent(new Entity("man"), options);
	}

	@Test
	public void defaultsOfABareOwnership() {
		LogicalOwnership owned = new LogicalOwnership();

		assertNull(owned.owner);
		assertEquals("", owned.ownerName.value);
		assertTrue(owned.isExist.value);
		assertTrue(owned.isControlled.value);
		assertFalse(owned.isCurrentlyControlled.value);
		assertFalse(owned.isOwnerDrawRound.value);
		assertFalse(owned.isOwnerWonGame.value);
		// only the factory path fills these lists
		assertTrue(owned.lstData.isEmpty());
		assertTrue(owned.lstLinkedHandlers.isEmpty());

		assertEquals("ownership", LogicalOwnership.SHORT_CLASS_NAME);
		assertEquals("ownership", owned.getShortClassName());
	}

	@Test
	public void aBareOwnershipIgnoresEveryProperty() {
		LogicalOwnership owned = new LogicalOwnership();

		// Component.java:187-194 matches against lstData only, and lstData is filled by
		// setContextToComponent, so a detached component silently drops every attribute
		owned.setPropertyData(new Property("isExist", new Bool(false)));
		owned.setPropertyData(new Property("ownerName", new Text("p1")));

		assertTrue(owned.isExist.value);
		assertEquals("", owned.ownerName.value);
	}

	@Test
	public void attributeValuesAreAppliedToTheNamedField() {
		LogicalOwnership owned = create(
				new Property("ownerName", new Text("p1")),
				new Property("isExist", new Bool(false)),
				new Property("isCurrentlyControlled", new Bool(true)));

		assertEquals("p1", owned.ownerName.value);
		assertFalse(owned.isExist.value);
		assertTrue(owned.isCurrentlyControlled.value);
		assertTrue(owned.isControlled.value);
	}

	@Test
	public void dataNameMatchingIsCaseSensitiveEvenThoughAttributeLookupIsNot() {
		// Property.isNamed is case insensitive, a data field name is matched exactly
		LogicalOwnership owned = create(
				new Property("OWNERNAME", new Text("p1")),
				new Property("ISEXIST", new Bool(false)));

		assertEquals("", owned.ownerName.value);
		assertTrue(owned.isExist.value);
	}

	@Test
	public void theFactoryNamesEveryDataFieldAndGivesItAContext() {
		LogicalOwnership owned = create();

		// six data fields of LogicalOwnership plus the position inherited from Component
		assertEquals(7, owned.lstData.size());
		assertTrue(owned.lstData.contains(owned.isExist));
		assertTrue(owned.lstData.contains(owned.ownerName));
		assertTrue(owned.lstData.contains(owned.position));

		assertEquals("isExist", owned.isExist.getName());
		assertEquals("ownerName", owned.ownerName.getName());
		assertEquals("position", owned.position.getName());

		assertNotNull(owned.isExist.getContext());
		// the field name is the context name until a map renames it
		assertEquals("isExist", owned.isExist.getContext().name);
		assertNotSame(owned.isExist.getContext(), owned.isCurrentlyControlled.getContext());
		assertTrue(owned.isExist.lstLinkedHandlers.contains(players));
	}
}
