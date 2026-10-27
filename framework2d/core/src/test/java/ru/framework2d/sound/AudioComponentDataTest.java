package ru.framework2d.sound;

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

import ru.framework2d.core.Component;
import ru.framework2d.core.Entity;
import ru.framework2d.testsupport.TestSupport;

/**
 * Data mapping of {@link AudioComponent}: which attribute names reach which field, what the
 * compiled-in defaults are, how attribute values are converted and clamped, and what a clone
 * inherits.
 *
 * <p>Attributes are applied through {@code ComponentsHandler.createComponent} ->
 * {@code setContextToComponent} -> {@code setOptions}, i.e. exactly the path a map tag
 * {@code <audio sound = "bump" volume = "1"/>} takes.</p>
 */
public class AudioComponentDataTest {

	private HandlerSounds handler;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		SoundTestSupport.resetPlayThrottle();
		handler = new HandlerSounds();
	}

	@After
	public void tearDown() {
		handler = null;
		TestSupport.resetPhysicsRegistries();
	}

	private AudioComponent audio(Object ... nameAndData) {
		return (AudioComponent) handler.createComponent(
				SoundTestSupport.entity("disc"), SoundTestSupport.tag("audio", nameAndData));
	}

	@Test
	public void theDefaultsOfAFreshAudioComponent() {
		AudioComponent audio = new AudioComponent();

		assertEquals(AudioComponent.SHORT_CLASS_NAME, audio.getShortClassName());
		assertEquals("audio", audio.getShortClassName());
		assertEquals("AudioComponent", audio.getName());
		assertEquals("bump", audio.sound.value);
		assertEquals(1.0f, audio.volume.value, 0.0001f);
		assertEquals(10.0f, audio.fullVolumeSpeed.value, 0.0001f);
		assertEquals(0.15f, audio.minVolume.value, 0.0001f);
		assertNotNull(audio.collision);
		assertEquals(0, audio.lstData.size());
	}

	@Test
	public void soundAttributesLandOnTheMatchingDataField() {
		AudioComponent audio = audio(
				"sound", SoundTestSupport.textValue("click"),
				"volume", SoundTestSupport.fractionalValue(0.5f),
				"fullVolumeSpeed", SoundTestSupport.fractionalValue(4.0f),
				"minVolume", SoundTestSupport.fractionalValue(0.3f));

		assertEquals("click", audio.sound.value);
		assertEquals(0.5f, audio.volume.value, 0.0001f);
		assertEquals(4.0f, audio.fullVolumeSpeed.value, 0.0001f);
		assertEquals(0.3f, audio.minVolume.value, 0.0001f);
	}

	@Test
	public void pins_attributeNamesAreCaseSensitive() {
		// DataInterface.java:51-58 matches an attribute against data.getName() with
		// contentEquals, unlike Property.isNamed, which is case-insensitive
		AudioComponent audio = audio(
				"Volume", SoundTestSupport.fractionalValue(0.25f),
				"SOUND", SoundTestSupport.textValue("click"),
				"minvolume", SoundTestSupport.fractionalValue(0.9f));

		assertEquals(1.0f, audio.volume.value, 0.0001f);
		assertEquals("bump", audio.sound.value);
		assertEquals(0.15f, audio.minVolume.value, 0.0001f);
	}

	@Test
	public void everyAttributeValueIsConvertedIntoTheFieldItLandsOn() {
		AudioComponent audio = audio(
				"volume", SoundTestSupport.numeralValue(2),
				"minVolume", SoundTestSupport.boolValue(true),
				"fullVolumeSpeed", SoundTestSupport.textValue("0.25"));

		assertEquals(2.0f, audio.volume.value, 0.0001f);
		assertEquals(1.0f, audio.minVolume.value, 0.0001f);
		assertEquals(0.25f, audio.fullVolumeSpeed.value, 0.0001f);

		// Text.setValue (data/Text.java:34-46) stringifies every other data type
		assertEquals("7", audio("sound", SoundTestSupport.numeralValue(7)).sound.value);
		assertEquals("false", audio("sound", SoundTestSupport.boolValue(false)).sound.value);
	}

	@Test
	public void pins_anUnparsableVolumeAttributeAbortsTheWholeComponent() {
		Entity disc = SoundTestSupport.entity("disc");

		// Fractional.setValue (data/Fractional.java:25-26) parses a Text value with no guard,
		// and ComponentsHandler.java:416-421 applies the options before the entity is attached,
		// so the component is built and then thrown away with the exception
		try {
			handler.createComponent(disc, SoundTestSupport.tag("audio",
					"sound", SoundTestSupport.textValue("click"),
					"volume", SoundTestSupport.textValue("loud")));
			fail("a non-numeric volume attribute must not be swallowed");
		} catch (NumberFormatException expected) {
			assertTrue(disc.lstComponents.isEmpty());
		}
	}

	@Test
	public void pins_aCollisionAttributeIsMatchedByTheTriggerAndChangesNothing() {
		// data/Trigger.java carries no value at all: the name matches, setValue is empty
		AudioComponent audio = audio("collision", SoundTestSupport.textValue("boom"));

		assertEquals("bump", audio.sound.value);
		assertEquals(1.0f, audio.volume.value, 0.0001f);
		assertEquals("collision", audio.collision.getName());
	}

	@Test
	public void pins_unknownAttributesAreSilentlyIgnored() {
		AudioComponent audio = audio(
				"volumeX", SoundTestSupport.fractionalValue(0.1f),
				"volUE", SoundTestSupport.textValue("x"),
				"", SoundTestSupport.textValue("y"));

		assertEquals(1.0f, audio.volume.value, 0.0001f);
		assertEquals("bump", audio.sound.value);
	}

	@Test
	public void aVolumeBindingIsReEvaluatedOnEveryCommitOfTheField() {
		// Component.java:196-208 turns <field>_binding into an Expression and
		// DataInterface.java:123 re-applies it before the handlers are notified
		AudioComponent audio = audio("volume_binding", SoundTestSupport.textValue("2*3"));

		assertEquals(1.0f, audio.volume.value, 0.0001f);

		audio.volume.commit(null, audio.entity);

		assertEquals(6.0f, audio.volume.value, 0.0001f);
	}

	@Test
	public void aCloneCopiesEverySoundFieldAndKeepsTheContexts() {
		AudioComponent prototype = SoundTestSupport.audio(handler, SoundTestSupport.entity("proto"));
		prototype.volume.value = 0.4f;
		prototype.sound.value = "click";
		prototype.minVolume.value = 0.3f;
		prototype.fullVolumeSpeed.value = 4.0f;
		Entity copies = SoundTestSupport.entity("copies");

		Component made = prototype.createAutoClone(copies);

		assertTrue(made instanceof AudioComponent);
		AudioComponent clone = (AudioComponent) made;
		assertEquals(0.4f, clone.volume.value, 0.0001f);
		assertEquals("click", clone.sound.value);
		assertEquals(0.3f, clone.minVolume.value, 0.0001f);
		assertEquals(4.0f, clone.fullVolumeSpeed.value, 0.0001f);
		assertEquals(6, clone.lstData.size());
		assertEquals("collision", clone.collision.getName());
		assertSame(prototype.collision.getContext(), clone.collision.getContext());
		assertFalse(clone.collision == prototype.collision);
		assertSame(copies, clone.entity);
		assertTrue(copies.lstComponents.contains(clone));
		assertEquals(2, SoundTestSupport.subscriptions(clone.collision, handler));

		// Component.java:75-77 always returns null: only createAutoClone works for this type
		assertNull(prototype.createClone(copies));
	}
}
