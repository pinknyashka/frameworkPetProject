package ru.framework2d.touches;

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

import ru.framework2d.compat.MotionEvent;
import ru.framework2d.core.agents.EngineLocks;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.core.agents.LockBusyException;
import ru.framework2d.core.agents.LockMode;
import ru.framework2d.core.agents.LockStatus;
import ru.framework2d.core.agents.ResourceLease;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link DirectTouches} is the single entry point of the whole touch pipeline, and the only
 * place where the AGENTS.md 1.6 contract ("drop the event instead of mutating a locked model")
 * is enforced. Every test here drives that gate directly: an external lease on
 * {@link EngineResource#TOUCH} must stop the event, releasing it must let the same event in.
 *
 * No main source was changed, so the oddities below are asserted exactly as they behave.
 */
public class DirectTouchesTest {

	private DirectTouches direct;

	/** Held by a foreign owner id, to simulate another agent (AGENTS.md 1.3). */
	private ResourceLease foreignLease;

	/** Held by this very agent/thread, to show reentrancy and the upgrade rejection. */
	private ResourceLease ownLease;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		direct = TouchTestSupport.registerPeriphery();
	}

	@After
	public void tearDown() {
		// never leak a TOUCH lease into the rest of the suite
		if (foreignLease != null) foreignLease.close();
		if (ownLease != null) ownLease.close();
		foreignLease = null;
		ownLease = null;
		direct = null;
	}

	private static final class Recorder implements MotionEventListener {

		int calls = 0;
		MotionEvent last;

		@Override
		public void handleMotionEvent(MotionEvent event) {
			calls++;
			last = event;
		}
	}

	@Test
	public void foreignWriteLeaseMakesTheEntryPointDropTheEvent() {
		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);
		MotionEvent event = TouchTestSupport.down(120f, 240f, 0);

		foreignLease = EngineLocks.acquire(
				EngineResource.TOUCH, LockMode.WRITE, TouchTestSupport.FOREIGN_AGENT, "another agent mutates", 0L);

		direct.toHandleEvent(event);

		assertEquals(0, listener.calls);
		assertNull("the dropped event must not be remembered", direct.getEvent());
		assertFalse("the dropped event must not set the unhandled flag", direct.gotUnhandledEvent());
		assertFalse(direct.haveWork());

		LockStatus status = EngineLocks.status(EngineResource.TOUCH);
		assertTrue(status.heldBy(TouchTestSupport.FOREIGN_AGENT));
		assertFalse(status.allowsWrite());
	}

	@Test
	public void sameEventIsHandledAfterTheForeignLeaseIsReleased() {
		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);
		MotionEvent event = TouchTestSupport.down(120f, 240f, 0);

		foreignLease = EngineLocks.acquire(
				EngineResource.TOUCH, LockMode.WRITE, TouchTestSupport.FOREIGN_AGENT, "another agent mutates", 0L);
		direct.toHandleEvent(event);
		assertEquals(0, listener.calls);

		foreignLease.close();
		foreignLease = null;

		direct.toHandleEvent(event);

		assertEquals(1, listener.calls);
		assertSame(event, listener.last);
		assertSame(event, direct.getEvent());
		assertTrue(direct.gotUnhandledEvent());
		assertTrue(EngineLocks.status(EngineResource.TOUCH).isFree());
	}

	@Test
	public void foreignReadLeaseAlsoDropsTheEventBecauseTheEntryPointAsksForWrite() {
		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);

		foreignLease = EngineLocks.acquire(
				EngineResource.TOUCH, LockMode.READ, TouchTestSupport.FOREIGN_AGENT, "another agent inspects", 0L);

		LockStatus status = EngineLocks.status(EngineResource.TOUCH);
		assertTrue("a reader does not block another reader", status.allowsRead());
		assertFalse("a reader does block the WRITE the entry point requests", status.allowsWrite());

		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));

		assertEquals(0, listener.calls);
		assertFalse(direct.gotUnhandledEvent());
	}

	@Test
	public void pins_readLeaseHeldByThisAgentEscapesAsIllegalStateInsteadOfDropping() {
		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);

		ownLease = EngineLocks.acquire(EngineResource.TOUCH, LockMode.READ, "inspect pointer state");

		// DirectTouches.java:43-51 only catches LockBusyException, while the registry rejects a
		// same-owner read-to-write upgrade with a plain IllegalStateException
		try {
			direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));
			fail("a same-agent READ lease must not be silently upgraded");
		} catch (IllegalStateException e) {
			assertFalse(e instanceof LockBusyException);
			assertTrue(e.getMessage().contains("Lock upgrade is not supported"));
		}

		assertEquals(0, listener.calls);
	}

	@Test
	public void pins_writeLeaseOfTheSameAgentAndThreadIsReenteredInsteadOfDropping() {
		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);

		ownLease = EngineLocks.acquire(EngineResource.TOUCH, LockMode.WRITE, "mutate pointer state");

		// the drop only happens for a foreign owner: the registry re-enters its own agent
		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));

		assertEquals(1, listener.calls);
		assertEquals(1, EngineLocks.status(EngineResource.TOUCH).holders().get(0).reentrant());

		ownLease.close();
		ownLease = null;
		assertTrue("the inner lease of toHandleEvent must not leak",
				EngineLocks.status(EngineResource.TOUCH).isFree());
	}

	@Test
	public void nullEventIsAcceptedAndForwardedWithoutAnyCheck() {
		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);

		direct.toHandleEvent(null);

		assertEquals(1, listener.calls);
		assertNull(listener.last);
		assertNull(direct.getEvent());
		assertTrue(direct.gotUnhandledEvent());
	}

	@Test
	public void peripheryDefaultsAndFlagAccessors() {
		assertEquals(10, direct.getMaximumTouchesNumber());
		assertNull(direct.touchHandler);
		assertNull(direct.getEvent());
		assertFalse(direct.gotUnhandledEvent());

		direct.setEventState(true);
		assertTrue(direct.gotUnhandledEvent());

		Recorder listener = new Recorder();
		direct.setTouchHandler(listener);
		assertSame(listener, direct.touchHandler);
	}

	@Test
	public void acceptedEventFeedsTheSchedulerOncePerDraw() {
		TouchTestSupport.Statics.scheduler().addSubengine(direct);
		long tick = TouchTestSupport.Statics.scheduler().currentTick;

		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));
		assertEquals(tick + 1, TouchTestSupport.Statics.scheduler().currentTick);

		direct.toHandleEvent(TouchTestSupport.down(10f, 20f, 0));
		assertEquals("the scheduler refuses a new tick until the draw is over",
				tick + 1, TouchTestSupport.Statics.scheduler().currentTick);

		direct.setWorkState(true);
		TouchTestSupport.Statics.scheduler().overDraw();
		assertEquals(tick + 2, TouchTestSupport.Statics.scheduler().currentTick);
	}

	@Test
	public void pins_touchesWithoutAMasterEngineCrashWhenTheirIterationEnds() {
		// DirectTouches.java:26 dereferences masterEngine in onFinish()
		direct.setWorkState(true);
		try {
			direct.startWork(0);
			fail("a bare DirectTouches has no master engine");
		} catch (NullPointerException expected) {
			assertFalse(direct.haveWork());
		}
	}

	@Test
	public void theUnhandledFlagIsRaisedBeforeTheListenerRuns() {
		final boolean[] flagDuringDispatch = new boolean[1];

		direct.setTouchHandler(new MotionEventListener() {
			@Override
			public void handleMotionEvent(MotionEvent motionEvent) {
				flagDuringDispatch[0] = direct.gotUnhandledEvent();
				direct.setEventState(false);
			}
		});

		direct.toHandleEvent(TouchTestSupport.down(5f, 5f, 3));

		assertTrue("the flag is set before the listener is called", flagDuringDispatch[0]);
		assertFalse("a consuming listener clears it again", direct.gotUnhandledEvent());
		assertNotNull(direct.getEvent());
	}
}
