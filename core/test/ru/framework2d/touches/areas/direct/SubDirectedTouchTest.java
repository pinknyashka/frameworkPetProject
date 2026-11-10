package ru.framework2d.touches.areas.direct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import ru.framework2d.graphics.cameras.WorldPointerCamera;
import ru.framework2d.touches.TouchTestSupport;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.touches.areas.HandlerTouchSelect;
import ru.framework2d.touches.areas.SubPointer;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link SubDirectedTouch} turns a pointer's position into the shot/rotate vectors of a
 * {@link TouchableAreaDirect} pad. It reads the display size once, in its constructor, so the
 * tests install the fake graphics of the harness (1024 x 768).
 */
public class SubDirectedTouchTest {

	private HandlerTouchSelect select;
	private SubPointer sub;
	private SubDirectedTouch directed;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.installFakeGraphics();

		select = new HandlerTouchSelect();
		sub = new SubPointer();
		directed = new SubDirectedTouch();
		select.addSubengine(sub);
		TouchTestSupport.under(select, directed);
	}

	@After
	public void tearDown() {
		TestSupport.clearFakeGraphics();
		directed = null;
		sub = null;
		select = null;
	}

	private TouchableAreaDirect pad(String name, double x, double y, double proportion, boolean inScreen) {
		TagOptions options = new TagOptions(TouchableAreaDirect.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("radius", new Fractional(4f)));
		options.lstPropertys.add(new Property("proportion", new Fractional((float) proportion)));
		TouchableAreaDirect area = (TouchableAreaDirect) select.createComponent(new Entity(name), options);
		area.position.set(x, y, 0);
		area.isInScreenProportion.value = inScreen;
		return area;
	}

	private WorldPointer pointerAt(TouchableAreaDirect area, String name, double x, double y) {
		WorldPointer pointer = (WorldPointer) select.createComponent(
				new Entity(name), new TagOptions(WorldPointer.SHORT_CLASS_NAME));
		pointer.position.set(x, y, 0);
		area.lstPointers.add(pointer);
		return pointer;
	}

	@Test
	public void dragAndReleaseUseDifferentScreenProportionLimits() {
		TouchableAreaDirect pad = pad("pad", 100, 100, 10, true);
		WorldPointer pointer = pointerAt(pad, "ptr", 500, 500);

		// SubDirectedTouch.java:80-119 never checks a context, any Point2 is a drag
		assertTrue(directed.transferLocalData(pad, new Point2()));
		assertEquals(40d, pad.rotateDirect.length2(), 0.01);
		assertEquals(1f, pad.lenght.value, 0.01f);

		// SubDirectedTouch.java:55 clamps the release ratio to 1, the drag branch at :103 does not
		assertTrue(directed.transferLocalData(pad, new Bool(false)));
		assertEquals(1d, pad.shotDirect.length2(), 0.01);
		assertTrue(pad.shotDirect.getX() > 0);
		assertTrue(pad.rotateDirect.getX() > 0);
	}

	@Test
	public void pins_releaseComputesLenghtFromTheStaleRotateDirectInsteadOfTheShot() {
		TouchableAreaDirect pad = pad("pad", 500, 500, 1024, true);
		WorldPointer pointer = pointerAt(pad, "ptr", 300, 300);

		directed.transferLocalData(pad, new Point2());
		assertEquals(0.4f, pad.lenght.value, 0.01f);

		pointer.position.set(900, 300, 0);
		directed.transferLocalData(pad, new Bool(false));

		assertEquals(0.763d, pad.shotDirect.length2(), 0.01);
		// SubDirectedTouch.java:66 reads rotateDirect, which the release branch never touched
		assertEquals("lenght came from the previous drag vector", 0.4f, pad.lenght.value, 0.01f);
	}

	@Test
	public void pins_cameraPointerWithoutALinkedCameraStopsTheProportionBranch() {
		TouchableAreaDirect pad = pad("pad", 500, 500, 1024, true);
		WorldPointerCamera cameraPointer = new WorldPointerCamera();
		cameraPointer.entity = new Entity("cam");
		cameraPointer.position.set(300, 300, 0);
		pad.lstPointers.add(cameraPointer);

		try {
			directed.transferLocalData(pad, new Point2());
			fail("SubDirectedTouch.java:97-99 dereferences linkedCamera");
		} catch (NullPointerException expected) {
			// the raw vector was already written, only the proportion correction is lost
			assertEquals(-200d, pad.rotateDirect.getX(), 0.001);
			assertEquals(0f, pad.lenght.value, 0.001f);
		}
	}

	@Test
	public void aPadWithMoreThanOnePointerIsNeverReleased() {
		TouchableAreaDirect pad = pad("pad", 500, 500, 1024, true);
		pointerAt(pad, "first", 300, 300);
		pointerAt(pad, "second", 700, 300);

		assertFalse(directed.transferLocalData(pad, new Point2()));
		assertFalse(directed.transferLocalData(pad, new Bool(false)));
		assertEquals(0d, pad.shotDirect.getX(), 0.001);
		assertEquals(0d, pad.rotateDirect.getX(), 0.001);
		assertEquals(0f, pad.lenght.value, 0.001f);
	}
}
