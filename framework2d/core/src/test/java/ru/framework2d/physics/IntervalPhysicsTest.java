package ru.framework2d.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.DataImplementator;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.TagOptions;
import ru.framework2d.testsupport.TestSupport;

public class IntervalPhysicsTest {

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
	}

	@After
	public void tearDown() {
		TestSupport.resetIntervalPhysics();
		EnginesManager.PARALLEL_WORK = false;
	}

	@Test
	public void compiledInDefaults() {
		assertEquals(1.0d, IntervalPhysics.PHYSICAL_UNIT, 0.0);
		assertEquals(90, IntervalPhysics.INTERVAL_LIMIT);
		assertEquals(50f, IntervalPhysics.IMPULSE_LIMIT, 0f);
		assertEquals(17, IntervalPhysics.BASE_PHYSICAL_INTERVAL);
		assertEquals(2, IntervalPhysics.MINIMAL_CALCULATION_INTERVAL);
	}

	@Test
	public void setOptionsReadsTagProperties() {
		IntervalPhysics physics = new IntervalPhysics();

		TagOptions options = new TagOptions("direct");
		options.lstPropertys.add(DataImplementator.toProperty("ph_unit", "0.5"));
		options.lstPropertys.add(DataImplementator.toProperty("interval_limit", "40"));
		options.lstPropertys.add(DataImplementator.toProperty("impulse_limit", "7"));
		physics.setOptions(options);

		assertEquals(0.5d, IntervalPhysics.PHYSICAL_UNIT, 1e-9);
		assertEquals(40, IntervalPhysics.INTERVAL_LIMIT);
		assertEquals(7f, IntervalPhysics.IMPULSE_LIMIT, 0f);
	}

	@Test
	public void doWorkAccumulatesElapsedAndCapsAtLimit() {
		IntervalPhysics physics = new IntervalPhysics();
		IntervalPhysics.REMAINING_PHYSICAL_INTERVAL = 10;
		IntervalPhysics.START_ITERATION_TIMER_MARK = System.currentTimeMillis() - 5000;

		physics.doWork();

		// far more than the limit elapsed: the accumulator must clamp
		assertEquals(IntervalPhysics.INTERVAL_LIMIT, IntervalPhysics.PROCESSED_INTERVAL);
		assertTrue(IntervalPhysics.REMAINING_PHYSICAL_INTERVAL <= IntervalPhysics.INTERVAL_LIMIT);
	}

	@Test
	public void shortGapsStillAdvanceTime() {
		IntervalPhysics physics = new IntervalPhysics();
		IntervalPhysics.REMAINING_PHYSICAL_INTERVAL = 0;
		IntervalPhysics.START_ITERATION_TIMER_MARK = System.currentTimeMillis();

		physics.doWork();

		assertTrue(IntervalPhysics.PROCESSED_INTERVAL > 0);
	}

	@Test
	public void onFinishSettlesAndReportsDoneToMaster() {
		IntervalPhysics physics = new IntervalPhysics();
		physics.addSubengine(new TestSupport.PlainEngine());

		TestSupport.PlainEngine master = new TestSupport.PlainEngine();
		physics.masterEngine = master;

		IntervalPhysics.REMAINING_PHYSICAL_INTERVAL = 30;
		IntervalPhysics.PROCESSED_INTERVAL = 17;

		physics.onFinish();

		assertEquals(13, IntervalPhysics.REMAINING_PHYSICAL_INTERVAL);
		assertEquals(1, master.subDoneCount);
	}

	@Test
	public void onFinishRepeatsThroughSchedulerWhenBehind() {
		TestSupport.PlainEngine master = new TestSupport.PlainEngine();

		IntervalPhysics physics = new IntervalPhysics();
		physics.masterEngine = master;
		physics.setWorkState(true);

		// one settled step leaves more than the base interval -> the physics
		// direct asks the scheduler to repeat; the repeat consumes the rest
		IntervalPhysics.REMAINING_PHYSICAL_INTERVAL = 50;
		IntervalPhysics.PROCESSED_INTERVAL = 17;

		physics.onFinish();

		assertTrue("catch-up must finish with the engine reporting done",
				master.subDoneCount >= 1);
		assertTrue(IntervalPhysics.REMAINING_PHYSICAL_INTERVAL <= IntervalPhysics.BASE_PHYSICAL_INTERVAL);
		assertFalse(physics.isInProcess());
	}
}
