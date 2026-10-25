package ru.framework2d.sound;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.DataContext;
import ru.framework2d.core.Entity;

import ru.framework2d.data.DataInterface;

import ru.framework2d.compat.Log;

public class HandlerSounds extends ComponentsHandler {

	private static final String LOG_TAG = "Sound";

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

					SoundPlayer.play(audio.sound.value, volume);

					Log.d(LOG_TAG, name + ": " + object.name + " reacts to <" + signalContext.name + "> -> play " + audio.sound.value + ";");
					return true;
				}
			}
		}
		return false;
	}
}
