package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.logic.classes.HandlerUnitClasses;
import ru.framework2d.logic.classes.UnitClass;
import ru.framework2d.logic.fields.LogicalField;
import ru.framework2d.logic.fields.LogicalUnit;
import ru.framework2d.logic.players.HandlerPlayers;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.logic.players.LogicalPlayer;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.areas.TouchableArea;
import ru.framework2d.testsupport.TestSupport;

/**
 * The shipped checker map must close a turn on a settled board.
 *
 * This runs the real {@code assets/xml/checkers_tree.xml} (only its engines and components that need
 * a GL context or real image and sound files are taken out) through the real scheduler: respawners
 * place the checkers, a shot is committed the way the touch handler commits a drag shot, and the map
 * must then hand the turn over once every checker stopped - with no checker leaving the board. The
 * second check locks the map wiring that makes this possible, which is exactly what
 * {@code assets/xml/checkers_tree_hq.xml} once lacked. The third check locks the half the player
 * sees: after the hand over the checkers of the new player are clickable and the old player's are not.
 */
public class ShippedMapTurnClosesTest {

	private static final String SHOOTER = "checkerwhite4";

	/** Engines and components that need a GL context or real media files. */
	private static final String [] MEDIA_ENGINES = { "OutputDraw", "DirectSound", "DirectTouches" };
	private static final String [] MEDIA_COMPONENTS = {
			"pict", "relief", "shadow", "animation", "audio", "light", "drag",
			"camera", "zoomer", "d_touch", "switcher" };

	private EntityStorage storage;
	private EnginesManager enginesManager;
	private EnginesScheduler scheduler;

	@Before
	public void setUp() {

		TestSupport.installFakeGraphics();
		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
	}

	@After
	public void tearDown() {

		TestSupport.resetIntervalPhysics();
		TestSupport.resetPhysicsRegistries();
		TestSupport.clearFakeGraphics();
	}

	private static File mapFile(String name) {

		File shared = new File("../assets/xml/" + name);
		if (shared.isFile()) return shared;
		File local = new File("assets/xml/" + name);
		if (local.isFile()) return local;
		throw new AssertionError("no map " + name + " next to the " + name + " test");
	}

	private static String mapText(String name) throws Exception {

		InputStream in = new FileInputStream(mapFile(name));
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte [] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
			return new String(out.toByteArray(), "UTF-8");
		} finally {
			in.close();
		}
	}

	/** Drops the engines and components that need a GL context or real media files. */
	private static String withoutMediaEngines(String xml, boolean keepTouch) {

		String stripped = xml;
		for (String engine : MEDIA_ENGINES) {
			if (keepTouch && engine.contentEquals("DirectTouches")) continue;
			stripped = stripped.replaceAll(
					"(?s)<direct[^>]*engineName\\s*=\\s*\"" + engine + "\".*?</direct>", "");
		}
		for (String tag : MEDIA_COMPONENTS) {
			//the real touch areas are the visible half of player control
			if (keepTouch && tag.contentEquals("d_touch")) continue;
			stripped = stripped.replaceAll("(?s)<" + tag + "\\b[^>]*/>", "");
			stripped = stripped.replaceAll("(?s)<" + tag + "\\b[^>]*>.*?</" + tag + ">", "");
		}
		return stripped;
	}

	private void parseMap(String name) throws Exception {

		parseMap(name, false);
	}

	private void parseMap(String name, boolean keepTouch) throws Exception {

		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
		scheduler = Engine.enginesScheduler;

		XmlPullParser parser = TestSupport.xmlParser(withoutMediaEngines(mapText(name), keepTouch));
		new MapParser(storage, enginesManager, null).parseMap(parser);
	}

	private Entity entityOf(String name) {

		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(name)) return entity;
		}
		throw new AssertionError("no entity named " + name);
	}

	private <T> T logicalOf(Entity entity, Class <T> type) {

		for (Object logical : entity.lstLogicals) {
			if (type.isInstance(logical)) return type.cast(logical);
		}
		return null;
	}

	private PhysicalCircle circleOf(String name) {

		for (PhysicalInterface physical : entityOf(name).lstPhysicals) {
			if (physical instanceof PhysicalCircle) return (PhysicalCircle) physical;
		}
		throw new AssertionError("no circle entity named " + name);
	}

	private TouchableArea touchAreaOf(String name) {

		for (InputTouchInterface touch : entityOf(name).lstTouches) {
			if (touch instanceof TouchableArea) return (TouchableArea) touch;
		}
		throw new AssertionError("no touchable area on " + name);
	}

	private LogicalField fieldOf() {

		for (Entity entity : storage.lstEntities) {
			LogicalField field = logicalOf(entity, LogicalField.class);
			if (field != null) return field;
		}
		throw new AssertionError("no field in the map");
	}

	private LogicalPlayer playerOf(String name) {

		for (Entity entity : storage.lstEntities) {
			for (Object logical : entity.lstLogicals) {
				if (logical instanceof LogicalPlayer
						&& name.equals(((LogicalPlayer) logical).name.value)) {
					return (LogicalPlayer) logical;
				}
			}
		}
		throw new AssertionError("no player named " + name);
	}

	private HandlerPlayers playersHandlerOf() {

		for (ComponentsHandler handler : enginesManager.lstHandlers) {
			if (handler instanceof HandlerPlayers) return (HandlerPlayers) handler;
		}
		throw new AssertionError("no players handler in the map");
	}

	private HandlerUnitClasses classesHandlerOf() {

		for (ComponentsHandler handler : enginesManager.lstHandlers) {
			if (handler instanceof HandlerUnitClasses) return (HandlerUnitClasses) handler;
		}
		return null;
	}

	/**
	 * The reserve pieces are placed by the respawners over the first frames; waits until the board
	 * comes to rest so a test starts from the same state as a player's first turn.
	 */
	private void settleSpawnedBoard() {

		for (int i = 0; i < 10; i++) scheduler.overDraw();

		LogicalField board = fieldOf();
		int frames = 0;
		while (frames < 600 && board.gotMovingUnits.value) {
			scheduler.overDraw();
			frames++;
		}
		assertFalse("the respawners left the board moving for " + (frames + 10) + " frame(s)",
				board.gotMovingUnits.value);
	}

	/** Runs the scheduler until the field reports that every checker stopped; returns the frame count. */
	private int settleBoard(int maxFrames) {

		LogicalField board = fieldOf();
		int frames = 0;
		while (frames < maxFrames && board.gotMovingUnits.value) {
			scheduler.overDraw();
			frames++;
		}
		return frames;
	}

	@Test
	public void checkersTreeShotThatStaysOnTheBoardClosesTheTurn() throws Exception {

		parseMap("checkers_tree.xml");
		settleSpawnedBoard();

		final int maxFrames = 3_000;

		PhysicalCircle shooter = circleOf(SHOOTER);
		LogicalField board = fieldOf();
		LogicalPlayer p1 = playerOf("p1");
		LogicalPlayer p2 = playerOf("p2");

		assertEquals(0.1, ru.framework2d.physics.IntervalPhysics.PHYSICAL_UNIT, 0.00000001);
		assertFalse("the board must start settled", board.gotMovingUnits.value);
		assertFalse(p1.turnActionPending);

		//a drag shot straight up the board, weak enough to stop before it reaches anyone
		shooter.impulse.set(0f, 0.4f);
		shooter.impulse.commit(null, entityOf(SHOOTER));

		assertTrue("the shot must set the checker in motion", shooter.movement.currentSpeed.value > 0d);
		assertTrue("the shot must open the turn action of its owner", p1.turnActionPending);

		int frames = settleBoard(maxFrames);
		String state = "shooter pos = (" + shooter.position.getX() + ", " + shooter.position.getY()
				+ "), shooter speed = " + shooter.movement.currentSpeed.value
				+ ", p1.gotAccess = " + p1.gotAccess.value
				+ ", p2.gotAccess = " + p2.gotAccess.value + ";";

		assertFalse("the board never reported that the checkers settled within " + frames
				+ " frame(s); " + state, board.gotMovingUnits.value);
		assertFalse("the acting checker must stop rolling; " + state, shooter.movement.isMoving());

		LogicalUnit movedUnit = logicalOf(entityOf(SHOOTER), LogicalUnit.class);
		assertTrue("the acting checker must stay on the board; " + state, movedUnit.inField.value);

		assertTrue("the turn must pass after a settled board with no checker leaving it; " + state,
				p2.gotAccess.value);
		assertFalse(p1.gotAccess.value);
		assertFalse(p1.turnActionPending);
	}

	/**
	 * The half of the hand over the player sees: the checkers of the player who got the turn must
	 * become clickable, the checkers of the player who lost it must stop being clickable.
	 *
	 * The maps mark the first player's checkers clickable and the second player's not clickable
	 * (the {@code isClikable} attribute of {@code d_touch}, the same context as {@code isControlled}
	 * of {@code ownership}). {@code isControlled} itself starts out true for every ownership, so the
	 * hand over must push the value to the touch areas instead of trusting that a change happened.
	 */
	@Test
	public void checkersTreeTurnHandsTheClickabilityToTheNextPlayer() throws Exception {

		parseMap("checkers_tree.xml", true);
		settleSpawnedBoard();

		final int maxFrames = 3_000;

		TouchableArea whiteArea = touchAreaOf(SHOOTER);
		TouchableArea blackArea = touchAreaOf("checkerblack1");
		LogicalOwnership blackOwnership =
				logicalOf(entityOf("checkerblack1"), LogicalOwnership.class);

		assertTrue("the map must start with the acting player's checkers clickable",
				whiteArea.isClikable.value);
		assertFalse("the map must start with the waiting player's checkers not clickable",
				blackArea.isClikable.value);

		PhysicalCircle shooter = circleOf(SHOOTER);
		LogicalPlayer p1 = playerOf("p1");
		LogicalPlayer p2 = playerOf("p2");

		shooter.impulse.set(0f, 0.4f);
		shooter.impulse.commit(null, entityOf(SHOOTER));
		int frames = settleBoard(maxFrames);

		String state = "frames = " + frames
				+ ", white clickability = " + whiteArea.isClikable.value
				+ ", black clickability = " + blackArea.isClikable.value
				+ ", black ownership controlled = " + blackOwnership.isControlled.value
				+ ", p2.gotAccess = " + p2.gotAccess.value + ";";

		assertFalse("the board must have settled; " + state, fieldOf().gotMovingUnits.value);
		assertTrue("the turn must have passed to the other player; " + state, p2.gotAccess.value);

		assertTrue("the checkers of the player who got the turn must be clickable; " + state,
				blackArea.isClikable.value);
		assertFalse("the checkers of the player who lost the turn must not be clickable; " + state,
				whiteArea.isClikable.value);
	}

	@Test
	public void bothCheckerMapsWireTheTurnPath() throws Exception {

		checkTurnWiringOf("checkers_tree.xml");
		checkTurnWiringOf("checkers_tree_hq.xml");
	}

	private void checkTurnWiringOf(String mapName) throws Exception {

		parseMap(mapName);
		settleSpawnedBoard();

		LogicalField board = fieldOf();
		HandlerPlayers players = playersHandlerOf();

		//the settled board is the only settlement signal the maps use
		for (LogicalPlayer player : players.lstPlayers) {
			assertSame(mapName + ": " + player.name.value
					+ " must share one context with board.gotMovingUnits",
					board.gotMovingUnits.getContext(), player.isInProgress.getContext());
		}

		//every checker on the desk is a class unit owned by a resolved player
		HandlerUnitClasses classes = classesHandlerOf();
		assertNotNull(mapName + ": no unit classes handler; the class turn rules are not in this map",
				classes);
		assertSame(mapName + ": the class turn rules must drive HandlerPlayers",
				classes, players.turnRules);

		int units = 0;
		for (Entity entity : storage.lstEntities) {
			if (entity.isPrototype) continue;

			LogicalUnit unit = logicalOf(entity, LogicalUnit.class);
			if (unit == null) continue;
			units++;

			assertNotNull(entity.name + " of " + mapName + " has no unit class component",
					logicalOf(entity, UnitClass.class));

			LogicalOwnership ownership = logicalOf(entity, LogicalOwnership.class);
			assertNotNull(entity.name + " of " + mapName + " has no ownership", ownership);
			assertNotNull(entity.name + " of " + mapName + " is not owned by a known player",
					ownership.owner);
		}

		assertTrue(mapName + ": no checker unit was placed on the board", units > 0);
	}
}
