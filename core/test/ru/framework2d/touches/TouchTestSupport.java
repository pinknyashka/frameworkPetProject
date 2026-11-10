package ru.framework2d.touches;

import java.util.ArrayList;
import java.util.List;

import ru.framework2d.compat.MotionEvent;
import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.EnginesScheduler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.DataInterface;
import ru.framework2d.touches.areas.TouchableArea;

/**
 * Helpers for the TOUCH batch. Kept inside {@code ru.framework2d.touches} on purpose:
 * {@code ru.framework2d.testsupport} belongs to another agent this round (AGENTS.md 1.7),
 * and package-private touch state is only visible from the package of the class under test.
 */
public final class TouchTestSupport {

	/** Owner id of the "other agent" leases that must block the touch pipeline. */
	public static final String FOREIGN_AGENT = "junit-foreign-touch-holder";

	private TouchTestSupport() {}

	/**
	 * {@code Engine.enginesManager} and friends are protected statics, so a test outside
	 * {@code ru.framework2d.core} reaches them through a subclass.
	 */
	public static final class Statics extends Engine {

		private Statics() {}

		public static EnginesManager manager() { return enginesManager; }

		public static EnginesScheduler scheduler() { return enginesScheduler; }

		public static EntityStorage storage() { return entityStorage; }
	}

	/**
	 * Creates a {@link DirectTouches} and announces it in the engine list, the way
	 * {@code <direct engineName="DirectTouches">} does; HandlerTouchScreen only binds itself
	 * to a periphery it finds there.
	 */
	public static DirectTouches registerPeriphery() {
		DirectTouches direct = new DirectTouches();
		Statics.manager().lstEngines.add(direct);
		return direct;
	}

	/** Sub-handler nesting plus the context refresh the parser does on the closing tag. */
	public static <T extends ComponentsHandler> T under(ComponentsHandler parent, T subHandler) {
		parent.addSubengine(subHandler);
		parent.getContexts();
		return subHandler;
	}

	/** ACTION_DOWN: the first pointer of a gesture, always at action index 0. */
	public static MotionEvent down(float x, float y, int pointerId) {
		return withPointers(new MotionEvent(MotionEvent.ACTION_DOWN), new float[][] { { x, y, pointerId } });
	}

	/**
	 * ACTION_POINTER_DOWN: Android keeps the whole pointer set in the event and puts the new
	 * pointer at {@code actionIndex}, so {@code getX(actionIndex)} has to be a valid slot.
	 */
	public static MotionEvent pointerDown(int actionIndex, float[][] pointers) {
		return withPointers(
				new MotionEvent(MotionEvent.ACTION_POINTER_DOWN
						| (actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT)),
				pointers);
	}

	/** ACTION_MOVE carries every pointer of the gesture, in the order of the action indexes. */
	public static MotionEvent move(float[][] pointers) {
		return withPointers(new MotionEvent(MotionEvent.ACTION_MOVE), pointers);
	}

	/** ACTION_POINTER_UP still lists the released pointer at its action index. */
	public static MotionEvent pointerUp(int actionIndex, float[][] pointers) {
		return withPointers(
				new MotionEvent(MotionEvent.ACTION_POINTER_UP
						| (actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT)),
				pointers);
	}

	/** ACTION_UP: the last remaining pointer, action index 0. */
	public static MotionEvent up(float x, float y, int pointerId) {
		return withPointers(new MotionEvent(MotionEvent.ACTION_UP), new float[][] { { x, y, pointerId } });
	}

	public static MotionEvent cancel(float x, float y) {
		return withPointers(new MotionEvent(MotionEvent.ACTION_CANCEL), new float[][] { { x, y, 0 } });
	}

	private static MotionEvent withPointers(MotionEvent event, float[][] pointers) {
		for (int i = 0; i < pointers.length; i++) {
			event.addActionPointer(pointers[i][0], pointers[i][1], (int) pointers[i][2]);
		}
		return event;
	}

	/**
	 * SubPointer declares two component classes with {@code processingNum = 1}
	 * (SubPointer.java:19), so the base area type never enters the handler's registration lists.
	 * Tests that need plain {@link TouchableArea} components add this sub handler; the quirk
	 * itself is pinned in HandlerTouchSelectTest.
	 */
	public static final class BaseAreas extends ComponentsHandler {

		@SuppressWarnings("unchecked")
		public BaseAreas() {
			super(1, (Class <Component>) TouchableArea.class.asSubclass(Component.class));
		}
	}

	/**
	 * Counts what a component-level or entity-level commit delivers. Add it to
	 * {@code data.lstLinkedHandlers} / {@code data.lstLocalLinkedHandlers} to see whether a
	 * commit happened at all, and to watch {@code DataInterface.commit} unlink handlers that
	 * answer false.
	 */
	public static final class Recorder extends ComponentsHandler {

		public final List <Entity> entities = new ArrayList <Entity>();
		public final List <Component> reflections = new ArrayList <Component>();
		public final List <DataInterface> datas = new ArrayList <DataInterface>();

		/** Value this handler answers; false makes commit drop it from the data. */
		public boolean answer = true;

		@SuppressWarnings("unchecked")
		public Recorder() {
			super(1, (Class <Component>) TouchableArea.class.asSubclass(Component.class));
		}

		@Override
		public boolean transferData(Entity object, DataInterface data) {
			entities.add(object);
			datas.add(data);
			return answer;
		}

		@Override
		public boolean transferLocalData(Component reflection, DataInterface data) {
			reflections.add(reflection);
			datas.add(data);
			return answer;
		}

		public int commits() { return entities.size() + reflections.size(); }
	}
}
