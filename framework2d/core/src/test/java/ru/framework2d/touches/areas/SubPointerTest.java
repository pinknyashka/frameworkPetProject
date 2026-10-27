package ru.framework2d.touches.areas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.touches.TouchTestSupport;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link SubPointer} is the activation machine of the area tree: it turns the pointer's
 * {@code isTouches} into {@code isSelected}, keeps the pointer/area link lists on both sides and
 * forwards the transition to the sibling sub handlers (switcher, direct, zoom).
 */
public class SubPointerTest {

	private HandlerTouchSelect select;
	private SubPointer sub;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		select = new HandlerTouchSelect();
		sub = new SubPointer();
		select.addSubengine(sub);
		// SubPointer does not register the base area type, so plain areas need their own sub
		// handler; connectComponent of SubPointer still runs for them through lstUsingComponents
		TouchTestSupport.under(select, new TouchTestSupport.BaseAreas());
	}

	@After
	public void tearDown() {
		sub = null;
		select = null;
	}

	private TouchableArea roundArea(String name, double x, double y, double radius) {
		TagOptions options = new TagOptions(TouchableArea.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("radius", new Fractional((float) radius)));
		TouchableArea area = (TouchableArea) select.createComponent(new Entity(name), options);
		area.position.set(x, y, 0);
		return area;
	}

	private WorldPointer pointerAt(String name, double x, double y) {
		WorldPointer pointer = (WorldPointer) select.createComponent(
				new Entity(name), new TagOptions(WorldPointer.SHORT_CLASS_NAME));
		pointer.position.set(x, y, 0);
		return pointer;
	}

	private static Bool touches(WorldPointer pointer, boolean value) {
		// SubPointer.java:44 branches on the pointer field, never on the delivered data
		pointer.isTouches.value = value;
		Bool signal = new Bool(value);
		signal.setContext(pointer.isTouches.getContext());
		return signal;
	}

	@Test
	public void pressSelectsEveryClikableAreaUnderThePointerOnBothSides() {
		TouchableArea area = roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);

		assertTrue(sub.transferLocalData(pointer, touches(pointer, true)));
		assertTrue(area.isSelected.value);
		assertEquals(1, area.lstPointers.size());
		assertSame(pointer, area.lstPointers.get(0));
		assertEquals(1, pointer.lstSelectedAreas.size());
		assertSame(area, pointer.lstSelectedAreas.get(0));

		sub.transferLocalData(pointer, touches(pointer, true));
		assertEquals("a repeated press must not duplicate the link", 1, area.lstPointers.size());
		assertEquals(1, pointer.lstSelectedAreas.size());
	}

	@Test
	public void pins_theSelectBranchFollowsThePointerFieldAndNotTheDeliveredValue() {
		TouchableArea area = roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);

		// SubPointer.java:41-44 checks the context and then reads pointer.isTouches.value,
		// so the value carried by the data is ignored in both directions
		Bool contradicting = new Bool(false);
		contradicting.setContext(pointer.isTouches.getContext());
		pointer.isTouches.value = true;

		assertTrue(sub.transferLocalData(pointer, contradicting));
		assertTrue("the area was selected by a data that said false", area.isSelected.value);

		Bool opposite = new Bool(true);
		opposite.setContext(pointer.isTouches.getContext());
		pointer.isTouches.value = false;

		assertTrue(sub.transferLocalData(pointer, opposite));
		assertFalse("the area was released by a data that said true", area.isSelected.value);
	}

	@Test
	public void pressOutsideEveryAreaSelectsNothing() {
		roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 400, 400);

		assertTrue(sub.transferLocalData(pointer, touches(pointer, true)));
		assertFalse(select.lstTouchableAreas.get(0).isSelected.value);
		assertTrue(select.lstTouchableAreas.get(0).lstPointers.isEmpty());
		assertTrue(pointer.lstSelectedAreas.isEmpty());
	}

	@Test
	public void liftOutsideTheAreaClearsBothListsAndCommitsTheClearedSelection() {
		TouchableArea area = roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);
		TouchTestSupport.Recorder recorder = new TouchTestSupport.Recorder();
		area.isSelected.lstLinkedHandlers.add(recorder);

		sub.transferLocalData(pointer, touches(pointer, true));
		assertEquals(1, recorder.commits());

		pointer.position.set(400, 400, 0);
		assertTrue(sub.transferLocalData(pointer, touches(pointer, false)));

		assertFalse(area.isSelected.value);
		assertTrue(area.lstPointers.isEmpty());
		assertTrue(pointer.lstSelectedAreas.isEmpty());
		assertEquals(2, recorder.commits());
	}

	@Test
	public void pins_liftStillInsideTheAreaNeverCommitsTheClearedSelection() {
		TouchableArea area = roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);
		TouchTestSupport.Recorder recorder = new TouchTestSupport.Recorder();
		area.isSelected.lstLinkedHandlers.add(recorder);

		sub.transferLocalData(pointer, touches(pointer, true));

		// SubPointer.java:97-99 suppresses the commit while touched() still matches
		assertTrue(sub.transferLocalData(pointer, touches(pointer, false)));

		assertFalse(area.isSelected.value);
		assertTrue(area.lstPointers.isEmpty());
		assertEquals("nobody was told that isSelected went false", 1, recorder.commits());
	}

	@Test
	public void theSelectionEdgeIsForwardedToEverySiblingSubHandlerThatHandlesAreas() {
		TouchTestSupport.Recorder sibling = TouchTestSupport.under(select, new TouchTestSupport.Recorder());
		TouchableArea area = roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);

		sub.transferLocalData(pointer, touches(pointer, true));
		assertEquals(1, sibling.reflections.size());
		assertSame(area, sibling.reflections.get(0));
		assertSame(area.isSelected, sibling.datas.get(0));
		assertTrue(area.isSelected.value);

		pointer.position.set(400, 400, 0);
		sub.transferLocalData(pointer, touches(pointer, false));
		assertEquals("the release edge is forwarded as well", 2, sibling.reflections.size());
		assertFalse(area.isSelected.value);
	}

	@Test
	public void prototypeAreasAreRegisteredNowhereAndCanNeverBeSelected() {
		Entity prototype = new Entity("proto");
		prototype.isPrototype = true;
		TagOptions options = new TagOptions(TouchableArea.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("radius", new Fractional(50f)));
		TouchableArea area = (TouchableArea) select.createComponent(prototype, options);
		area.position.set(100, 100, 0);
		sub.connectComponent(area);

		WorldPointer pointer = pointerAt("ptr", 120, 100);
		assertTrue(sub.transferLocalData(pointer, touches(pointer, true)));

		assertFalse(area.isSelected.value);
		assertTrue(area.lstPointers.isEmpty());
		assertTrue(pointer.lstSelectedAreas.isEmpty());
	}

	@Test
	public void pins_reconnectingAnAreaThrowsAwayItsPointersButKeepsItsSelection() {
		TouchableArea area = roundArea("button", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);
		sub.transferLocalData(pointer, touches(pointer, true));

		// SubPointer.java:32 replaces the whole pointer list on every (re)connect
		sub.connectComponent(area);

		assertTrue(area.lstPointers.isEmpty());
		assertTrue("the area still believes it is selected", area.isSelected.value);
		assertEquals(1, pointer.lstSelectedAreas.size());
	}

	@Test
	public void moveAndMotionSignalsOnlyTravelThroughTheirOwnContext() {
		WorldPointer pointer = pointerAt("ptr", 0, 0);

		Bool moving = new Bool(true);
		assertFalse(sub.transferLocalData(pointer, moving));
		assertFalse(pointer.isCurrentlyMoving.value);

		moving.setContext(pointer.isCurrentlyMoving.getContext());
		assertTrue(sub.transferLocalData(pointer, moving));
		assertTrue(pointer.isCurrentlyMoving.value);

		Point2 dragged = new Point2(50, 50);
		assertFalse(sub.transferLocalData(pointer, dragged));

		dragged.setContext(pointer.position.getContext());
		assertTrue("reported handled although no selected area existed",
				sub.transferLocalData(pointer, dragged));
	}

	@Test
	public void everyReflectionThatIsNotAPointerIsCastBlindly() {
		TouchableArea area = roundArea("button", 100, 100, 50);

		// SubPointer.java:40 casts the reflection before looking at the data type
		try {
			sub.transferLocalData(area, area.isClikable);
			fail("a TouchableArea is not a WorldPointer");
		} catch (ClassCastException expected) {
			assertFalse(area.isSelected.value);
		}
	}
}
