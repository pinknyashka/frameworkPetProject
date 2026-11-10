package ru.framework2d.sound;

import com.badlogic.gdx.audio.Sound;

import ru.framework2d.compat.Log;
import ru.framework2d.core.Engine;
import ru.framework2d.core.GameResources;

public final class SoundPlayer {

    private static final String LOG_TAG = "Sound";

    private static final long NO_PLAYBACK = -1L;

    private SoundPlayer() {
    }

    public static long play(String soundName, float volume) {

        GameResources resources = Engine.getResources();
        if (resources == null) {
            return NO_PLAYBACK;
        }

        Sound sound = resources.getSound(soundName);
        if (sound == null) {
            return NO_PLAYBACK;
        }

        long id = sound.play(Math.max(0f, Math.min(1f, volume)));
        if (id >= 0) {
            Log.d(LOG_TAG, "play " + soundName + ";");
        }
        return id;
    }
}
