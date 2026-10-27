package ru.framework2d.testsupport;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.mxp1.MXParser;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

import ru.framework2d.core.Component;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.Engine;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.blocks.PhysicalBlock;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.physics.medium.PhysicalBody;
import ru.framework2d.physics.medium.PhysicalMedium;

/**
 * Shared helpers for headless engine tests.
 *
 * The engine keeps several process-wide statics (Engine.resources/entityStorage/
 * enginesManager/enginesScheduler, IntervalPhysics interval counters, shape
 * interaction registries). Every test must build its own world with
 * {@link #bootstrapEngine()} and reset the shared physics state in @After.
 */
public final class TestSupport {

	public static final int FAKE_DISPLAY_WIDTH = 1024;
	public static final int FAKE_DISPLAY_HEIGHT = 768;

	private TestSupport() {}

	/**
	 * Creates a fresh EntityStorage and injects it (plus a fresh scheduler)
	 * into the Engine statics through the standard EnginesManager constructor.
	 * GameResources stay null: logging is null-guarded, so this is headless-safe.
	 */
	public static EntityStorage bootstrapEngine() {
		EntityStorage storage = new EntityStorage();
		new EnginesManager(storage, null);
		return storage;
	}

	/** Restores every IntervalPhysics static to its compiled-in default. */
	public static void resetIntervalPhysics() {
		IntervalPhysics.PHYSICAL_UNIT = 1.0d;
		IntervalPhysics.INTERVAL_LIMIT = 90;
		IntervalPhysics.IMPULSE_LIMIT = 50;
		IntervalPhysics.BASE_PHYSICAL_INTERVAL = 17;
		IntervalPhysics.MINIMAL_CALCULATION_INTERVAL = 2;
		IntervalPhysics.PROCESSED_INTERVAL = 17;
		IntervalPhysics.REMAINING_PHYSICAL_INTERVAL = 17;
		IntervalPhysics.START_ITERATION_TIMER_MARK = System.currentTimeMillis();
	}

	/** Clears the static broadphase/interaction registries of shape classes. */
	public static void resetPhysicsRegistries() {
		PhysicalCircle.BROADPHASE_HANDLER = null;
		PhysicalBlock.BROADPHASE_HANDLER = null;
		PhysicalCircle.setInteractionHandler(componentClass(PhysicalCircle.class), null);
		PhysicalCircle.setInteractionHandler(componentClass(PhysicalBlock.class), null);
		PhysicalMedium.setInteractionHandler(componentClass(PhysicalBody.class), null);
	}

	@SuppressWarnings("unchecked")
	public static Class<Component> componentClass(Class<? extends Component> c) {
		return (Class<Component>) c;
	}

	/** Pull parser over an in-memory map string (xpp3 is on the test classpath). */
	public static XmlPullParser xmlParser(String xml) throws Exception {
		MXParser parser = new MXParser();
		parser.setInput(new StringReader(xml));
		return parser;
	}

	/**
	 * MapParser seeds DisplayWidth/DisplayHeight from Gdx.graphics during
	 * construction. A dynamic proxy is enough: only getWidth()/getHeight()
	 * are ever called by the engine.
	 */
	public static Graphics installFakeGraphics() {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
				switch (method.getName()) {
					case "getWidth":
					case "getGraphicsWidth":
					case "getDisplayWidth":
					case "getBackBufferWidth":
						return FAKE_DISPLAY_WIDTH;
					case "getHeight":
					case "getGraphicsHeight":
					case "getDisplayHeight":
					case "getBackBufferHeight":
						return FAKE_DISPLAY_HEIGHT;
					default:
						Class<?> rt = method.getReturnType();
						if (rt == boolean.class) return Boolean.FALSE;
						if (rt == void.class) return null;
						if (rt.isPrimitive()) return 0;
						return null;
				}
			}
		};
		Graphics fake = (Graphics) Proxy.newProxyInstance(
				Graphics.class.getClassLoader(), new Class<?>[] { Graphics.class }, handler);
		Gdx.graphics = fake;
		return fake;
	}

	public static void clearFakeGraphics() {
		Gdx.graphics = null;
	}

	/** Minimal engine usable as a throwaway master/subengine in tests. */
	public static class PlainEngine extends Engine {
		public int subDoneCount = 0;

		@Override
		public void subEngineWorkDone(Engine subEngine) {
			subDoneCount++;
			super.subEngineWorkDone(subEngine);
		}
	}
}
