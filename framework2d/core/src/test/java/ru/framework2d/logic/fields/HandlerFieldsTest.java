package ru.framework2d.logic.fields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Component;
import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;
import ru.framework2d.testsupport.TestSupport;

/**
 * Headless tests for the field/unit logic (HandlerFields, LogicalField, LogicalUnit).
 * Components are created through the real creation path
 * (ComponentsHandler.createComponent) so factory, contexts, attributes and
 * registration all run exactly as during map parsing.
 */
public class HandlerFieldsTest {

	private HandlerFields handler;

	@Before
	public void setUp() {

		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();

		handler = new HandlerFields();
		// HandlerFields.iterate() ends with masterEngine.subEngineWorkDone(this),
		// so a stand-alone handler needs a throwaway master engine.
		handler.masterEngine = new TestSupport.PlainEngine();
	}

	private static class PlainLogical extends LogicalInterface { }

	private static Position3 pos(double x, double y) {

		Position3 position = new Position3();
		position.x.value = x;
		position.y.value = y;
		return position;
	}

	private LogicalField createField(Entity entity, String name, double centerX, double centerY, double size) {

		TagOptions options = new TagOptions("field");
		options.lstPropertys.add(new Property("name", new Text(name)));
		options.lstPropertys.add(new Property("position:x", new Fractional((float) centerX)));
		options.lstPropertys.add(new Property("position:y", new Fractional((float) centerY)));
		options.lstPropertys.add(new Property("size:x", new Fractional((float) size)));
		options.lstPropertys.add(new Property("size:y", new Fractional((float) size)));

		return (LogicalField) handler.createComponent(entity, options);
	}

	private LogicalUnit createUnit(Entity entity, String fieldName, double x, double y) {

		TagOptions options = new TagOptions("unit");
		options.lstPropertys.add(new Property("fieldName", new Text(fieldName)));
		options.lstPropertys.add(new Property("position:x", new Fractional((float) x)));
		options.lstPropertys.add(new Property("position:y", new Fractional((float) y)));

		return (LogicalUnit) handler.createComponent(entity, options);
	}

	// ------------------------------------------------------------------- creation

	@Test
	public void handlerAnswersFieldAndUnitTags() {

		assertTrue(handler.canCreateThisComponent("field"));
		assertTrue(handler.canCreateThisComponent("LogicalField"));
		assertTrue(handler.canCreateThisComponent("unit"));
		assertTrue(handler.canCreateThisComponent("LogicalUnit"));

		// tag matching is contentEquals -> case sensitive (ComponentsHandler:193-201)
		assertFalse(handler.canCreateThisComponent("Field"));
		assertFalse(handler.canCreateThisComponent("grid"));
	}

	@Test
	public void radiusPropertyTurnsTheFieldIntoACircleAndOverwritesItsSize() {

		Entity entity = new Entity("field1");
		TagOptions options = new TagOptions("field");
		options.lstPropertys.add(new Property("size:x", new Fractional(100)));
		options.lstPropertys.add(new Property("size:y", new Fractional(100)));
		options.lstPropertys.add(new Property("radius", new Fractional(10)));

		LogicalField field = (LogicalField) handler.createComponent(entity, options);

		assertTrue(field.isCircle.value);
		assertEquals(10f, field.radius.value, 0.0001f);

		// LogicalField.setPropertyData runs the radius branch after super, so inside
		// one tag radius always wins over an explicit size (:45-51)
		assertEquals(20.0, field.size.getX(), 0.0001);
		assertEquals(20.0, field.size.getY(), 0.0001);
	}

	@Test
	public void fieldNamePropertyAccumulatesTheCheckListButKeepsOnlyTheLastValue() {

		LogicalUnit unit = new LogicalUnit();
		unit.setPropertyData(new Property("fieldName", new Text("board")));
		unit.setPropertyData(new Property("fieldName", new Text("yard")));

		assertEquals(2, unit.lstFieldsNameToCheck.size());
		assertEquals("board", unit.lstFieldsNameToCheck.get(0));
		assertEquals("yard", unit.lstFieldsNameToCheck.get(1));
		assertEquals("yard", unit.fieldName.value);
	}

	@Test
	public void pins_detachedComponentIgnoresEveryAttributeButItsExplicitOverrides() {

		LogicalField field = new LogicalField();
		field.setPropertyData(new Property("size:x", new Fractional(100)));
		field.setPropertyData(new Property("isEnabled", new Bool(false)));

		// Component.setPropertyData only walks lstData, which stays empty until
		// setContextToComponent has run (ComponentsHandler:328-379)
		assertEquals(0.0, field.size.getX(), 0.0001);
		assertTrue(field.isEnabled.value);

		// LogicalField has an explicit radius branch, so it works even detached
		field.setPropertyData(new Property("radius", new Fractional(5)));
		assertEquals(10.0, field.size.getX(), 0.0001);
	}

	// ------------------------------------------------------------------- geometry

	@Test
	public void rectFieldTreatsItsOwnBorderAsOutside() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);

		assertFalse(field.checkEscape(pos(0, 0)));
		assertFalse(field.checkEscape(pos(49, 0)));

		// Point2.checkField is strict (:93-97): a point exactly on the border counts
		// as escaped
		assertTrue(field.checkEscape(pos(50, 0)));
		assertTrue(field.checkEscape(pos(0, 50)));
	}

	@Test
	public void pins_rectFieldBoundsAreTruncatedToInt() {

		LogicalField field = createField(new Entity("field1"), "board", 0.5, 0, 100);

		// (int) (0.5 - 50) == -49, so the left bound is -49 and not -49.5
		assertFalse(field.checkEscape(pos(-48, 0)));
		assertTrue(field.checkEscape(pos(-49, 0)));
	}

	@Test
	public void pins_circleFieldEscapeCheckIsInverted() {

		LogicalField field = new LogicalField();
		field.setPropertyData(new Property("radius", new Fractional(10)));

		// LogicalField:55-56 returns dist < radius, the opposite of the rectangle
		// branch: being inside the circle is reported as "escaped"
		assertTrue(field.checkEscape(pos(0, 0)));
		assertFalse(field.checkEscape(pos(50, 0)));
	}

	@Test
	public void pins_circleEscapeCheckIgnoresTheThirdCoordinate() {

		LogicalField field = new LogicalField();
		field.setPropertyData(new Property("radius", new Fractional(10)));
		field.position.z.value = 1000;

		// Point3.dist(Point3) is package-private, so from this package the public
		// Point2.dist(Point2) overload is the only applicable one (:54-56)
		assertTrue(field.checkEscape(pos(0, 0)));
	}

	// ------------------------------------------------------------------- transfers

	@Test
	public void positionTransferMovesEveryLogicalOfTheEntityAndQueuesItOnce() {

		Entity entity = new Entity("checker");
		LogicalUnit unit = createUnit(entity, "board", 0, 0);

		assertTrue(handler.transferData(entity, pos(12, 7)));
		assertEquals(1, handler.lstMovedObjects.size());
		assertSame(entity, handler.lstMovedObjects.get(0));
		assertEquals(12.0, unit.position.getX(), 0.0001);
		assertEquals(7.0, unit.position.getY(), 0.0001);
		assertTrue(handler.haveWork());

		handler.transferData(entity, pos(13, 8));
		assertEquals(1, handler.lstMovedObjects.size());
	}

	@Test
	public void pins_nonPositionTransferIsRejectedAndUnlinksTheHandlerFromThatData() {

		Entity entity = new Entity("field1");
		LogicalField field = createField(entity, "board", 0, 0, 100);

		assertFalse(handler.transferData(entity, new Text("hello")));

		// setContextToComponent subscribes the handler through the context and
		// connectComponent subscribes it again (ComponentsHandler:215-219)
		assertEquals(2, field.name.lstLinkedHandlers.size());

		field.name.value = "renamed";
		field.name.commit(handler, entity);			// the sender itself is skipped
		assertEquals(2, field.name.lstLinkedHandlers.size());

		// one rejected commit drops every occurrence (DataInterface:130-137)
		field.name.commit(null, entity);
		assertTrue(field.name.lstLinkedHandlers.isEmpty());
	}

	@Test
	public void entityWithoutUnitOrFieldIsNotQueued() {

		Entity entity = new Entity("plain");
		entity.lstLogicals.add(new PlainLogical());

		assertFalse(handler.transferData(entity, pos(1, 1)));
		assertTrue(handler.lstMovedObjects.isEmpty());
		assertFalse(handler.haveWork());
	}

	// ---------------------------------------------------------------------- moving

	@Test
	public void unitEnteringFieldSetsInFieldAndGotUnitInField() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 70, 0);

		unit.inField.value = false;
		handler.transferData(unitEntity, pos(0, 0));
		handler.iterate();

		assertTrue(unit.inField.value);
		assertTrue(field.lstUnitsInField.contains(unit));
		assertTrue(field.gotUnitInField.value);
		assertTrue(field.gotMovingUnits.value);
		assertTrue(handler.lstMovedObjects.isEmpty());
	}

	@Test
	public void unitLeavingFieldClearsTheMembershipAndTheTrigger() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 0, 0);

		unit.inField.value = false;
		handler.transferData(unitEntity, pos(0, 0));
		handler.iterate();
		assertTrue(field.gotUnitInField.value);

		handler.transferData(unitEntity, pos(70, 0));
		handler.iterate();

		assertFalse(unit.inField.value);
		assertTrue(field.lstUnitsInField.isEmpty());
		assertFalse(field.gotUnitInField.value);
	}

	@Test
	public void pins_unitThatStartsInsideAFieldIsNeverRegisteredInIt() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 0, 0);

		handler.transferData(unitEntity, pos(0, 0));
		handler.iterate();

		// LogicalUnit.inField defaults to true (:23) and HandlerFields:73 only reacts
		// to a change, so an already contained unit is never registered in the field
		assertTrue(unit.inField.value);
		assertTrue(field.lstUnitsInField.isEmpty());
		assertFalse(field.gotUnitInField.value);
	}

	@Test
	public void pins_disabledFieldIsSkippedForUnitMotionButNotForItsOwnMotion() {

		Entity fieldEntity = new Entity("field1");
		LogicalField field = createField(fieldEntity, "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 0, 0);

		field.isEnabled.value = false;
		unit.inField.value = false;

		handler.transferData(unitEntity, pos(0, 0));
		handler.iterate();
		assertFalse(unit.inField.value);			// :66 honours isEnabled

		handler.transferData(fieldEntity, pos(0, 0));
		handler.iterate();
		assertTrue(unit.inField.value);			// :101-126 never checks isEnabled
	}

	@Test
	public void pins_leavingWithoutEverBeingInsideLeavesTheTriggerUntouched() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 0, 0);

		unit.inField.value = true;
		handler.transferData(unitEntity, pos(70, 0));
		handler.iterate();

		// lstUnitsInField.remove(unit) is false when the unit was never inside, so
		// gotUnitInField is not re-committed (:80-83)
		assertFalse(unit.inField.value);
		assertTrue(field.lstUnitsInField.isEmpty());
		assertFalse(field.gotUnitInField.value);
	}

	@Test
	public void gotMovingUnitsFallsBackToFalseWhenNothingMovesAnymore() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		createUnit(unitEntity, "board", 0, 0);

		handler.transferData(unitEntity, pos(0, 0));
		handler.iterate();
		assertTrue(field.gotMovingUnits.value);
		assertTrue(handler.haveWork());

		handler.iterate();
		assertFalse(field.gotMovingUnits.value);
		assertFalse(handler.haveWork());
	}

	// ---------------------------------------------------------------- registration

	@Test
	public void unitAndFieldAreLinkedByNameInAnyRegistrationOrder() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 0, 0);

		assertTrue(field.lstUnits.contains(unit));
		assertTrue(unit.lstFieldsToCheck.contains(field));

		createField(new Entity("field2"), "yard", 0, 0, 10);
		LogicalUnit secondUnit = createUnit(new Entity("checker2"), "yard", 0, 0);

		assertEquals(2, handler.lstFields.size());
		assertEquals(2, handler.lstUnits.size());
		assertFalse(secondUnit.lstFieldsToCheck.contains(field));
	}

	@Test
	public void pins_unitIsLinkedOnlyThroughTheFirstUnitOfItsEntity() {

		Entity shared = new Entity("two_units");
		LogicalUnit first = createUnit(shared, "board", 0, 0);

		TagOptions options = new TagOptions("unit");
		options.lstPropertys.add(new Property("fieldName", new Text("board")));
		Component created = handler.createComponent(shared, options);

		// :228-270: links of every unit of one entity are written into the FIRST unit,
		// and the returned component is that first unit
		assertSame(first, created);
		assertEquals(1, shared.lstLogicals.size());
		assertSame(first, shared.lstLogicals.get(0));
	}

	@Test
	public void pins_secondUnitRegistrationDuplicatesTheFirstUnitInTheUnitList() {

		createField(new Entity("field1"), "board", 0, 0, 100);
		Entity shared = new Entity("two_units");
		LogicalUnit first = createUnit(shared, "board", 0, 0);
		handler.createComponent(shared, new TagOptions("unit"));

		// the guard checks `component` (:224) while the list gets `firstUnit` (:269)
		assertEquals(2, handler.lstUnits.size());
		assertSame(first, handler.lstUnits.get(0));
		assertSame(first, handler.lstUnits.get(1));
	}

	@Test
	public void pins_detachedUnitIsNotRegisteredWhileADetachedFieldIs() {

		handler.connectComponent(new LogicalField());
		assertEquals(1, handler.lstFields.size());

		handler.connectComponent(new LogicalUnit());
		assertTrue(handler.lstUnits.isEmpty());		// :226 requires component.entity
	}

	@Test
	public void pins_registrationDropsTheSecondUnitOfTheSameEntity() {

		Entity shared = new Entity("two_units");
		createUnit(shared, "board", 0, 0);

		LogicalUnit second = new LogicalUnit();
		second.entity = shared;
		shared.addComponent(second);

		handler.connectComponent(second);

		assertEquals(1, shared.lstLogicals.size());
		assertFalse(shared.lstComponents.contains(second));
	}

	@Test
	public void pins_fieldAndUnitAreLinkedOnlyWhenNamesMatchCaseSensitively() {

		LogicalField field = createField(new Entity("field1"), "Board", 0, 0, 100);
		LogicalUnit unit = createUnit(new Entity("checker"), "board", 0, 0);

		// name matching uses String.contentEquals (:206, :244, :256)
		assertTrue(field.lstUnits.isEmpty());
		assertTrue(unit.lstFieldsToCheck.isEmpty());
	}

	@Test
	public void pins_lstFieldsWhereIAmIsDeadCode() {

		LogicalField field = createField(new Entity("field1"), "board", 0, 0, 100);
		Entity unitEntity = new Entity("checker");
		LogicalUnit unit = createUnit(unitEntity, "board", 0, 0);

		unit.inField.value = false;
		handler.transferData(unitEntity, pos(0, 0));
		handler.iterate();

		assertTrue(field.lstUnitsInField.contains(unit));
		assertTrue(unit.lstFieldsWhereIAm.isEmpty());	// never written by the engine
	}
}
