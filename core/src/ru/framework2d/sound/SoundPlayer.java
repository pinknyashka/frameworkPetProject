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

        Sound sound = resolve(soundName);
        if (sound == null) {
            return NO_PLAYBACK;
        }

        long id = sound.play(clampVolume(volume));
        if (id >= 0) {
            Log.d(LOG_TAG, "play " + soundName + ";");
        }
        return id;
    }

    /**
     * Starts the sound in an open-ended loop; the returned id is the handle
     * {@link #setVolume(String, long, float)} and {@link #stop(String, long)} need.
     */
    public static long loop(String soundName, float volume) {

        Sound sound = resolve(soundName);
        if (sound == null) {
            return NO_PLAYBACK;
        }

        long id = sound.loop(clampVolume(volume));
        if (id >= 0) {
            Log.d(LOG_TAG, "loop " + soundName + ";");
        }
        return id;
    }

    /** Changes the volume of a running playback; silently ignores a dead handle. */
    public static void setVolume(String soundName, long id, float volume) {

        Sound sound = resolve(soundName);
        if (sound == null) {
            return;
        }

        sound.setVolume(id, clampVolume(volume));
    }

    public static void stop(String soundName, long id) {

        Sound sound = resolve(soundName);
        if (sound == null) {
            return;
        }

        sound.stop(id);
        Log.d(LOG_TAG, "stop " + soundName + ";");
    }

    private static Sound resolve(String soundName) {

        GameResources resources = Engine.getResources();
        if (resources == null) {
            return null;
        }

        return resources.getSound(soundName);
    }

    private static float clampVolume(float volume) {
        return Math.max(0f, Math.min(1f, volume));
    }
}
