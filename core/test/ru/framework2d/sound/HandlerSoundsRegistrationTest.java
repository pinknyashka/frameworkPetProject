package ru.framework2d.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.TagOptions;
import ru.framework2d.core.agents.EngineLocks;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.data.DataInterface;
import ru.framework2d.testsupport.TestSupport;

/**
 * Registration and component-creation half of {@link HandlerSounds}: which tags it answers,
 * what its registration lists hold, what {@code createComponent} wires, and which domain the
 * handler declares as its own scope.
 */
public class HandlerSoundsRegistrationTest {

	private HandlerSounds handler;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		handler = new HandlerSounds();
	}

	@After
	public void tearDown() {
		handler = null;
		TestSupport.resetPhysicsRegistries();
	}

	@Test
	public void theHandlerHandlesTheAudioTagAndTheComponentClassName() {
		assertTrue(handler.canCreateThisComponent(AudioComponent.SHORT_CLASS_NAME));
		assertTrue(handler.canCreateThisComponent("audio"));
		assertTrue(handler.canCreateThisComponent("AudioComponent"));
		assertEquals(Arrays.asList("audio"), handler.lstComponentsShortNames);

		assertEquals(1, handler.processingNum);
		assertEquals(1, handler.lstProcessingComponents.size());
		assertEquals(1, handler.lstUsingComponents.size());
		assertEquals(1, handler.lstBasicProcessingComponents.size());
		assertSame(AudioComponent.class, handler.lstProcessingComponents.get(0));
		assertSame(AudioComponent.class, handler.lstUsingComponents.get(0));
	}

	@Test
	public void pins_componentTagMatchingIsCaseSensitive() {
		// ComponentsHandler.java:191-201 uses contentEquals on both the simple class name
		// and the SHORT_CLASS_NAME list, so no case variant of the tag is accepted
		assertFalse(handler.canCreateThisComponent("Audio"));
		assertFalse(handler.canCreateThisComponent("AUDIO"));
		assertFalse(handler.canCreateThisComponent("audioComponent"));
		assertFalse(handler.canCreateThisComponent("audi"));
		assertFalse(handler.canCreateThisComponent("pict"));
		assertFalse(handler.canCreateThisComponent("surface"));
	}

	@Test
	public void theSoundHandlerOwnsTheSoundDomain() {
		assertEquals(EngineResource.SOUND, handler.scopeResource());
		assertEquals(EngineResource.forPackage(HandlerSounds.class.getPackage().getName()),
				handler.scopeResource());
		assertTrue(handler.scopeName().startsWith(EngineLocks.currentAgent() + ":"));
		assertTrue(handler.scopeName().endsWith("HandlerSounds"));
	}

	@Test
	public void createComponentRefusesTagsTheHandlerDoesNotHandle() {
		assertNull(handler.createComponent(SoundTestSupport.entity("disc"), new TagOptions("nope")));
		assertNull(handler.createComponent(SoundTestSupport.entity("disc"), new TagOptions("Audio")));
		assertNull(handler.createComponent(SoundTestSupport.entity("disc"), new TagOptions("")));
		assertTrue(handler.lstComponentsShortNames.size() == 1);
	}

	@Test
	public void createComponentAttachesTheComponentOrLeavesItDetachedButStillConnected() {
		Entity disc = SoundTestSupport.entity("disc");

		AudioComponent audio = (AudioComponent) handler.createComponent(
				disc, new TagOptions("AudioComponent"));

		assertNotNull(audio);
		assertSame(disc, audio.entity);
		assertSame(handler, audio.getFactory());
		assertTrue(disc.lstComponents.contains(audio));
		assertEquals(1, audio.lstLinkedHandlers.size());

		// ComponentsHandler.java:412-426: connectComponent runs even when master == null, so a
		// detached component has no entity yet is subscribed and fully contexted
		AudioComponent detached = (AudioComponent) handler.createComponent(
				null, new TagOptions("audio"));

		assertNotNull(detached);
		assertNull(detached.entity);
		assertEquals(1, detached.lstLinkedHandlers.size());
		assertSame(handler, detached.lstLinkedHandlers.get(0));
		assertEquals(10, detached.lstData.size());
	}

	@Test
	public void everySoundFieldHasItsOwnContextNamedAfterTheField() {
		AudioComponent first = SoundTestSupport.audio(handler, SoundTestSupport.entity("disc"));
		AudioComponent second = SoundTestSupport.audio(handler, SoundTestSupport.entity("other"));

		List<String> names = new ArrayList<String>();
		for (DataInterface data : first.lstData) names.add(data.getName());
		Collections.sort(names);
		assertEquals(Arrays.asList("collision", "fullVolumeSpeed", "minVolume", "parallelSounds",
				"parallelWindow", "playback", "position", "sound", "sounds", "volume"), names);

		assertEquals("collision", first.collision.getContext().name);
		assertEquals("sound", first.sound.getContext().name);
		assertEquals("sounds", first.sounds.getContext().name);
		assertEquals("playback", first.playback.getContext().name);
		assertEquals("volume", first.volume.getContext().name);
		assertEquals("fullVolumeSpeed", first.fullVolumeSpeed.getContext().name);
		assertEquals("minVolume", first.minVolume.getContext().name);
		assertEquals("parallelSounds", first.parallelSounds.getContext().name);
		assertEquals("parallelWindow", first.parallelWindow.getContext().name);
		assertFalse(first.collision.getContext() == first.volume.getContext());
		assertSame(first.collision.getContext(), second.collision.getContext());
	}

	@Test
	public void pins_theInheritedPositionFieldIsSubscribedToTheSoundHandler() {
		// Component.position (Component.java:47) is a data field, so getContexts() gives the
		// sound handler a position context too and setContextToComponent + connectComponent
		// (ComponentsHandler.java:357-363 / :215-219) subscribe it twice
		AudioComponent audio = SoundTestSupport.audio(handler, SoundTestSupport.entity("disc"));

		assertEquals("position", audio.position.getName());
		assertEquals("position", audio.position.getContext().name);
		assertEquals(2, SoundTestSupport.subscriptions(audio.position, handler));
		assertEquals(2, SoundTestSupport.subscriptions(audio.collision, handler));
		assertEquals(1, audio.position.lstLocalLinkedHandlers.size());
	}

	@Test
	public void aMasterHandlerInheritsTheAudioTag() {
		HandlerSounds master = new HandlerSounds();

		master.addSubengine(handler);

		assertTrue(master.canCreateThisComponent("audio"));
		assertEquals(1, master.lstSubHandlers.size());
		assertSame(master, handler.masterHandler);
		assertEquals(master.name + "." + handler.name, handler.fullName);
	}

	@Test
	public void loadEntitiesConnectsComponentsBuiltByAnotherHandler() {
		EntityStorage storage = TestSupport.bootstrapEngine();
		Entity disc = SoundTestSupport.entity("disc");
		AudioComponent audio = SoundTestSupport.audio(handler, disc);
		storage.lstEntities.add(disc);

		// HandlerSounds.java:24: every HandlerSounds re-scans the storage in its constructor
		HandlerSounds second = new HandlerSounds();

		assertEquals(2, audio.lstLinkedHandlers.size());
		assertTrue(audio.lstLinkedHandlers.contains(second));
		assertEquals(2, SoundTestSupport.subscriptions(audio.collision, handler));
		assertEquals(1, SoundTestSupport.subscriptions(audio.collision, second));
	}
}
