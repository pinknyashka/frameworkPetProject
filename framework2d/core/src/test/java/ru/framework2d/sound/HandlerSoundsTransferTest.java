package ru.framework2d.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.data.Trigger;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * The {@link HandlerSounds#transferData} branch matrix and the collision -> volume ramp, driven
 * headlessly: the signal is a bare {@link Trigger} carrying the handler's own {@code collision}
 * context, and the audible result is captured by the recording resource manager of
 * {@link SoundTestSupport} (no audio device, no asset).
 */
public class HandlerSoundsTransferTest {

	private HandlerSounds handler;
	private SoundTestSupport.Recorder recorder;
	private Entity disc;
	private AudioComponent audio;
	private PhysicalCircle shape;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		SoundTestSupport.resetPlayThrottle();
		recorder = SoundTestSupport.attachRecordingResources();
		handler = new HandlerSounds();
		disc = SoundTestSupport.entity("disc");
		audio = SoundTestSupport.audio(handler, disc);
	}

	@After
	public void tearDown() {
		handler = null;
		recorder = null;
		shape = null;
		TestSupport.resetPhysicsRegistries();
		SoundTestSupport.resetPlayThrottle();
		TestSupport.bootstrapEngine();
	}

	/** One signal straight into the handler; returns the volume that reached the device. */
	private float play(Trigger signal) {
		SoundTestSupport.resetPlayThrottle();
		assertTrue(handler.transferData(disc, signal));
		return recorder.lastVolume();
	}

	/** Keeps a single circle in the entity and drives its collision speed. */
	private float ramp(float collisionSpeed) {
		if (shape == null) shape = SoundTestSupport.addShape(disc, collisionSpeed);
		else shape.collisionSpeed.value = collisionSpeed;
		return play(SoundTestSupport.collisionSignal(audio));
	}

	@Test
	public void anEntityWithoutAnAudioComponentIsRefused() {
		assertFalse(handler.transferData(SoundTestSupport.entity("empty"),
				SoundTestSupport.collisionSignal(audio)));
		assertTrue(recorder.played.isEmpty());
	}

	@Test
	public void pins_aRefusedSignalUnsubscribesOnlyThatDataFromTheHandler() {
		// DataInterface.java:130-137 drops the handler from the data that got a false answer
		assertFalse(handler.transferData(disc, audio.position));
		assertEquals(2, SoundTestSupport.subscriptions(audio.position, handler));

		audio.position.commit(null, disc);

		assertEquals(0, SoundTestSupport.subscriptions(audio.position, handler));
		assertTrue(recorder.played.isEmpty());
		assertEquals(2, SoundTestSupport.subscriptions(audio.collision, handler));
		assertEquals(1.0f, play(SoundTestSupport.collisionSignal(audio)), 0.0001f);
	}

	@Test
	public void aSignalWithoutAContextIsRefusedAndDoesNotCrash() {
		// HandlerSounds.java:37 guards signalContext != null before comparing it
		assertFalse(handler.transferData(disc, new Trigger()));
		assertTrue(recorder.played.isEmpty());
	}

	@Test
	public void pins_contextMatchingIsByIdentityNotByName() {
		HandlerSounds other = new HandlerSounds();
		AudioComponent foreign = SoundTestSupport.audio(other, SoundTestSupport.entity("other"));
		Trigger sameName = SoundTestSupport.signalLike(foreign.collision);

		assertEquals("collision", sameName.getContext().name);
		assertEquals("collision", audio.collision.getContext().name);
		assertFalse(sameName.getContext() == audio.collision.getContext());
		assertFalse(handler.transferData(disc, sameName));
		assertTrue(recorder.played.isEmpty());
	}

	@Test
	public void theCollisionSignalPlaysTheConfiguredSoundAtItsRawVolume() {
		assertTrue(handler.transferData(disc, SoundTestSupport.collisionSignal(audio)));

		assertEquals(1, recorder.played.size());
		assertEquals("bump", recorder.lastSound());
		assertEquals(1.0f, recorder.lastVolume(), 0.0001f);
		assertEquals(1, recorder.requested.size());

		audio.sound.value = "click";

		assertEquals(1.0f, play(SoundTestSupport.collisionSignal(audio)), 0.0001f);
		assertEquals("click", recorder.lastSound());
	}

	@Test
	public void aStillCollisionIsPlayedAtMinVolume() {
		assertEquals(0.15f, ramp(0f), 0.0001f);
	}

	@Test
	public void theRampIsLinearBetweenMinAndFullVolume() {
		assertEquals(0.575f, ramp(5f), 0.0001f);

		audio.minVolume.value = 0.25f;

		assertEquals(0.625f, ramp(5f), 0.0001f);
	}

	@Test
	public void pins_everyCollisionSpeedOutsideTheUnitRatioRangeIsClamped() {
		assertEquals(1.0f, ramp(20f), 0.0001f);
		assertEquals(0.15f, ramp(-5f), 0.0001f);
	}

	@Test
	public void pins_aNonPositiveFullVolumeSpeedAlwaysPlaysAtFullVolume() {
		// HandlerSounds.java:52-53: full <= 0 takes the ": 1f" branch instead of dividing
		audio.fullVolumeSpeed.value = 0f;
		assertEquals(1.0f, ramp(1000f), 0.0001f);

		audio.fullVolumeSpeed.value = -3f;
		assertEquals(1.0f, ramp(1000f), 0.0001f);
	}

	@Test
	public void pins_minVolumeAndRawVolumeAreClampedIntoTheUnitRange() {
		audio.minVolume.value = 5f;
		assertEquals(1.0f, ramp(0f), 0.0001f);

		audio.minVolume.value = -1f;
		assertEquals(0.5f, ramp(5f), 0.0001f);

		audio.minVolume.value = 0.15f;
		audio.volume.value = -3f;
		assertEquals(0.0f, ramp(10f), 0.0001f);

		audio.volume.value = 5f;
		assertEquals(1.0f, ramp(10f), 0.0001f);
	}

	@Test
	public void pins_onlyTheFirstPhysicalShapeOfTheEntityDrivesTheRamp() {
		// HandlerSounds.java:44-60 skips non-shape physicals and breaks on the first shape
		SoundTestSupport.addMedium(disc);

		assertEquals(0.15f, ramp(0f), 0.0001f);

		SoundTestSupport.addShape(disc, 10f);

		assertEquals(0.15f, ramp(0f), 0.0001f);
		assertEquals(3, disc.lstPhysicals.size());
	}

	@Test
	public void pins_onlyTheFirstAudioComponentOfAnEntityMakesASound() {
		AudioComponent second = SoundTestSupport.audio(handler, disc);
		second.sound.value = "click";

		assertTrue(handler.transferData(disc, SoundTestSupport.collisionSignal(second)));

		assertEquals(1, recorder.played.size());
		assertEquals("bump", recorder.lastSound());
	}

	@Test
	public void transferDataNeverChangesTheModelItReads() {
		assertEquals(0.575f, ramp(5f), 0.0001f);

		assertEquals(1.0f, audio.volume.value, 0.0001f);
		assertEquals(10.0f, audio.fullVolumeSpeed.value, 0.0001f);
		assertEquals(0.15f, audio.minVolume.value, 0.0001f);
		assertEquals(5.0f, shape.collisionSpeed.value, 0.0001f);
		assertEquals(2, SoundTestSupport.subscriptions(audio.collision, handler));
	}

	@Test
	public void pins_aCollisionCommitPlaysOnceEvenThoughTheHandlerIsSubscribedTwice() {
		// setContextToComponent (ComponentsHandler.java:357-363) and connectComponent (:215-219)
		// both link the handler, so one commit answers transferData twice; only the
		// MIN_REPEAT_INTERVAL_MS window (SoundPlayer.java:22-25) hides the second sound
		audio.collision.commit(null, disc);

		assertEquals(1, recorder.played.size());
		assertEquals(1, recorder.requested.size());
		assertEquals(2, SoundTestSupport.subscriptions(audio.collision, handler));
	}

	@Test
	public void aMissingSoundNameIsSilentlySkippedAndTheSignalIsStillHandled() {
		((SoundTestSupport.RecordingResources) recorder.resources).resolveSounds = false;

		SoundTestSupport.resetPlayThrottle();
		assertTrue(handler.transferData(disc, SoundTestSupport.collisionSignal(audio)));

		assertTrue(recorder.played.isEmpty());
		assertEquals(1, recorder.requested.size());
	}
}
