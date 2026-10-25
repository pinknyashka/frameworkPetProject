package ru.framework2d.sound;

import com.badlogic.gdx.audio.Sound;

import ru.framework2d.compat.Log;
import ru.framework2d.core.Engine;
import ru.framework2d.core.GameResources;

public final class SoundPlayer {

    private static final String LOG_TAG = "Sound";

    private static final long MIN_REPEAT_INTERVAL_MS = 120;

    private static long lastPlayedAt;

    private SoundPlayer() {
    }

    public static void play(String soundName, float volume) {

        long now = System.currentTimeMillis();
        if (now - lastPlayedAt < MIN_REPEAT_INTERVAL_MS) {
            return;
        }

        GameResources resources = Engine.getResources();
        if (resources == null) {
            return;
        }

        Sound sound = resources.getSound(soundName);
        if (sound == null) {
            return;
        }

        lastPlayedAt = now;
        sound.play(Math.max(0f, Math.min(1f, volume)));
        Log.d(LOG_TAG, "play " + soundName + ";");
    }
}
