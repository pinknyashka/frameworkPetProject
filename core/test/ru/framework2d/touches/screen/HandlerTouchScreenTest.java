package ru.framework2d.touches.screen;

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
import ru.framework2d.core.TagOptions;
import ru.framework2d.touches.DirectTouches;
import ru.framework2d.touches.TouchTestSupport;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link HandlerTouchScreen} turns a raw {@code MotionEvent} into {@link TouchFinger} state: a
 * press allocates the next free finger, a move writes screen coordinates, a release frees the
 * finger. Nothing in that chain ever consults {@code getPointerId} beyond the press, and the
 * pending changes only reach the model inside {@code iterate()}.
 *
 * No main source was changed; every pin below is the current behaviour of the shipped code.
 */
public class HandlerTouchScreenTest {

	private DirectTouches direct;
	private HandlerTouchScreen screen;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		HandlerTouchScreen.currently = false;
		direct = TouchTestSupport.registerPeriphery();
		screen = new HandlerTouchScreen();
		direct.addSubengine(screen);
	}

	@After
	public void tearDown() {
		HandlerTouchScreen.currently = false;
		screen = null;
		direct = null;
	}

	@Test
	public void constructorBindsItselfAsThePeripheryListener() {
		assertSame(screen, direct.touchHandler);
		assertEquals(10, screen.lstTouches.size());
		assertTrue(screen.lstPointers.isEmpty());
		assertTrue(screen.lstActiveTouches.isEmpty());
		assertTrue(screen.lstMovedTouches.isEmpty());
		assertTrue(screen.lstChangeStateTouches.isEmpty());
		assertFalse(HandlerTouchScreen.currently);
	}

	@Test
	public void everyFingerIsBoundToItsOwnPointerEntityInOrder() {
		for (int i = 0; i < 10; i++) {
			TouchFinger finger = screen.lstTouches.get(i);
			assertEquals(i, finger.index);
			assertEquals(-1, finger.pointerId);
			assertEquals("pointer" + i, finger.entity.name);
			assertTrue(finger.entity.lstTouches.contains(finger));
			assertEquals("InputTouchFinger", finger.getName());
			assertFalse(finger.touched.value);
			assertFalse(finger.isCurrentlyMoving.value);
		}
	}

	@Test
	public void pressAllocatesTheFirstFreeFingerAndStoresScreenCoordinates() {
		direct.toHandleEvent(TouchTestSupport.down(120f, 240f, 7));

		TouchFinger finger = screen.lstTouches.get(0);
		assertEquals(1, screen.lstActiveTouches.size());
		assertSame(finger, screen.lstActiveTouches.get(0));
		assertEquals(7, finger.pointerId);
		assertTrue(finger.touched.value);
		assertEquals(120d, finger.position.getX(), 0.001);
		assertEquals(240d, finger.position.getY(), 0.001);
		assertEquals(120d, finger.oldPosition.getX(), 0.001);
		assertTrue(screen.lstMovedTouches.contains(finger));
		assertTrue(screen.lstChangeStateTouches.contains(finger));
		assertFalse("isCurrentlyMoving is only set inside iterate()", finger.isCurrentlyMoving.value);
		assertFalse("the screen consumes the unhandled flag", direct.gotUnhandledEvent());
		assertTrue(screen.haveWork());
	}

	@Test
	public void secondPressTakesTheNextFreeFingerAndKeepsItsOwnPointerId() {
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 4));
		direct.toHandleEvent(TouchTestSupport.pointerDown(1,
				new float[][] { { 10f, 20f, 4f }, { 30f, 40f, 9f } }));

		assertEquals(2, screen.lstActiveTouches.size());
		assertEquals(4, screen.lstTouches.get(0).pointerId);
		assertEquals(9, screen.lstTouches.get(1).pointerId);
		assertEquals(10d, screen.lstTouches.get(0).position.getX(), 0.001);
		assertEquals(30d, screen.lstTouches.get(1).position.getX(), 0.001);
		assertTrue(screen.lstTouches.get(1).touched.value);
	}

	@Test
	public void pins_moveAssignsSamplesByListPositionNotByPointerId() {
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 3));
		direct.toHandleEvent(TouchTestSupport.pointerDown(1,
				new float[][] { { 10f, 20f, 3f }, { 30f, 40f, 1f } }));

		// HandlerTouchScreen.java:127-129 indexes lstActiveTouches with the event index and never
		// calls getPointerId, so the finger holding id 3 receives the sample that belongs to id 1
		direct.toHandleEvent(TouchTestSupport.move(new float[][] { { 100f, 110f, 1f }, { 120f, 130f, 3f } }));

		assertEquals(3, screen.lstTouches.get(0).pointerId);
		assertEquals(100d, screen.lstTouches.get(0).position.getX(), 0.001);
		assertEquals("oldPosition keeps the previous sample", 10d, screen.lstTouches.get(0).oldPosition.getX(), 0.001);
		assertEquals(1, screen.lstTouches.get(1).pointerId);
		assertEquals(120d, screen.lstTouches.get(1).position.getX(), 0.001);
	}

	@Test
	public void pins_releaseUsesTheActionIndexAsTheFingerIndexAndIgnoresThePointerId() {
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 8));
		direct.toHandleEvent(TouchTestSupport.pointerDown(1,
				new float[][] { { 10f, 20f, 8f }, { 30f, 40f, 2f } }));

		// HandlerTouchScreen.java:155 drops lstActiveTouches.get(event.getActionIndex())
		direct.toHandleEvent(TouchTestSupport.pointerUp(0,
				new float[][] { { 10f, 20f, 8f }, { 30f, 40f, 2f } }));

		assertFalse("the finger owning pointerId 8 was the one dropped", screen.lstTouches.get(0).touched.value);
		assertTrue("the finger of the released pointerId 2 is still down", screen.lstTouches.get(1).touched.value);
		assertEquals(1, screen.lstActiveTouches.size());
		assertSame(screen.lstTouches.get(1), screen.lstActiveTouches.get(0));
		assertTrue(screen.lstChangeStateTouches.contains(screen.lstTouches.get(0)));
		assertEquals("a released finger stays in lstTouches", 10, screen.lstTouches.size());
	}

	@Test
	public void pins_releaseOfAnUnknownPointerIndexThrowsAndLeavesCurrentlyTrue() {
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 5));

		try {
			direct.toHandleEvent(TouchTestSupport.pointerUp(3, new float[][] { { 1f, 1f, 5f } }));
			fail("only one finger is active, so action index 3 is out of range");
		} catch (IndexOutOfBoundsException expected) {
			// HandlerTouchScreen.java:313/329 set the static flag without any try/finally
			assertTrue("the static currently flag leaks", HandlerTouchScreen.currently);
			assertEquals("the exception happened before the finger was dropped", 1, screen.lstActiveTouches.size());
			assertTrue(screen.lstTouches.get(0).touched.value);
			assertEquals("the press is still queued for the next iterate", 1, screen.lstChangeStateTouches.size());
			assertSame(screen.lstTouches.get(0), screen.lstChangeStateTouches.get(0));
		}
	}

	@Test
	public void pins_pressingWithEveryFingerInUseThrowsOutOfTheFingerPool() {
		direct.toHandleEvent(TouchTestSupport.down(1f, 1f, 0));
		for (int index = 1; index < 10; index++) {
			float[][] pointers = new float[index + 1][3];
			for (int i = 0; i <= index; i++) {
				pointers[i] = new float[] { i, i, i };
			}
			direct.toHandleEvent(TouchTestSupport.pointerDown(index, pointers));
		}
		assertEquals(10, screen.lstActiveTouches.size());

		float[][] eleventh = new float[11][3];
		for (int i = 0; i < 11; i++) {
			eleventh[i] = new float[] { i, i, i };
		}
		// HandlerTouchScreen.java:87 walks lstTouches without a bound check
		try {
			direct.toHandleEvent(TouchTestSupport.pointerDown(10, eleventh));
			fail("the 11th pointer must overflow the finger pool");
		} catch (IndexOutOfBoundsException expected) {
			assertEquals(10, screen.lstActiveTouches.size());
		}
	}

	@Test
	public void cancelActionChangesNothingAndKeepsThePendingChanges() {
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));
		assertEquals(1, screen.lstActiveTouches.size());

		direct.toHandleEvent(TouchTestSupport.cancel(99f, 99f));

		assertEquals(1, screen.lstActiveTouches.size());
		assertTrue(screen.lstTouches.get(0).touched.value);
		assertEquals(1, screen.lstMovedTouches.size());
		assertTrue(screen.haveWork());
	}

	@Test
	public void iterateCommitsThePendingFingersAndEmptiesTheChangeLists() {
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));
		assertTrue(screen.startWork(0));

		assertTrue(screen.lstMovedTouches.isEmpty());
		assertTrue(screen.lstChangeStateTouches.isEmpty());
		assertFalse(screen.haveWork());
		assertFalse(screen.lstTouches.get(0).isCurrentlyMoving.value);
		assertFalse(direct.haveWork());
	}

	@Test
	public void worldPointerComponentsAreTrackedSeparatelyFromFingers() {
		WorldPointer pointer = (WorldPointer) screen.createComponent(
				new Entity("ptr"), new TagOptions(WorldPointer.SHORT_CLASS_NAME));

		assertNotNull(pointer);
		assertEquals(1, screen.lstPointers.size());
		assertSame(pointer, screen.lstPointers.get(0));
		assertEquals(10, screen.lstTouches.size());
		assertFalse(pointer.isTouches.value);
		assertFalse(pointer.isCurrentlyMoving.value);
		assertTrue(pointer.lstSelectedAreas.isEmpty());
		assertNull(pointer.linkedFinger);
	}

	@Test
	public void pins_fingerTagIsOnlyReachableThroughTheHanderOverride() {
		// HandlerTouchScreen.java:217 handles the literal "InputTouchFinger", which
		// canCreateThisComponent does not know, and the registered short name "finger" cannot be
		// instantiated because TouchFinger has no no-argument constructor
		assertFalse(screen.canCreateThisComponent("InputTouchFinger"));
		assertTrue(screen.canCreateThisComponent("finger"));
		assertNull(screen.createComponent(new Entity("orphan"), new TagOptions("finger")));
		assertNotNull(screen.createComponent(new Entity("ptr"), new TagOptions("pointer")));
	}

	@Test
	public void pins_preNamedPointerEntityIsLeftWithoutAFinger() {
		TestSupport.bootstrapEngine();
		DirectTouches fresh = TouchTestSupport.registerPeriphery();
		TouchTestSupport.Statics.storage().lstEntities.add(new Entity("pointerOld"));

		HandlerTouchScreen late = new HandlerTouchScreen();

		assertEquals("one of the ten slots is wasted", 9, late.lstTouches.size());
		assertTrue("the pre-named entity got no finger",
				TouchTestSupport.Statics.storage().lstEntities.get(0).lstTouches.isEmpty());
		assertSame(late, fresh.touchHandler);
	}
}
