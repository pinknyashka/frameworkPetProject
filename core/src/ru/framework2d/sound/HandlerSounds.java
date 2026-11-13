package ru.framework2d.sound;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
	private static final int MAX_PARALLEL_SOUNDS = 3;
	private static final int MAX_MOTION_SOUNDS = 3;
	private static final long MILLIS_NS = 1_000_000L;

	private static final String SOURCE_MOTION = "motion";

	private Random random = new Random();
	private final Map<AudioComponent, ParallelSoundState> parallelStates = new IdentityHashMap<AudioComponent, ParallelSoundState>();

	//components with source "motion"; they are polled once per engine tick because the physics
	//engine stops a shape without publishing anything, so the stop has to be observed
	private final List<AudioComponent> lstMotionComponents = new ArrayList<AudioComponent>();

	//running motion loops, one per audio component; the whole map is the "moving checkers" budget
	private final Map<AudioComponent, MotionPlayback> motionPlaybacks = new IdentityHashMap<AudioComponent, MotionPlayback>();

	public HandlerSounds() {

		super(1, (Class <Component>) AudioComponent.class.asSubclass(Component.class));

		getContexts();

		loadEntities(entityStorage);
	}

	@Override
	public Component connectComponent(Component component) {

		Component connected = super.connectComponent(component);

		if (connected instanceof AudioComponent) {
			registerMotionComponent((AudioComponent) connected);
		}

		return connected;
	}

	private void registerMotionComponent(AudioComponent audio) {

		if (isMotionSource(audio) && !lstMotionComponents.contains(audio)) {
			lstMotionComponents.add(audio);
			//the motion state has to be watched every tick; the physics engine never reports a stop
			setWorkState(true);
		}
	}

	static boolean isMotionSource(AudioComponent audio) {

		String source = audio.source.value;
		return source != null && SOURCE_MOTION.equalsIgnoreCase(source.trim());
	}

	@Override
	protected void iterate() {

		updateMotionSounds();

		masterEngine.subEngineWorkDone(this);
	}

	/** Starts, re-volumes and stops the motion sounds of every connected motion component. */
	void updateMotionSounds() {

		for (int no = 0; no < lstMotionComponents.size(); no++) {

			AudioComponent audio = lstMotionComponents.get(no);
			Entity object = audio.entity;

			MotionPlayback playback = motionPlaybacks.get(audio);
			double speed = (object == null) ? 0 : movementSpeedOf(object);

			if (playback == null) {

				if (object != null && speed >= motionThreshold(audio)) {
					startMotionSound(audio, object, speed);
				}

			} else if (object == null || speed < motionThreshold(audio)) {

				stopMotionSound(audio, playback, object);

			} else {

				SoundPlayer.setVolume(playback.soundName, playback.id, rampedVolume(audio, speed));
			}
		}
	}

	private void startMotionSound(AudioComponent audio, Entity object, double speed) {

		int limit = clampMotion(audio.motionSounds.value);
		if (motionPlaybacks.size() >= limit) {
			return;
		}

		String soundName = pickSound(audio);
		float volume = rampedVolume(audio, speed);
		long id = SoundPlayer.loop(soundName, volume);
		if (id < 0) {
			return;
		}

		motionPlaybacks.put(audio, new MotionPlayback(soundName, id));

		Log.d(LOG_TAG, name + ": " + object.name + " moves at " + speed + " -> loop " + soundName + " vol " + volume + ";");
	}

	private void stopMotionSound(AudioComponent audio, MotionPlayback playback, Entity object) {

		motionPlaybacks.remove(audio);
		SoundPlayer.stop(playback.soundName, playback.id);

		Log.d(LOG_TAG, name + ": " + (object == null ? "gone" : object.name) + " stopped -> stop " + playback.soundName + ";");
	}

	private double motionThreshold(AudioComponent audio) {

		double threshold = audio.motionSpeed.value;
		return threshold < 0 ? 0 : threshold;
	}

	/** Speed of the entity's own movement; the first shape wins, like the collision ramp does. */
	private double movementSpeedOf(Entity object) {

		double other = 0;

		for (PhysicalInterface physical : object.lstPhysicals) {

			double speed = Math.abs(physical.movement.currentSpeed.value);

			if (physical instanceof PhysicalShape) return speed;
			other = speed;
		}

		return other;
	}

	@Override
	public boolean transferData(Entity object, DataInterface data) {

		DataContext signalContext = data.getContext();

		boolean transfered = false;

		for (Component component : object.lstComponents) {
			if (component instanceof AudioComponent) {

				AudioComponent audio = (AudioComponent) component;

				//a motion component is driven by the movement of its entity, never by a signal
				if (isMotionSource(audio)) continue;

				if (signalContext != null && signalContext == audio.collision.getContext()) {

					playCollisionSound(object, audio, data, signalContext);
					transfered = true;
				}
			}
		}

		return transfered;
	}

	private void playCollisionSound(Entity object, AudioComponent audio, DataInterface data, DataContext signalContext) {

		long now = System.nanoTime();
		ParallelSoundState state = parallelStates.get(audio);
		if (state == null) {
			state = new ParallelSoundState();
			parallelStates.put(audio, state);
		}

		if (isDuplicateDelivery(state, data)) {
			return;
		}

		int limit = clampParallel(audio.parallelSounds.value);
		if (!allowPlayback(state, now, limit, audio.parallelWindow.value)) {
			return;
		}

		String soundName = pickSound(audio);
		float volume = collisionVolume(audio, object);
		long soundId = SoundPlayer.play(soundName, volume);
		if (soundId >= 0) {
			registerPlayback(state, now);
		}

		Log.d(LOG_TAG, name + ": " + object.name + " reacts to <" + signalContext.name + "> -> play " + soundName + " vol " + volume + ";");
	}

	/** Collision volume: the configured one, scaled by the power of the hit. */
	private float collisionVolume(AudioComponent audio, Entity object) {

		for (PhysicalInterface physical : object.lstPhysicals) {
			if (physical instanceof PhysicalShape) {
				return rampedVolume(audio, ((PhysicalShape) physical).collisionSpeed.value);
			}
		}

		return clampVolume(audio.volume.value);
	}

	/** Scales the configured volume between minVolume (slowest) and volume (fullVolumeSpeed and faster). */
	private float rampedVolume(AudioComponent audio, double speed) {

		float volume = clampVolume(audio.volume.value);
		float min = clampVolume(audio.minVolume.value);

		double full = audio.fullVolumeSpeed.value;
		double ratio = (full > 0f) ? speed / full : 1f;
		if (ratio < 0) ratio = 0;
		else if (ratio > 1) ratio = 1;

		return (float) (volume * (min + (1f - min) * ratio));
	}

	private static float clampVolume(float volume) {
		if (volume < 0f) return 0f;
		if (volume > 1f) return 1f;
		return volume;
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

	private boolean isDuplicateDelivery(ParallelSoundState state, DataInterface data) {
		if (state.lastData != data) {
			state.lastData = data;
			state.pendingDuplicates = 0;
		}

		if (state.pendingDuplicates > 0) {
			state.pendingDuplicates--;
			return true;
		}

		int links = 0;
		for (ComponentsHandler linked : data.lstLinkedHandlers) {
			if (linked == this) links++;
		}

		if (links > 1) {
			state.pendingDuplicates = links - 1;
		}

		return false;
	}

	private int clampParallel(int limit) {
		if (limit <= 0) return 0;
		if (limit > MAX_PARALLEL_SOUNDS) return MAX_PARALLEL_SOUNDS;
		return limit;
	}

	private int clampMotion(int limit) {
		if (limit <= 0) return 0;
		if (limit > MAX_MOTION_SOUNDS) return MAX_MOTION_SOUNDS;
		return limit;
	}

	private boolean allowPlayback(ParallelSoundState state, long now, int limit, int windowMs) {
		if (limit <= 0) return false;
		if (windowMs <= 0) return true;

		long windowNs = (long) windowMs * MILLIS_NS;
		int kept = 0;
		for (int i = 0; i < state.count; i++) {
			if (now - state.started[i] < windowNs) {
				state.started[kept++] = state.started[i];
			}
		}
		state.count = kept;
		return state.count < limit;
	}

	private void registerPlayback(ParallelSoundState state, long now) {
		if (state.count < state.started.length) {
			state.started[state.count++] = now;
		}
	}

	/** A running motion loop: the name it was started from, and the device handle of that sound. */
	private static final class MotionPlayback {
		private final String soundName;
		private final long id;

		private MotionPlayback(String soundName, long id) {
			this.soundName = soundName;
			this.id = id;
		}
	}

	private static final class ParallelSoundState {
		private DataInterface lastData;
		private int pendingDuplicates;
		private final long[] started = new long[MAX_PARALLEL_SOUNDS];
		private int count;
	}

	void setRandom(Random random) {
		this.random = random == null ? new Random() : random;
	}

	int motionComponentsNum() {
		return lstMotionComponents.size();
	}

	int motionSoundsNum() {
		return motionPlaybacks.size();
	}
}
