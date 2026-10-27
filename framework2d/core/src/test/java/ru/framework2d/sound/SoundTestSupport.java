package ru.framework2d.sound;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.audio.Sound;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.GameResources;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.data.Trigger;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.physics.medium.PhysicalMedium;

/**
 * Batch-local helpers for the sound tests.
 *
 * <p>The only observable output of the whole sound package is
 * {@link SoundPlayer#play(String, float)}, which needs a non-null
 * {@code GameResources}. The real loader reads {@code assets/config.xml} through
 * {@code Gdx.files} in its constructor (GameResources.java:36-38 -> :47-52), which
 * is unusable headless and would pull asset IO into a unit test, so the recorder
 * is an instance of {@link RecordingResources} allocated <b>without running that
 * constructor</b>. Nothing from {@code GameResources} is ever executed: the only
 * method the sound code calls on it ({@code getSound}) is overridden, and the
 * {@code Sound} it hands out is a recording {@link Proxy}. No audio device, no
 * {@code Gdx.files}, no {@code Gdx.audio}, no asset from {@code assets/Sounds}.</p>
 *
 * <p>{@link SoundPlayer} also keeps a process-wide throttle
 * ({@code MIN_REPEAT_INTERVAL_MS} / {@code lastPlayedAt}), so every test must reset
 * or arm it explicitly through {@link #resetPlayThrottle()} /
 * {@link #armPlayThrottle(long)} instead of sleeping.</p>
 */
public final class SoundTestSupport {

	/** An agent id that is not this JVM, used to simulate a foreign lease holder. */
	public static final String FOREIGN_AGENT = "tests-sound-foreign";

	private SoundTestSupport() {}

	/** Fresh engine statics with GameResources left null. */
	public static EntityStorage bootstrap() {
		EntityStorage storage = new EntityStorage();
		new EnginesManager(storage, null);
		return storage;
	}

	/**
	 * Installs the recording resource manager as {@code Engine.resources} through the
	 * public EnginesManager constructor, so no reflective write of an engine static is
	 * needed and the next {@code bootstrap()} clears it again.
	 */
	public static Recorder attachRecordingResources() {
		Recorder recorder = new Recorder();
		recorder.resources = allocatedRecordingResources(recorder);
		new EnginesManager(new EntityStorage(), recorder.resources);
		return recorder;
	}

	/** One recorded {@code Sound.play(volume)} together with the name it was resolved from. */
	public static final class Play {

		public final String sound;
		public final float volume;

		Play(String sound, float volume) {
			this.sound = sound;
			this.volume = volume;
		}

		@Override
		public String toString() {
			return sound + "@" + volume;
		}
	}

	/** The recording state shared by the fake resource manager and the fake sound. */
	public static final class Recorder {

		public GameResources resources;

		/** Every name the sound asked the resource manager for, in order. */
		public final List<String> requested = new ArrayList<String>();

		/** Every volume that reached a real {@code Sound.play(float)}. */
		public final List<Play> played = new ArrayList<Play>();

		String pendingName = "";

		Sound proxy;

		public void clear() {
			requested.clear();
			played.clear();
			pendingName = "";
		}

		public float lastVolume() {
			return played.isEmpty() ? Float.NaN : played.get(played.size() - 1).volume;
		}

		public String lastSound() {
			return played.isEmpty() ? null : played.get(played.size() - 1).sound;
		}
	}

	/**
	 * {@code getSound} is the single entry point of {@code SoundPlayer}; the rest of
	 * {@code GameResources} (sprites, images, maps) touches {@code Gdx.files} and is
	 * deliberately never reachable from here.
	 */
	public static final class RecordingResources extends GameResources {

		Recorder recorder;

		/** Set to false to make the cache answer "no such sound". */
		boolean resolveSounds = true;

		@Override
		public Sound getSound(String soundName) {
			recorder.requested.add(soundName);
			recorder.pendingName = soundName;
			return resolveSounds ? SoundTestSupport.soundProxy(recorder) : null;
		}
	}

	private static GameResources allocatedRecordingResources(Recorder recorder) {
		try {
			// sun.misc.Unsafe is reached reflectively so the test still compiles with
			// --release 8; allocateInstance skips GameResources()<->Config() and their
			// Gdx.files.internal("config.xml") call, and also skips field initializers,
			// so both recorder and resolveSounds are assigned after allocation.
			Class<?> unsafe = Class.forName("sun.misc.Unsafe");
			Field theUnsafe = unsafe.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object instance = unsafe.getMethod("allocateInstance", Class.class)
					.invoke(theUnsafe.get(null), RecordingResources.class);
			RecordingResources resources = (RecordingResources) instance;
			resources.recorder = recorder;
			resources.resolveSounds = true;
			return resources;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot build the recording GameResources", e);
		}
	}

	static Sound soundProxy(final Recorder recorder) {
		if (recorder.proxy == null) {
			recorder.proxy = (Sound) Proxy.newProxyInstance(
					Sound.class.getClassLoader(),
					new Class<?>[] { Sound.class },
					new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) {
							if ("play".equals(method.getName())
									&& args != null && args.length == 1 && args[0] instanceof Float) {
								recorder.played.add(
										new Play(recorder.pendingName, ((Float) args[0]).floatValue()));
							}
							Class<?> returns = method.getReturnType();
							if (returns == long.class) return Long.valueOf(0L);
							if (returns == boolean.class) return Boolean.FALSE;
							if (returns == void.class) return null;
							if (returns.isPrimitive()) return Integer.valueOf(0);
							return null;
						}
					});
		}
		return recorder.proxy;
	}

	// ------------------------------------------------------- the play throttle

	private static Field throttleField() throws NoSuchFieldException {
		Field field = SoundPlayer.class.getDeclaredField("lastPlayedAt");
		field.setAccessible(true);
		return field;
	}

	/** Clears the global repeat window of {@link SoundPlayer}; call it in @Before. */
	public static void resetPlayThrottle() {
		try {
			throttleField().setLong(null, 0L);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot reset the SoundPlayer throttle", e);
		}
	}

	/**
	 * Arms the throttle as if a sound had been played {@code millisAgo} milliseconds
	 * ago, which makes the 120 ms repeat window testable without sleeping.
	 */
	public static void armPlayThrottle(long millisAgo) {
		try {
			throttleField().setLong(null, System.currentTimeMillis() - millisAgo);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot arm the SoundPlayer throttle", e);
		}
	}

	/** The current value of the private repeat-window timestamp. */
	public static long readPlayThrottle() {
		try {
			return throttleField().getLong(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot read the SoundPlayer throttle", e);
		}
	}

	// ------------------------------------------------------- component fixtures

	/** Builds map-like tag options out of alternating attribute name / value pairs. */
	public static TagOptions tag(String tagName, Object ... nameAndData) {
		TagOptions options = new TagOptions(tagName);
		for (int pair = 0; pair + 1 < nameAndData.length; pair += 2) {
			options.lstPropertys.add(
					new Property((String) nameAndData[pair], (DataInterface) nameAndData[pair + 1]));
		}
		return options;
	}

	public static AudioComponent audio(HandlerSounds handler, Entity entity) {
		return (AudioComponent) handler.createComponent(
				entity, tag(AudioComponent.SHORT_CLASS_NAME));
	}

	/** A signal carrying the very context the audio component listens to. */
	public static Trigger collisionSignal(AudioComponent audio) {
		Trigger signal = new Trigger();
		signal.setName(audio.collision.getName());
		signal.setContext(audio.collision.getContext());
		return signal;
	}

	/** A signal carrying the name/context pair of another data (another handler). */
	public static Trigger signalLike(Trigger source) {
		Trigger signal = new Trigger();
		signal.setName(source.getName());
		signal.setContext(source.getContext());
		return signal;
	}

	/** Adds a circle to the entity and returns it; used as the collision speed source. */
	public static PhysicalCircle addShape(Entity entity, float collisionSpeed) {
		PhysicalCircle circle = new PhysicalCircle();
		circle.collisionSpeed.value = collisionSpeed;
		entity.addComponent(circle);
		return circle;
	}

	/** Adds a non-shape physical (a medium surface) to the entity. */
	public static PhysicalMedium addMedium(Entity entity) {
		PhysicalMedium medium = new PhysicalMedium();
		entity.addComponent(medium);
		return medium;
	}

	public static Entity entity(String name) {
		return new Entity(name);
	}

	// ------------------------------------------------------------ data values

	public static DataInterface fractionalValue(float value) {
		return new Fractional(value);
	}

	public static DataInterface numeralValue(int value) {
		return new Numeral(value);
	}

	public static DataInterface boolValue(boolean value) {
		return new Bool(value);
	}

	public static DataInterface textValue(String value) {
		return new Text(value);
	}

	/** Names of every data object the component carries, in list order. */
	public static String dataNames(Component component) {
		StringBuilder builder = new StringBuilder();
		for (DataInterface data : component.lstData) {
			if (builder.length() > 0) builder.append(',');
			builder.append(data.getName()).append(':').append(data.getClass().getSimpleName());
		}
		return builder.toString();
	}

	/** Number of times this handler is subscribed to the given data. */
	public static int subscriptions(DataInterface data, HandlerSounds handler) {
		int found = 0;
		for (ComponentsHandler linked : data.lstLinkedHandlers) {
			if (linked == handler) found++;
		}
		return found;
	}

	public static void closeQuietly(AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception ignored) {
				// leases and scopes close idempotently
			}
		}
	}
}
