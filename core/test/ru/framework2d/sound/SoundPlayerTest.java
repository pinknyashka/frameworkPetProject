package ru.framework2d.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Engine;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link SoundPlayer} on its own: name resolution, the second volume clamp, the returned
 * device handle and the silent failure paths. Playback pacing is no longer tested here;
 * {@link HandlerSoundsTransferTest} owns the per-component collision window.
 */
public class SoundPlayerTest {

	private SoundTestSupport.Recorder recorder;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		recorder = SoundTestSupport.attachRecordingResources();
	}

	@After
	public void tearDown() {
		recorder = null;
		TestSupport.bootstrapEngine();
	}

	@Test
	public void playResolvesTheSoundNameThroughTheResourceManager() {
		long id = SoundPlayer.play("bump", 0.5f);

		assertEquals(0L, id);
		assertEquals(1, recorder.requested.size());
		assertEquals("bump", recorder.requested.get(0));
		assertEquals(1, recorder.played.size());
		assertEquals("bump", recorder.lastSound());
		assertEquals(0.5f, recorder.lastVolume(), 0.0001f);
	}

	@Test
	public void pins_theVolumeHandedToTheDeviceIsClampedOnceMore() {
		SoundPlayer.play("bump", 5.0f);
		assertEquals(1.0f, recorder.lastVolume(), 0.0001f);

		recorder.clear();

		SoundPlayer.play("bump", -3.0f);
		assertEquals(0.0f, recorder.lastVolume(), 0.0001f);
	}

	@Test
	public void twoImmediatePlaybacksReachTheDeviceBecausePacingIsLeftToTheHandler() {
		SoundPlayer.play("bump", 0.4f);
		SoundPlayer.play("click", 0.6f);

		assertEquals(2, recorder.played.size());
		assertEquals("bump", recorder.played.get(0).sound);
		assertEquals("click", recorder.lastSound());
	}

	@Test
	public void aCallWithoutAnyResourceManagerIsSilentlySkipped() {
		TestSupport.bootstrapEngine();

		assertEquals(null, Engine.getResources());
		assertEquals(-1L, SoundPlayer.play("bump", 1.0f));
		assertEquals(-1L, SoundPlayer.loop("slide", 1.0f));

		SoundPlayer.setVolume("bump", 0L, 0.5f);
		SoundPlayer.stop("bump", 0L);

		assertTrue(recorder.played.isEmpty());
		assertTrue(recorder.looped.isEmpty());
		assertTrue(recorder.volumes.isEmpty());
		assertTrue(recorder.stopped.isEmpty());
		assertEquals(0, recorder.requested.size());
	}

	@Test
	public void aMissingSoundIsSilentlySkipped() {
		((SoundTestSupport.RecordingResources) recorder.resources).resolveSounds = false;

		assertEquals(-1L, SoundPlayer.play("missing", 1.0f));

		assertTrue(recorder.played.isEmpty());
		assertEquals(1, recorder.requested.size());
		assertEquals("missing", recorder.requested.get(0));
	}

	@Test
	public void loopResolvesTheSoundNameAndReturnsADeviceHandle() {
		long id = SoundPlayer.loop("slide", 0.5f);

		assertEquals(0L, id);
		assertEquals(1, recorder.looped.size());
		assertEquals("slide", recorder.lastLoopSound());
		assertEquals(0.5f, recorder.lastLoopVolume(), 0.0001f);
		assertTrue(recorder.played.isEmpty());
	}

	@Test
	public void pins_theLoopVolumeIsClampedOnceMore() {
		SoundPlayer.loop("slide", 5.0f);
		assertEquals(1.0f, recorder.lastLoopVolume(), 0.0001f);

		recorder.clear();

		SoundPlayer.loop("slide", -3.0f);
		assertEquals(0.0f, recorder.lastLoopVolume(), 0.0001f);
	}

	@Test
	public void setVolumeAndStopAddressTheHandleThatTheLoopReturned() {
		long id = SoundPlayer.loop("slide", 0.5f);

		SoundPlayer.setVolume("slide", id, 0.25f);
		SoundPlayer.stop("slide", id);

		assertEquals(1, recorder.volumes.size());
		assertEquals(0.25f, recorder.volumes.get(0).volume, 0.0001f);
		assertEquals(1, recorder.stopped.size());
		assertEquals(Long.valueOf(id), recorder.stopped.get(0));
	}

	@Test
	public void aMissingClipCannotBeLoopedNorStopped() {
		((SoundTestSupport.RecordingResources) recorder.resources).resolveSounds = false;

		assertEquals(-1L, SoundPlayer.loop("missing", 1.0f));
		SoundPlayer.setVolume("missing", 7L, 1.0f);
		SoundPlayer.stop("missing", 7L);

		assertTrue(recorder.looped.isEmpty());
		assertTrue(recorder.volumes.isEmpty());
		assertTrue(recorder.stopped.isEmpty());
	}
}
