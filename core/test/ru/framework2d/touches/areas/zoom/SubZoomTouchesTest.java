package ru.framework2d.touches.areas.zoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Point2;
import ru.framework2d.touches.TouchTestSupport;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.touches.areas.HandlerTouchSelect;
import ru.framework2d.touches.areas.SubPointer;
import ru.framework2d.touches.screen.TouchFinger;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link SubZoomTouches} works on the two {@link TouchFinger}s of the two pointers selected on a
 * {@link TouchableAreaZoom}: it scales {@code zoom}, clamps it to {@code zoomMin}/{@code zoomMax}
 * and re-anchors {@code realizePosition} under the middle of the pinch.
 */
public class SubZoomTouchesTest {

	private HandlerTouchSelect select;
	private SubPointer sub;
	private SubZoomTouches zoomer;

	private TouchableAreaZoom surface;
	private TouchFinger firstFinger;
	private TouchFinger secondFinger;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		select = new HandlerTouchSelect();
		sub = new SubPointer();
		select.addSubengine(sub);
		zoomer = TouchTestSupport.under(select, new SubZoomTouches());
	}

	@After
	public void tearDown() {
		secondFinger = null;
		firstFinger = null;
		surface = null;
		zoomer = null;
		sub = null;
		select = null;
	}

	private TouchableAreaZoom surface(String name) {
		return (TouchableAreaZoom) select.createComponent(
				new Entity(name), new TagOptions(TouchableAreaZoom.SHORT_CLASS_NAME));
	}

	private WorldPointer pointerWithFinger(String name, double x, double y) {
		WorldPointer pointer = (WorldPointer) select.createComponent(
				new Entity(name), new TagOptions(WorldPointer.SHORT_CLASS_NAME));
		TouchFinger finger = new TouchFinger(0);
		finger.entity = pointer.entity;
		pointer.entity.lstTouches.add(finger);
		finger.position.set(x, y);
		finger.oldPosition.set(x, y);
		return pointer;
	}

	/** Two pointers, each owning one finger; that is what the zoom handler reads. */
	private void twoFingers(double fx, double fy, double sx, double sy) {
		surface.lstPointers.clear();
		WorldPointer first = pointerWithFinger("first", fx, fy);
		firstFinger = (TouchFinger) first.entity.lstTouches.get(0);
		WorldPointer second = pointerWithFinger("second", sx, sy);
		secondFinger = (TouchFinger) second.entity.lstTouches.get(0);
		surface.lstPointers.add(first);
		surface.lstPointers.add(second);
	}

	private boolean pinch(double fx, double fy, double sx, double sy, boolean newTick) {
		if (newTick) {
			TouchTestSupport.Statics.scheduler().currentTick++;
		}
		firstFinger.oldPosition.set(firstFinger.position);
		secondFinger.oldPosition.set(secondFinger.position);
		firstFinger.position.set(fx, fy);
		secondFinger.position.set(sx, sy);
		return zoomer.transferLocalData(surface, new Point2());
	}

	@Test
	public void pinchKeepsTheOldSpanAndProducesNoZoom() {
		surface = surface("surface");
		twoFingers(0, 0, 100, 0);

		assertFalse(pinch(0, 0, 100, 0, true));
		assertEquals(1f, surface.zoom.value, 0.001f);
		assertEquals(0d, surface.realizePosition.getX(), 0.001);
		assertEquals(0d, surface.realizePosition.getY(), 0.001);
	}

	@Test
	public void fingersComingTogetherHalveTheZoomAndKeepTheAnchor() {
		surface = surface("surface");
		twoFingers(0, 0, 100, 0);

		pinch(0, 0, 50, 0, true);

		assertEquals(0.5f, surface.zoom.value, 0.001f);
		assertEquals(0d, surface.realizePosition.getX(), 0.001);
		assertEquals(0d, surface.realizePosition.getY(), 0.001);
	}

	@Test
	public void zoomIsClampedToTheConfiguredLimits() {
		surface = surface("surface");

		surface.zoom.value = 8f;
		twoFingers(0, 0, 100, 0);
		pinch(0, 0, -200, 0, true);
		assertEquals(10f, surface.zoom.value, 0.001f);

		surface.zoom.value = 0.15f;
		twoFingers(0, 0, 200, 0);
		pinch(0, 0, 100, 0, true);
		assertEquals(0.1f, surface.zoom.value, 0.001f);
	}

	@Test
	public void rotatingThePairTurnsTheRealizedPositionAroundThePinchMiddle() {
		surface = surface("surface");
		twoFingers(0, 0, 100, 0);

		pinch(0, 0, 0, 100, true);

		assertEquals(1f, surface.zoom.value, 0.001f);
		assertEquals(0d, surface.realizePosition.getX(), 0.001);
		assertEquals(0d, surface.realizePosition.getY(), 0.001);
		assertEquals(Math.PI / 2, surface.realizePosition.getAlpha(), 0.001);
	}

	@Test
	public void pins_onePinchPerSchedulerTick() {
		surface = surface("surface");
		twoFingers(0, 0, 100, 0);

		pinch(0, 0, 50, 0, true);
		assertEquals(0.5f, surface.zoom.value, 0.001f);

		pinch(0, 0, 25, 0, false);
		assertEquals("the same tick is ignored (SubZoomTouches.java:33)",
				0.5f, surface.zoom.value, 0.001f);
	}

	@Test
	public void pins_aSecondPointerWithoutAFingerCrashesThePinch() {
		surface = surface("surface");
		surface.lstPointers.add(pointerWithFinger("solo", 0, 0));

		TouchTestSupport.Statics.scheduler().currentTick++;
		assertFalse(zoomer.transferLocalData(surface, new Point2()));
		assertEquals(1f, surface.zoom.value, 0.001f);

		WorldPointer bare = (WorldPointer) select.createComponent(
				new Entity("bare"), new TagOptions(WorldPointer.SHORT_CLASS_NAME));
		surface.lstPointers.add(bare);

		try {
			zoomer.transferLocalData(surface, new Point2());
			fail("SubZoomTouches.java:48 dereferences a finger that does not exist");
		} catch (NullPointerException expected) {
			assertEquals(1f, surface.zoom.value, 0.001f);
		}
	}
}
