package ru.framework2d.touches.motion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.TagOptions;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Position3;
import ru.framework2d.touches.WorldPointer;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link HandlerMotion} is a half-removed subsystem: it still registers {@link TouchMotion} and
 * {@link WorldPointer}, keeps the two lists, and every branch of its transfer logic is commented
 * out. What survives is the registration contract and the data-commit side effects.
 */
public class HandlerMotionTest {

	private HandlerMotion handler;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		handler = new HandlerMotion();
	}

	@After
	public void tearDown() {
		handler = null;
	}

	private TouchMotion motion(String name) {
		return (TouchMotion) handler.createComponent(new Entity(name), new TagOptions(TouchMotion.SHORT_CLASS_NAME));
	}

	@Test
	public void theHandlerKnowsMotionsAndPointersButNeverWorks() {
		assertTrue(handler.canCreateThisComponent(TouchMotion.SHORT_CLASS_NAME));
		assertTrue(handler.canCreateThisComponent("TouchMotion"));
		assertTrue(handler.canCreateThisComponent(WorldPointer.SHORT_CLASS_NAME));
		assertFalse(handler.canCreateThisComponent("finger"));
		assertEquals(EngineResource.TOUCH, handler.scopeResource());
		assertFalse(handler.startWork(0));
	}

	@Test
	public void connectComponentKeepsEveryMotionAndOnlyTheEnabledOnesActive() {
		TouchMotion active = motion("m0");
		assertEquals(1, handler.lstMotions.size());
		assertEquals(1, handler.lstActiveMotions.size());
		assertSame(active, handler.lstMotions.get(0));
		assertEquals(200, active.latency);
		assertTrue(active.enabled.value);

		TouchMotion disabled = new TouchMotion();
		disabled.entity = new Entity("m1");
		disabled.enabled.value = false;
		handler.connectComponent(disabled);

		assertEquals(2, handler.lstMotions.size());
		assertEquals(1, handler.lstActiveMotions.size());

		handler.connectComponent(active);
		assertEquals("a repeated connect must not duplicate the motion", 2, handler.lstMotions.size());
	}

	@Test
	public void transferDataIsAnEmptyShellThatRefusesEverything() {
		TouchMotion m = motion("m0");

		Position3 where = new Position3();
		where.setContext(m.position.getContext());
		where.set(10, 20, 0);
		assertFalse(handler.transferData(m.entity, where));
		assertEquals(0d, m.position.getX(), 0.001);

		Bool off = new Bool(false);
		off.setContext(m.enabled.getContext());
		assertFalse(handler.transferData(m.entity, off));
		assertTrue(m.enabled.value);

		assertFalse(handler.transferData(new Entity("empty"), where));
	}

	@Test
	public void pins_transferLocalDataReadsTheContextNameBeforeItLogs() {
		TouchMotion m = motion("m0");

		try {
			handler.transferLocalData(m, new Position3());
			fail("HandlerMotion.java:77 dereferences data.getContext()");
		} catch (NullPointerException expected) {
			assertEquals(0d, m.position.getX(), 0.001);
			assertTrue(m.enabled.value);
		}
	}

	@Test
	public void pins_theFirstLocalCommitUnlinksTheMotionHandlerForever() {
		TouchMotion m = motion("m0");
		assertEquals(1, m.position.lstLocalLinkedHandlers.size());

		// every branch is commented out, so the answer is false and DataInterface.commit
		// (DataInterface.java:162-165) drops the handler from the data
		m.position.localCommit(null, m, m.position);

		assertTrue(m.position.lstLocalLinkedHandlers.isEmpty());
		assertFalse(handler.transferLocalData(m, m.position));
		assertEquals(0d, m.position.getX(), 0.001);
	}
}
