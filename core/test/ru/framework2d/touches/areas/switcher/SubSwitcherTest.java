package ru.framework2d.touches.areas.switcher;

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
import ru.framework2d.touches.TouchTestSupport;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.touches.areas.HandlerTouchSelect;
import ru.framework2d.touches.areas.SubPointer;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link SubSwitcher} reacts to the {@code isSelected} transition that {@code SubPointer}
 * forwards and publishes the new {@code isOn} of a {@link TouchableAreaSwitcher}.
 */
public class SubSwitcherTest {

	private HandlerTouchSelect select;
	private SubSwitcher switcherHandler;
	private SubPointer sub;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		select = new HandlerTouchSelect();
		sub = new SubPointer();
		select.addSubengine(sub);
		switcherHandler = TouchTestSupport.under(select, new SubSwitcher());
	}

	@After
	public void tearDown() {
		sub = null;
		switcherHandler = null;
		select = null;
	}

	private TouchableAreaSwitcher toggle(String name, double x, double y, double radius) {
		TagOptions options = new TagOptions(TouchableAreaSwitcher.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("radius", new Fractional((float) radius)));
		TouchableAreaSwitcher area = (TouchableAreaSwitcher) select.createComponent(new Entity(name), options);
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
	public void selectingTheSwitcherFlipsIsOnAndPublishesItToTheEntity() {
		TouchableAreaSwitcher button = toggle("toggle", 100, 100, 50);
		WorldPointer pointer = pointerAt("ptr", 120, 100);
		TouchTestSupport.Recorder recorder = new TouchTestSupport.Recorder();
		button.isOn.lstLinkedHandlers.add(recorder);

		assertFalse(button.isOn.value);
		sub.transferLocalData(pointer, touches(pointer, true));

		assertTrue(button.isSelected.value);
		assertTrue("the select edge flips the switch", button.isOn.value);
		assertEquals(1, recorder.entities.size());
		assertSame(button.isOn, recorder.datas.get(0));
		assertSame(button.entity, recorder.entities.get(0));

		sub.transferLocalData(pointer, touches(pointer, false));
		assertTrue("a release never flips the switch back", button.isOn.value);

		sub.transferLocalData(pointer, touches(pointer, true));
		assertFalse("the next select flips it back", button.isOn.value);
		assertEquals(2, recorder.entities.size());
	}

	@Test
	public void dataOfAnotherContextNeverReachesTheSwitch() {
		TouchableAreaSwitcher button = toggle("toggle", 100, 100, 50);

		Bool unrelated = new Bool(true);
		assertFalse(switcherHandler.transferLocalData(button, unrelated));
		assertFalse(button.isOn.value);

		unrelated.setContext(button.isClikable.getContext());
		assertFalse(switcherHandler.transferLocalData(button, unrelated));
		assertFalse(button.isOn.value);

		unrelated.setContext(button.isSelected.getContext());
		assertTrue("an unselected switcher is reported as handled without changing isOn",
				switcherHandler.transferLocalData(button, unrelated));
		assertFalse(button.isOn.value);
	}

	@Test
	public void pointerAndSwitcherUnderTheSameParentToggleTheSwitchTogether() {
		HandlerTouchSelect parserStyle = new HandlerTouchSelect();
		SubPointer deep = TouchTestSupport.under(parserStyle, new SubPointer());
		TouchTestSupport.under(parserStyle, new SubSwitcher());

		TagOptions options = new TagOptions(TouchableAreaSwitcher.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("radius", new Fractional(50f)));
		TouchableAreaSwitcher button = (TouchableAreaSwitcher) parserStyle.createComponent(new Entity("toggle"), options);
		button.position.set(100, 100, 0);
		WorldPointer pointer = (WorldPointer) parserStyle.createComponent(
				new Entity("ptr"), new TagOptions(WorldPointer.SHORT_CLASS_NAME));
		pointer.position.set(120, 100, 0);
		TouchTestSupport.Recorder recorder = new TouchTestSupport.Recorder();
		button.isOn.lstLinkedHandlers.add(recorder);

		deep.transferLocalData(pointer, touches(pointer, true));

		assertTrue(button.isSelected.value);
		assertTrue("the switcher is notified through the parent sub-handler list", button.isOn.value);
		assertEquals(1, recorder.entities.size());
		assertSame(button.entity, recorder.entities.get(0));
	}

	@Test
	public void pins_switcherWithoutAMasterHandlerDiesAfterItFlippedIsOn() {
		SubSwitcher lonely = new SubSwitcher();
		TouchableAreaSwitcher detached = new TouchableAreaSwitcher();
		detached.isSelected.value = true;

		try {
			// both contexts are null, so the signal matches without any map wiring
			lonely.transferLocalData(detached, new Bool(true));
			fail("SubSwitcher.java:30 dereferences masterHandler");
		} catch (NullPointerException expected) {
			assertTrue("isOn had already been flipped", detached.isOn.value);
			assertTrue(detached.isSelected.value);
		}
	}
}
