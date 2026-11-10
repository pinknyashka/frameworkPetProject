package ru.framework2d.touches.areas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.touches.TouchTestSupport;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link HandlerTouchSelect} owns the {@link TouchableArea} lists and the two entity-level
 * transfers (position and clickability). It declares no components itself: the area types come
 * from its sub handlers, exactly like {@code <sub engineName="SubPointer">} in the shipped map.
 */
public class HandlerTouchSelectTest {

	private HandlerTouchSelect select;
	private SubPointer sub;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		select = new HandlerTouchSelect();
		sub = new SubPointer();
		select.addSubengine(sub);
		// SubPointer never registers the base area type, so plain areas need their own sub handler
		TouchTestSupport.under(select, new TouchTestSupport.BaseAreas());
	}

	@After
	public void tearDown() {
		sub = null;
		select = null;
	}

	private TouchableArea area(String name, String tag, Property... properties) {
		TagOptions options = new TagOptions(tag);
		for (Property property : properties) {
			options.lstPropertys.add(property);
		}
		return (TouchableArea) select.createComponent(new Entity(name), options);
	}

	@Test
	public void pins_subPointerDeclaresTwoClassesButRegistersOnlyTheFirstOne() {
		SubPointer only = new SubPointer();
		HandlerTouchSelect bare = new HandlerTouchSelect();
		assertEquals(0, bare.lstProcessingComponents.size());
		assertEquals(EngineResource.TOUCH, bare.scopeResource());
		assertEquals(EngineResource.TOUCH, sub.scopeResource());

		// SubPointer.java:19 passes processingNum 1 together with two component classes, so
		// TouchableArea only lands in lstUsingComponents and is neither creatable nor contextual
		assertEquals(1, only.lstProcessingComponents.size());
		assertEquals(2, only.lstUsingComponents.size());
		assertSame(WorldPointer.class, only.lstProcessingComponents.get(0));

		TouchTestSupport.under(bare, only);
		assertTrue(bare.canCreateThisComponent("pointer"));
		assertFalse(bare.canCreateThisComponent("TouchableArea"));
		assertFalse(bare.canCreateThisComponent(TouchableArea.SHORT_CLASS_NAME));
		assertNull(bare.createComponent(new Entity("plain"), new TagOptions(TouchableArea.SHORT_CLASS_NAME)));

		TouchTestSupport.under(bare, new TouchTestSupport.BaseAreas());
		assertTrue(bare.canCreateThisComponent(TouchableArea.SHORT_CLASS_NAME));
		TouchableArea created = (TouchableArea) bare.createComponent(
				new Entity("plain"), new TagOptions(TouchableArea.SHORT_CLASS_NAME));
		assertNotNull("the late handler made the plain area creatable", created);
		assertTrue(bare.lstTouchableAreas.contains(created));
	}

	@Test
	public void pins_theContextListIsFrozenAfterTheFirstNonEmptyBuildContext() {
		HandlerTouchSelect bare = new HandlerTouchSelect();
		TouchTestSupport.under(bare, new SubPointer());
		WorldPointer pointer = (WorldPointer) bare.createComponent(
				new Entity("ptr"), new TagOptions(WorldPointer.SHORT_CLASS_NAME));

		// ComponentsHandler.java:251 only rebuilds the context list while it is still empty, so the
		// first successful getContexts() freezes it and a component type registered afterwards keeps
		// null contexts: it can never reach the pointer through the data system
		TouchTestSupport.under(bare, new TouchTestSupport.BaseAreas());
		TouchableArea created = (TouchableArea) bare.createComponent(
				new Entity("plain"), new TagOptions(TouchableArea.SHORT_CLASS_NAME));

		assertNotNull("the pointer was registered before the first build", pointer.isTouches.getContext());
		assertNotNull(created);
		assertNull("the late handler never got its data contexts", created.isClikable.getContext());
		assertNull(created.isSelected.getContext());
	}

	@Test
	public void createdAreaIsRegisteredInBothAreaListsAndGetsAFreshPointerList() {
		TouchableArea area = area("button", TouchableArea.SHORT_CLASS_NAME);

		assertNotNull(area);
		assertSame(area, area.entity.lstTouches.get(0));
		assertTrue(select.lstTouchableAreas.contains(area));
		assertTrue("isClikable defaults to true", select.lstActiveTouchableAreas.contains(area));
		assertEquals("SubPointer.connectComponent reset the list", 0, area.lstPointers.size());
		assertEquals("TouchableArea", area.getName());
		assertFalse(area.isSelected.value);
	}

	@Test
	public void radiusPropertyMakesTheAreaRoundAndSizesItsBox() {
		TouchableArea area = area("round", TouchableArea.SHORT_CLASS_NAME,
				new Property("radius", new Fractional(50f)));

		assertTrue(area.isCircle.value);
		assertEquals(50f, area.radius.value, 0.001f);
		assertEquals(100d, area.size.getX(), 0.001);
		assertEquals(100d, area.size.getY(), 0.001);

		area.position.set(100, 100, 0);
		assertTrue(area.touched(new Point2(130, 100)));
		assertEquals(30d, area.dragPoint.getX(), 0.001);
		assertFalse("the rim itself is exclusive", area.touched(new Point2(150, 100)));
	}

	@Test
	public void pins_areaWithoutSizeOrRadiusCanNeverBeTouchedAndTheRectBorderIsExclusive() {
		TouchableArea nothing = area("nothing", TouchableArea.SHORT_CLASS_NAME);
		nothing.position.set(100, 100, 0);
		assertTrue(nothing.isClikable.value);
		assertFalse(nothing.isCircle.value);
		assertFalse("a 0x0 box can never contain a point", nothing.touched(new Point2(100, 100)));

		TouchableArea box = area("box", TouchableArea.SHORT_CLASS_NAME,
				new Property("size:x", new Fractional(100f)),
				new Property("size:y", new Fractional(60f)));
		box.position.set(100, 100, 0);
		assertTrue(box.touched(new Point2(120, 110)));
		assertFalse(box.touched(new Point2(151, 100)));
		assertFalse("checkField is strictly inside the half-size", box.touched(new Point2(150, 100)));
	}

	@Test
	public void clickabilitySignalMovesTheAreaInOutTheActiveList() {
		TouchableArea area = area("button", TouchableArea.SHORT_CLASS_NAME,
				new Property("radius", new Fractional(50f)));
		area.position.set(100, 100, 0);

		Bool off = new Bool(false);
		off.setContext(area.isClikable.getContext());
		assertTrue(select.transferData(area.entity, off));
		assertFalse(area.isClikable.value);
		assertFalse(select.lstActiveTouchableAreas.contains(area));
		assertFalse("an inactive area is never touched", area.touched(new Point2(100, 100)));

		Bool on = new Bool(true);
		on.setContext(area.isClikable.getContext());
		assertTrue(select.transferData(area.entity, on));
		assertTrue(area.isClikable.value);
		assertTrue(select.lstActiveTouchableAreas.contains(area));
	}

	@Test
	public void onlyPositionAndBoolTravelAndOnlyThroughTheirOwnContext() {
		TouchableArea area = area("button", TouchableArea.SHORT_CLASS_NAME);

		Position3 where = new Position3();
		where.setContext(area.position.getContext());
		where.set(40, 60, 0);
		assertTrue(select.transferData(area.entity, where));
		assertEquals(40d, area.position.getX(), 0.001);
		assertEquals(60d, area.position.getY(), 0.001);

		Position3 foreign = new Position3();
		foreign.set(1, 1, 1);
		assertFalse("a data without the area's context is ignored", select.transferData(area.entity, foreign));
		assertEquals(40d, area.position.getX(), 0.001);

		Text text = new Text("false");
		text.setContext(area.isClikable.getContext());
		assertFalse(select.transferData(area.entity, text));
		assertTrue(area.isClikable.value);

		assertFalse(select.transferData(new Entity("empty"), where));
		assertFalse(select.startWork(0));
	}

	@Test
	public void pins_twoUnwiredAreasShareEveryNullContext() {
		// HandlerTouchSelect.java:66/74 compares contexts by identity, and an unwired component
		// has null on both sides, so one signal drives every detached area of the entity
		Entity entity = new Entity("detached");
		TouchableArea first = new TouchableArea();
		TouchableArea second = new TouchableArea();
		first.entity = entity;
		second.entity = entity;
		entity.lstTouches.add(first);
		entity.lstTouches.add(second);

		assertTrue(select.transferData(entity, new Bool(false)));
		assertFalse(first.isClikable.value);
		assertFalse(second.isClikable.value);
	}

	@Test
	public void pins_areaWithoutEntityCrashesTheClickabilityLogAfterTheValueWasWritten() {
		Entity entity = new Entity("host");
		TouchableArea orphan = new TouchableArea();
		entity.lstTouches.add(orphan);

		try {
			select.transferData(entity, new Bool(false));
			fail("HandlerTouchSelect.java:85 dereferences touchableArea.entity");
		} catch (NullPointerException expected) {
			assertFalse("the value had already been applied", orphan.isClikable.value);
		}
	}
}
