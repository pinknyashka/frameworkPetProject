package ru.framework2d.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.agents.AgentScoped;
import ru.framework2d.core.agents.AgentTestScope;
import ru.framework2d.core.agents.EngineLocks;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.core.agents.LockBusyException;
import ru.framework2d.core.agents.LockMode;
import ru.framework2d.core.agents.LockStatus;
import ru.framework2d.core.agents.ResourceLease;
import ru.framework2d.testsupport.TestSupport;

/**
 * The SOUND half of the AGENTS.md 1.6 contract, pinned as it is today and not as it should be.
 *
 * <p>AGENTS.md section 4 states that {@code SoundPlayer.play} and
 * {@code HandlerSounds.transferData} <b>should</b> take {@code SOUND} leases. They do not: the
 * whole {@code ru.framework2d.sound} package contains no reference to {@code EngineLocks}, so an
 * exclusive lease - even one taken with the documented {@code TEST} isolation mode - does not
 * pause audio playback. These tests assert exactly that.</p>
 */
public class SoundLockContractTest {

	private HandlerSounds handler;
	private SoundTestSupport.Recorder recorder;
	private Entity disc;
	private AudioComponent audio;

	private ResourceLease ownLease;
	private ResourceLease foreignLease;
	private AgentTestScope testScope;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		recorder = SoundTestSupport.attachRecordingResources();
		handler = new HandlerSounds();
		disc = SoundTestSupport.entity("disc");
		audio = SoundTestSupport.audio(handler, disc);
	}

	@After
	public void tearDown() {
		SoundTestSupport.closeQuietly(testScope);
		SoundTestSupport.closeQuietly(foreignLease);
		SoundTestSupport.closeQuietly(ownLease);
		testScope = null;
		foreignLease = null;
		ownLease = null;
		handler = null;
		recorder = null;
		TestSupport.resetPhysicsRegistries();
		TestSupport.bootstrapEngine();
		assertTrue("no SOUND lease may survive a test",
				EngineLocks.status(EngineResource.SOUND).isFree());
	}

	@Test
	public void pins_thisAgentsExclusiveTestLeaseDoesNotStopTheSoundHandler() {
		ownLease = EngineLocks.acquire(EngineResource.SOUND, LockMode.TEST,
				"AGENTS.md 1.6 test isolation of audio playback");

		LockStatus status = EngineLocks.status(EngineResource.SOUND);
		assertTrue(status.heldBy(EngineLocks.currentAgent()));
		assertEquals(LockMode.TEST, status.holders().get(0).mode());
		assertFalse("a TEST lease forbids any further reader", status.allowsRead());
		assertFalse(status.allowsWrite());

		// HandlerSounds.transferData never asks for the domain it declares as its own scope,
		// so the isolated domain is still mutated (played) underneath the lease
		assertTrue(handler.transferData(disc, SoundTestSupport.collisionSignal(audio)));
		assertEquals(1, recorder.played.size());
		assertEquals("bump", recorder.lastSound());
		assertEquals(EngineResource.SOUND, handler.scopeResource());
	}

	@Test
	public void pins_aForeignExclusiveLeaseDoesNotStopTheSoundHandlerEither() {
		foreignLease = EngineLocks.acquire(EngineResource.SOUND, LockMode.WRITE,
				SoundTestSupport.FOREIGN_AGENT, "another agent edits the audio subsystem", 0L);

		assertFalse(EngineLocks.isAvailable(EngineResource.SOUND, LockMode.WRITE));
		assertFalse(EngineLocks.isAvailable(EngineResource.SOUND, LockMode.READ));

		assertTrue(handler.transferData(disc, SoundTestSupport.collisionSignal(audio)));
		assertEquals(1, recorder.played.size());
	}

	@Test
	public void theLockLayerItselfDoesIsolateTheSoundDomain() {
		testScope = EngineLocks.beginTestScope("sound batch isolation", EngineResource.SOUND);

		assertTrue(EngineLocks.status(EngineResource.SOUND).heldBy(EngineLocks.currentAgent()));
		assertFalse(EngineLocks.status(EngineResource.SOUND).allowsRead());
		try {
			EngineLocks.acquire(EngineResource.SOUND, LockMode.READ,
					SoundTestSupport.FOREIGN_AGENT, "a foreign reader peeks", 0L);
			fail("a foreign reader must not enter an exclusive TEST scope");
		} catch (LockBusyException expected) {
			assertTrue(expected.status().heldBy(EngineLocks.currentAgent()));
			assertTrue(expected.describe().contains("SOUND"));
		}

		testScope.close();
		testScope = null;

		assertTrue(EngineLocks.status(EngineResource.SOUND).isFree());
		assertTrue(EngineLocks.isAvailable(EngineResource.SOUND, LockMode.READ));
	}

	@Test
	public void pins_theTopLevelSoundEngineCannotDeclareALockScopeAtAll() {
		DirectSound direct = new DirectSound();

		// DirectSound extends Engine, not ComponentsHandler, so it has no scopeResource()/
		// lockScope(): the direct engine that owns the sound subtree cannot take its domain
		assertFalse(direct instanceof AgentScoped);
		assertFalse(ComponentsHandler.class.isAssignableFrom(DirectSound.class));
		assertEquals("DirectSound", direct.name);
		assertFalse(direct.startWork(0));
		assertEquals(EngineResource.SOUND,
				EngineResource.forPackage(DirectSound.class.getPackage().getName()));
	}
}
