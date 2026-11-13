package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.logic.classes.UnitClass;
import ru.framework2d.logic.fields.LogicalField;
import ru.framework2d.logic.fields.LogicalUnit;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.logic.players.LogicalPlayer;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.testsupport.TestSupport;

/**
 * A turn must close after the acting checker comes to rest, even when nothing left the board.
 *
 * The shipped checker maps settle a turn through one signal only: {@code field.gotMovingUnits},
 * aliased to {@code isInProgress} of the players (see assets/xml/checkers_tree.xml). So the acting
 * player keeps the turn until the field reports that every unit stopped moving. A shot must
 * therefore hand the turn over as soon as the board settles, whether the checker stopped on the
 * board, or bounced around on it, and no unit needs to leave the battlefield.
 */
public class MapTurnClosesWhenNoCheckerExitsTest {

	private static final String MAP =
			"<map version = \"1.0\" name = \"turn closes without exit\" parallel = \"false\">"
			+ "<direct package = \"ru.framework2d.physics\" engineName = \"IntervalPhysics\""
			+ " ph_unit = \"DisplayWidth/10240\" interval_limit = \"70\" impulse_limit = \"5\">"
			+ "<handler subpackage = \"collisions\" engineName = \"HandlerShapesCollisions\">"
			+ "<interaction subpackage = \"circles\" engineName = \"InteractionCircles\"/>"
			+ "<context name = \"impulse\" value = \"directImpulse\"/>"
			+ "<context name = \"isEnabled\" value = \"exist\"/>"
			+ "</handler>"
			+ "<handler subpackage = \"medium\" engineName = \"HandlerMediums\">"
			+ "<interaction engineName = \"InteractionMediumBody\"/>"
			+ "<context name = \"isEnabled\" value = \"exist\"/>"
			+ "</handler>"
			+ "</direct>"
			+ "<direct package = \"ru.framework2d.logic\" engineName = \"DirectLogicStandart\">"
			+ "<handler subpackage = \"fields\" engineName = \"HandlerFields\">"
			+ "<context name = \"inField\" value = \"exist\"/>"
			+ "</handler>"
			+ "<handler subpackage = \"players\" engineName = \"HandlerPlayers\">"
			+ "<context name = \"isCurrentlyControlled\" value = \"isSelected\"/>"
			+ "<context name = \"isControlled\" value = \"isClikable\"/>"
			+ "<context name = \"isExist\" value = \"exist\"/>"
			+ "<context name = \"finishedRoundTrigger\" value = \"finishRound\"/>"
			+ "</handler>"
			+ "<handler subpackage = \"classes\" engineName = \"HandlerUnitClasses\">"
			+ "<context name = \"actionStarted\" value = \"directImpulse\"/>"
			+ "<context name = \"wasAttacked\" value = \"collision\"/>"
			+ "<context name = \"remainsOnBoard\" value = \"exist\"/>"
			+ "</handler>"
			+ "</direct>"
			+ "<entity name = \"board\" single = \"true\">"
			+ "<surface position:x = \"0\" position:y = \"0\" size:x = \"100\" size:y = \"100\""
			+ " slowingEffect = \"0.15\"/>"
			+ "<field name = \"board\" position:x = \"0\" position:y = \"0\""
			+ " size:x = \"100\" size:y = \"100\">"
			+ "<context name = \"gotMovingUnits\" value = \"isInProgress\"/>"
			+ "</field>"
			+ "<player name = \"p1\" gotAccess = \"true\" isWaitForTurnOver = \"true\""
			+ " isGetTurnIfWon = \"true\" wonRounds = \"0\" startPoints = \"0\""
			+ " pointsGaveForRoundVictory = \"1\" pointsNeedToVictory = \"6\"/>"
			+ "<player name = \"p2\" gotAccess = \"false\" isWaitForTurnOver = \"true\""
			+ " isGetTurnIfWon = \"true\" wonRounds = \"7\" startPoints = \"7\""
			+ " pointsGaveForRoundVictory = \"-1\" pointsLostForRoundLose = \"-1\"/>"
			+ "<entity name = \"white\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"-20\" radius = \"4\" weight = \"0.1\"/>"
			+ "<body roughness = \"0.1\"/>"
			+ "<unit fieldName = \"board\"/>"
			+ "<ownership ownerName = \"p1\"/>"
			+ "<infantry/>"
			+ "</entity>"
			+ "<entity name = \"black\" single = \"true\">"
			+ "<circle position:x = \"0\" position:y = \"0\" radius = \"4\" weight = \"0.1\"/>"
			+ "<body roughness = \"0.1\"/>"
			+ "<unit fieldName = \"board\"/>"
			+ "<ownership ownerName = \"p2\"/>"
			+ "<infantry/>"
			+ "</entity>"
			+ "</entity>"
			+ "</map>";

	private static final int MAX_FRAMES = 3_000;

	private EntityStorage storage;
	private EnginesManager enginesManager;
	private EnginesScheduler scheduler;

	private LogicalField field;
	private LogicalPlayer p1;
	private LogicalPlayer p2;
	private LogicalUnit whiteUnit;
	private LogicalUnit blackUnit;
	private UnitClass whiteClass;
	private PhysicalCircle white;
	private PhysicalCircle black;

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

	private void parseMap() {

		storage = TestSupport.bootstrapEngine();
		enginesManager = Engine.enginesManager;
		scheduler = Engine.enginesScheduler;

		try {
			XmlPullParser parser = TestSupport.xmlParser(MAP);
			new MapParser(storage, enginesManager, null).parseMap(parser);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		field = logicalOf("board", LogicalField.class, "no field in the map");
		p1 = playerOf("p1");
		p2 = playerOf("p2");
		whiteUnit = logicalOf("white", LogicalUnit.class, "no unit named white");
		blackUnit = logicalOf("black", LogicalUnit.class, "no unit named black");
		whiteClass = logicalOf("white", UnitClass.class, "no unit class on white");
		white = circleOf("white");
		black = circleOf("black");
	}

	private Entity entityOf(String name) {

		for (Entity entity : storage.lstEntities) {
			if (entity.name.equals(name)) return entity;
		}
		throw new AssertionError("no entity named " + name);
	}

	@SuppressWarnings("unchecked")
	private <T> T logicalOf(String entityName, Class <T> type, String message) {

		for (Object logical : entityOf(entityName).lstLogicals) {
			if (type.isInstance(logical)) return (T) logical;
		}
		throw new AssertionError(message);
	}

	private LogicalPlayer playerOf(String name) {

		for (Object logical : entityOf("board").lstLogicals) {
			if (logical instanceof LogicalPlayer
					&& name.equals(((LogicalPlayer) logical).name.value)) {
				return (LogicalPlayer) logical;
			}
		}
		throw new AssertionError("no player named " + name);
	}

	private PhysicalCircle circleOf(String name) {

		for (PhysicalInterface physical : entityOf(name).lstPhysicals) {
			if (physical instanceof PhysicalCircle) return (PhysicalCircle) physical;
		}
		throw new AssertionError("no circle entity named " + name);
	}

	private String trace(PhysicalCircle shooter) {

		return "shooter speed = " + String.format("%.6f", shooter.movement.currentSpeed.value)
				+ ", spin = " + String.format("%.6f", shooter.movement.alphaRotation.value)
				+ ", shooter pos = (" + String.format("%.2f", shooter.position.getX())
				+ ", " + String.format("%.2f", shooter.position.getY()) + ")"
				+ ", other pos = (" + String.format("%.2f", otherOf(shooter).position.getX())
				+ ", " + String.format("%.2f", otherOf(shooter).position.getY()) + ")"
				+ ", gotMovingUnits = " + field.gotMovingUnits.value
				+ ", p1.inProgress = " + p1.isInProgress.value
				+ ", p1.pending = " + p1.turnActionPending
				+ ", p2.pending = " + p2.turnActionPending
				+ ", p2.inProgress = " + p2.isInProgress.value
				+ ", p2.gotAccess = " + p2.gotAccess.value + ";";
	}

	private PhysicalCircle otherOf(PhysicalCircle shooter) {
		return shooter == white ? black : white;
	}

	/**
	 * The shot signal the touch handler commits for a drag shot: the direction is aliased to the
	 * physics impulse and to the class action. The turn must close on this signal alone, without the
	 * selection release that SubPointer commits next to it.
	 */
	private void fireShot(PhysicalCircle shooter, float ix, float iy) {

		shooter.impulse.set(ix, iy);
		shooter.impulse.commit(null, shooter.entity);
	}

	/** The release of the selected area, SubPointer commits right next to the shot. */
	private void releaseSelection(LogicalOwnership ownership) {

		ownership.isCurrentlyControlled.value = true;
		ownership.isCurrentlyControlled.commit(null, ownership.entity);

		ownership.isCurrentlyControlled.value = false;
		ownership.isCurrentlyControlled.commit(null, ownership.entity);
	}

	private int runUntilSettled(StringBuilder log, PhysicalCircle shooter, int maxFrames) {

		int frames = 0;
		while (frames < maxFrames && field.gotMovingUnits.value) {
			scheduler.overDraw();
			frames++;
			if (frames % 60 == 0) {
				log.append("frame ").append(frames).append(": ").append(trace(shooter)).append('\n');
			}
		}
		log.append("settled at frame ").append(frames).append(": ").append(trace(shooter)).append('\n');
		System.out.println("[diag] " + log.toString().replace('\n', '|'));
		return frames;
	}

	private String logOf(StringBuilder log) {
		return log.toString().replace('\n', '|');
	}

	private void assertTurnPassedAfterSettling(StringBuilder log, PhysicalCircle shooter) {

		int frames = runUntilSettled(log, shooter, MAX_FRAMES);

		assertFalse("the board never reported that the units settled within " + frames
				+ " frame(s), so the turn could not close (see [diag] output):\n" + logOf(log),
				field.gotMovingUnits.value);
		assertFalse("the acting checker must stop moving (see [diag] output):\n" + logOf(log),
				shooter.movement.isMoving());

		assertTrue("no player gained control after the shot settled (see [diag] output):\n"
				+ logOf(log), p2.gotAccess.value);
		assertFalse(p1.gotAccess.value);
		assertTrue(p2.isInProgress.value);
		assertFalse(p1.isInProgress.value);
	}

	@Test
	public void shotThatHitsNothingStillClosesTheTurn() {

		parseMap();

		assertEquals(0.1, IntervalPhysics.PHYSICAL_UNIT, 0.00000001);
		assertSame("the map must merge the shot impulse context with the class action context",
				white.impulse.getContext(), whiteClass.actionStarted.getContext());

		//weak enough to stop on the board, and pointed away from the other checker
		StringBuilder log = new StringBuilder();
		log.append("start: ").append(trace(white)).append('\n');
		fireShot(white, 0f, -0.2f);

		assertTrue("the shot must set the checker in motion", white.movement.currentSpeed.value > 0d);
		assertTrue("the shot must open the acting player's turn action", p1.turnActionPending);
		assertTrue("the field must see the moving checker", field.gotMovingUnits.value);

		assertTurnPassedAfterSettling(log, white);

		assertTrue("the acting checker must stay on the board (see [diag] output):\n" + logOf(log),
				whiteUnit.inField.value);
		assertTrue("the idle checker must stay on the board (see [diag] output):\n" + logOf(log),
				blackUnit.inField.value);
		assertEquals(0, whiteClass.enemiesDestroyed.value);
		assertEquals(0d, black.position.getY(), 1d);
	}

	@Test
	public void shotAndSelectionReleaseCloseTheTurnOnce() {

		parseMap();

		StringBuilder log = new StringBuilder();
		log.append("start: ").append(trace(white)).append('\n');

		//what the drag gesture really produces: the shot first, then the release of the selected area
		fireShot(white, 0f, -0.2f);
		releaseSelection(logicalOf("white", LogicalOwnership.class, "no ownership on white"));

		assertTurnPassedAfterSettling(log, white);

		assertTrue("only the settled board may rotate the turn once (see [diag] output):\n"
				+ logOf(log), p2.gotAccess.value);
		assertFalse(p1.turnActionPending);
		assertFalse(p2.turnActionPending);
	}

	@Test
	public void contactThatKeepsBothCheckersOnBoardStillClosesTheTurn() {

		parseMap();

		assertSame("the map must merge the shot impulse context with the class action context",
				white.impulse.getContext(), whiteClass.actionStarted.getContext());

		//straight into the neighbour checker, hard enough to move both of them
		StringBuilder log = new StringBuilder();
		log.append("start: ").append(trace(white)).append('\n');
		fireShot(white, 0f, 0.6f);

		assertTrue("the shot must set the checker in motion", white.movement.currentSpeed.value > 0d);

		int frames = 0;
		boolean contacted = false;
		while (frames < MAX_FRAMES && !contacted) {
			scheduler.overDraw();
			frames++;
			contacted = black.movement.currentSpeed.value > 0d;
		}
		log.append("contact at frame ").append(frames).append(": ")
				.append(trace(white)).append('\n');
		assertTrue("the shot never reached the neighbour checker (see [diag] output):\n"
				+ logOf(log), contacted);

		assertTurnPassedAfterSettling(log, white);

		assertTrue("the acting checker must stay on the board (see [diag] output):\n" + logOf(log),
				whiteUnit.inField.value);
		assertTrue("the struck checker must stay on the board (see [diag] output):\n" + logOf(log),
				blackUnit.inField.value);
	}
}
