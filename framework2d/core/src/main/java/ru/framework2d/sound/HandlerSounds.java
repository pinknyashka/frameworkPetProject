package ru.framework2d.sound;

import java.util.Random;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.DataContext;
import ru.framework2d.core.Entity;

import ru.framework2d.data.DataInterface;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.physics.collisions.PhysicalShape;

import ru.framework2d.compat.Log;

public class HandlerSounds extends ComponentsHandler {

	private static final String LOG_TAG = "Sound";

	private Random random = new Random();

	public HandlerSounds() {

		super(1, (Class <Component>) AudioComponent.class.asSubclass(Component.class));

		getContexts();

		loadEntities(entityStorage);
	}

	@Override
	public boolean transferData(Entity object, DataInterface data) {

		DataContext signalContext = data.getContext();

		for (Component component : object.lstComponents) {
			if (component instanceof AudioComponent) {

				AudioComponent audio = (AudioComponent) component;

				if (signalContext != null && signalContext == audio.collision.getContext()) {

					float volume = audio.volume.value;
					if (volume < 0f) volume = 0f;
					else if (volume > 1f) volume = 1f;

					//scale by the power of the hit: faster relative speed -> louder
					for (PhysicalInterface physical : object.lstPhysicals) {
						if (physical instanceof PhysicalShape) {
							PhysicalShape shape = (PhysicalShape) physical;

							float min = audio.minVolume.value;
							if (min < 0f) min = 0f;
							else if (min > 1f) min = 1f;

							double full = audio.fullVolumeSpeed.value;
							double ratio = (full > 0f) ? shape.collisionSpeed.value / full : 1f;
							if (ratio < 0) ratio = 0;
							else if (ratio > 1) ratio = 1;

							volume = (float) (volume * (min + (1f - min) * ratio));
							break;
						}
					}

					String soundName = pickSound(audio);
					SoundPlayer.play(soundName, volume);

					Log.d(LOG_TAG, name + ": " + object.name + " reacts to <" + signalContext.name + "> -> play " + soundName + " vol " + volume + ";");
					return true;
				}
			}
		}
		return false;
	}

	private String pickSound(AudioComponent audio) {

		String list = audio.sounds.value;
		String rule = audio.playback.value;

		if (list == null || list.length() == 0
				|| rule == null || !"random".equalsIgnoreCase(rule.trim())) {
			return audio.sound.value;
		}

		String[] parts = list.split(",");
		String first = null;
		int count = 0;

		for (String part : parts) {
			String name = part.trim();
			if (name.length() > 0) {
				if (first == null) first = name;
				count++;
			}
		}

		if (count == 0) return audio.sound.value;

		int pick = random.nextInt(count);
		if (pick == 0) return first;

		int index = 0;
		for (String part : parts) {
			String name = part.trim();
			if (name.length() > 0 && index++ == pick) return name;
		}

		return first;
	}

	void setRandom(Random random) {
		this.random = random == null ? new Random() : random;
	}
}
