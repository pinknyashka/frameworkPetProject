package ru.framework2d.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Engine;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link SoundPlayer} on its own: name resolution, the second volume clamp and the
 * process-wide repeat window. The window is driven through {@link SoundTestSupport} instead
 * of sleeping, so every test stays deterministic.
 */
public class SoundPlayerTest {

	private SoundTestSupport.Recorder recorder;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		SoundTestSupport.resetPlayThrottle();
		recorder = SoundTestSupport.attachRecordingResources();
	}

	@After
	public void tearDown() {
		recorder = null;
		SoundTestSupport.resetPlayThrottle();
		TestSupport.bootstrapEngine();
	}

	@Test
	public void playResolvesTheSoundNameThroughTheResourceManager() {
		SoundPlayer.play("bump", 0.5f);

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

		SoundTestSupport.resetPlayThrottle();
		recorder.clear();

		SoundPlayer.play("bump", -3.0f);
		assertEquals(0.0f, recorder.lastVolume(), 0.0001f);
	}

	@Test
	public void pins_theRepeatWindowIsGlobalAndNotPerSoundName() {
		// SoundPlayer.java:15 keeps a single timestamp for the whole process
		SoundTestSupport.armPlayThrottle(50L);
		SoundPlayer.play("bump", 1.0f);
		assertTrue(recorder.played.isEmpty());

		SoundTestSupport.armPlayThrottle(200L);
		SoundPlayer.play("click", 1.0f);
		assertEquals(1, recorder.played.size());
		assertEquals("click", recorder.lastSound());
	}

	@Test
	public void aCallWithoutAnyResourceManagerIsSilentlySkipped() {
		TestSupport.bootstrapEngine();

		assertEquals(null, Engine.getResources());
		SoundPlayer.play("bump", 1.0f);

		assertTrue(recorder.played.isEmpty());
		assertEquals(0, recorder.requested.size());
		assertEquals(0L, SoundTestSupport.readPlayThrottle());
	}

	@Test
	public void pins_onlyAnActualPlayArmsTheRepeatWindow() {
		SoundPlayer.play("bump", 1.0f);
		assertNotEquals(0L, SoundTestSupport.readPlayThrottle());
		assertEquals(1, recorder.played.size());

		SoundTestSupport.resetPlayThrottle();
		recorder.clear();
		((SoundTestSupport.RecordingResources) recorder.resources).resolveSounds = false;

		// SoundPlayer.java:32-35 returns before line 37, so a failed lookup never throttles
		SoundPlayer.play("missing", 1.0f);

		assertEquals(0L, SoundTestSupport.readPlayThrottle());
		assertTrue(recorder.played.isEmpty());
		assertEquals(1, recorder.requested.size());
		assertEquals("missing", recorder.requested.get(0));
	}
}
