package ru.framework2d.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Component;
import ru.framework2d.core.Entity;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * The motion half of {@link HandlerSounds}: a component with {@code source = "motion"} follows the
 * movement of its own entity, loops one sound while the physics speed is non-zero, re-volumes it as
 * the speed changes, stops it when the entity stops, and keeps at most three moving entities
 * sounding at the same time. Movement is written straight into
 * {@code PhysicalCircle.movement.currentSpeed}, the same field the physics engine integrates, and
 * the handler is polled the way the scheduler polls it.
 */
public class HandlerSoundsMotionTest {

	private HandlerSounds handler;
	private SoundTestSupport.Recorder recorder;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		recorder = SoundTestSupport.attachRecordingResources();
		handler = new HandlerSounds();
	}

	@After
	public void tearDown() {
		handler = null;
		recorder = null;
		TestSupport.resetPhysicsRegistries();
		TestSupport.bootstrapEngine();
	}

	/** One checker: a motion audio component, a slide clip set, and a shape moving at {@code speed}. */
	private Entity checker(String name, double speed) {
		Entity entity = SoundTestSupport.entity(name);
		AudioComponent motion = SoundTestSupport.motionAudio(handler, entity);
		motion.sound.value = "slide";
		PhysicalCircle shape = SoundTestSupport.addShape(entity, 0f);
		SoundTestSupport.setMovementSpeed(shape, speed);
		return entity;
	}

	private AudioComponent motionOf(Entity entity) {
		for (Component component : entity.lstComponents) {
			if (component instanceof AudioComponent) return (AudioComponent) component;
		}
		return null;
	}

	private PhysicalCircle shapeOf(Entity entity) {
		return (PhysicalCircle) entity.lstPhysicals.get(0);
	}

	@Test
	public void aMotionComponentIsPolledWhileItIsConnected() {
		assertFalse(handler.haveWork());
		assertEquals(0, handler.motionComponentsNum());

		SoundTestSupport.motionAudio(handler, SoundTestSupport.entity("disc"));

		assertEquals(1, handler.motionComponentsNum());
		assertTrue(handler.haveWork());
	}

	@Test
	public void aPlainCollisionComponentIsNeverMotionPolled() {
		SoundTestSupport.audio(handler, SoundTestSupport.entity("disc"));

		assertEquals(0, handler.motionComponentsNum());
		assertFalse(handler.haveWork());
	}

	@Test
	public void anEntityConnectedTwiceIsStillPolledOnce() {
		Entity entity = SoundTestSupport.entity("disc");
		AudioComponent motion = SoundTestSupport.motionAudio(handler, entity);

		handler.connectComponent(motion);
		handler.connectComponent(motion);

		assertEquals(1, handler.motionComponentsNum());
	}

	@Test
	public void movingStartsOneLoopingSound() {
		checker("slider", 5.0);

		handler.updateMotionSounds();

		assertEquals(1, recorder.looped.size());
		assertEquals("slide", recorder.lastLoopSound());
		//default ramp: volume 1, minVolume 0.15, fullVolumeSpeed 10 at speed 5
		assertEquals(0.575f, recorder.lastLoopVolume(), 0.0001f);
		assertEquals(1, handler.motionSoundsNum());
		assertTrue(recorder.played.isEmpty());
	}

	@Test
	public void oneMotionComponentOnlyEverHoldsOneLoop() {
		Entity entity = checker("slider", 5.0);

		handler.updateMotionSounds();
		handler.updateMotionSounds();
		handler.updateMotionSounds();

		assertEquals(1, recorder.looped.size());
		assertEquals(1, handler.motionSoundsNum());
		assertEquals(2, recorder.volumes.size());
	}

	@Test
	public void stoppingStopsTheLoopAndFreesTheSlot() {
		Entity entity = checker("slider", 5.0);
		PhysicalCircle shape = shapeOf(entity);

		handler.updateMotionSounds();
		SoundTestSupport.setMovementSpeed(shape, 0.0);
		handler.updateMotionSounds();

		assertEquals(1, recorder.looped.size());
		assertEquals(1, recorder.stopped.size());
		assertEquals(0, handler.motionSoundsNum());
	}

	@Test
	public void speedBelowTheThresholdNeverStartsTheSound() {
		Entity entity = checker("crawler", 0.4);
		AudioComponent motion = motionOf(entity);
		motion.motionSpeed.value = 0.5f;

		handler.updateMotionSounds();

		assertTrue(recorder.looped.isEmpty());
		assertTrue(recorder.stopped.isEmpty());
	}

	@Test
	public void speedFallingToTheThresholdStopsTheRunningSound() {
		Entity entity = checker("slider", 5.0);
		AudioComponent motion = motionOf(entity);
		PhysicalCircle shape = shapeOf(entity);
		motion.motionSpeed.value = 1.0f;

		handler.updateMotionSounds();
		SoundTestSupport.setMovementSpeed(shape, 0.9);
		handler.updateMotionSounds();

		assertEquals(1, recorder.stopped.size());
		assertEquals(0, handler.motionSoundsNum());
	}

	@Test
	public void theVolumeTracksTheSpeedOfTheMovingChecker() {
		Entity entity = checker("slider", 5.0);

		handler.updateMotionSounds();
		SoundTestSupport.setMovementSpeed(shapeOf(entity), 10.0);
		handler.updateMotionSounds();

		assertEquals(0.575f, recorder.lastLoopVolume(), 0.0001f);
		assertEquals(1, recorder.volumes.size());
		assertEquals(1.0f, recorder.volumes.get(0).volume, 0.0001f);
	}

	@Test
	public void motionSoundSelectsRandomlyFromTheClipSet() {
		handler.setRandom(new Random(0) {
			@Override
			public int nextInt(int bound) {
				assertEquals(3, bound);
				return 2;
			}
		});

		Entity entity = checker("slider", 5.0);
		AudioComponent motion = motionOf(entity);
		motion.sounds.value = "s1, s2 ,, s3";
		motion.playback.value = "random";

		handler.updateMotionSounds();

		assertEquals("s3", recorder.lastLoopSound());
	}

	@Test
	public void onlyThreeMovingCheckersSoundAtTheSameTime() {
		Entity first = checker("first", 5.0);
		checker("second", 5.0);
		checker("third", 5.0);
		Entity fourth = checker("fourth", 5.0);

		handler.updateMotionSounds();

		assertEquals(3, recorder.looped.size());
		assertEquals(3, handler.motionSoundsNum());

		//the stopped checker gives its slot to the one that is still waiting
		SoundTestSupport.setMovementSpeed(shapeOf(first), 0.0);
		handler.updateMotionSounds();

		assertEquals(4, recorder.looped.size());
		assertEquals(3, handler.motionSoundsNum());

		SoundTestSupport.setMovementSpeed(shapeOf(fourth), 0.0);
		handler.updateMotionSounds();

		assertEquals(4, recorder.looped.size());
		assertEquals(2, handler.motionSoundsNum());
	}

	@Test
	public void theConfiguredMotionLimitIsStillCappedAtThree() {
		checker("first", 5.0);
		checker("second", 5.0);
		checker("third", 5.0);
		Entity fourth = checker("fourth", 5.0);
		motionOf(fourth).motionSounds.value = 9;

		handler.updateMotionSounds();

		assertEquals(3, recorder.looped.size());
	}

	@Test
	public void aCheckerWithTheMotionLimitOffNeverSounds() {
		Entity entity = checker("frozen", 5.0);
		(motionOf(entity)).motionSounds.value = 0;

		handler.updateMotionSounds();

		assertTrue(recorder.looped.isEmpty());
	}

	@Test
	public void aMissingClipNeverStartsAMotionSlot() {
		checker("slider", 5.0);
		((SoundTestSupport.RecordingResources) recorder.resources).resolveSounds = false;

		handler.updateMotionSounds();
		handler.updateMotionSounds();

		assertTrue(recorder.looped.isEmpty());
		assertEquals(0, handler.motionSoundsNum());
		assertTrue(recorder.stopped.isEmpty());
	}

	@Test
	public void aCheckerWithoutAnyPhysicalIsLeftAlone() {
		Entity entity = SoundTestSupport.entity("ghost");
		SoundTestSupport.motionAudio(handler, entity);

		handler.updateMotionSounds();

		assertTrue(recorder.looped.isEmpty());
		assertEquals(0, handler.motionSoundsNum());
	}

	@Test
	public void aMotionSoundIsStoppedWhenTheEntityGoesAway() {
		Entity entity = checker("slider", 5.0);
		AudioComponent motion = motionOf(entity);

		handler.updateMotionSounds();
		motion.entity = null;
		handler.updateMotionSounds();

		assertEquals(1, recorder.stopped.size());
		assertEquals(0, handler.motionSoundsNum());
	}
}
