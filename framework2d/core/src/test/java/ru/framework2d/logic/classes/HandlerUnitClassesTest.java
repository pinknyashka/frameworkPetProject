package ru.framework2d.logic.classes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Text;
import ru.framework2d.data.Vector2;
import ru.framework2d.logic.fields.LogicalField;
import ru.framework2d.logic.fields.LogicalUnit;
import ru.framework2d.logic.grids.CellGrid;
import ru.framework2d.logic.grids.LogicalGrid;
import ru.framework2d.logic.players.HandlerPlayers;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.logic.players.LogicalPlayer;
import ru.framework2d.testsupport.TestSupport;

/**
 * Class rules around the infantry action window (phase 2 of plans/turn-based-logics-plan.md).
 *
 * The map normally merges the UnitClass signal fields with touch/physics/field contexts. These
 * tests build the same context identity by hand and then send the same data objects that the
 * engine would commit.
 */
public class HandlerUnitClassesTest {

	private HandlerPlayers players;
	private HandlerUnitClasses classes;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();

		players = new HandlerPlayers();
		classes = new HandlerUnitClasses();
		classes.installInto(players);
	}

	@After
	public void tearDown() {
		classes = null;
		players = null;
		TestSupport.resetIntervalPhysics();
	}

	private LogicalPlayer player(String name) {
		TagOptions options = new TagOptions(LogicalPlayer.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("name", new Text(name)));
		return (LogicalPlayer) players.createComponent(new Entity(name), options);
	}

	private LogicalOwnership ownership(Entity entity, String ownerName) {
		TagOptions options = new TagOptions(LogicalOwnership.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("ownerName", new Text(ownerName)));
		return (LogicalOwnership) players.createComponent(entity, options);
	}

	private UnitClass infantry(Entity entity) {
		return (UnitClass) classes.createComponent(entity, new TagOptions(Infantry.SHORT_CLASS_NAME));
	}

	private UnitClass cavalry(Entity entity) {
		return (UnitClass) classes.createComponent(entity, new TagOptions(Cavalry.SHORT_CLASS_NAME));
	}

	private UnitClass minigun(Entity entity) {
		return (UnitClass) classes.createComponent(entity, new TagOptions(Minigun.SHORT_CLASS_NAME));
	}

	private <T extends DataInterface> T signal(T value, DataInterface field) {
		value.setContext(field.getContext());
		return value;
	}

	private void shareBoardContext(UnitClass unit, LogicalOwnership ownership) {
		unit.remainsOnBoard.setContext(ownership.isExist.getContext());
	}

	@Test
	public void handlerReplacesTheDefaultSequentialRule() {
		assertSame(classes, players.turnRules);
		assertEquals("class-abilities", classes.rulesName());
	}

	@Test
	public void actionStartedMarksTheActingUnitAndOpensTheAction() {
		player("p1");
		player("p2");

		Entity white = new Entity("white");
		LogicalOwnership whiteOwnership = ownership(white, "p1");
		UnitClass whiteClass = infantry(white);
		shareBoardContext(whiteClass, whiteOwnership);

		assertTrue(classes.transferData(white, signal(new Vector2(1, 2), whiteClass.actionStarted)));

		assertTrue(classes.actionOpen);
		assertSame(whiteClass, classes.lastActedUnit);
		assertEquals("p1", classes.actionOwnerName);
		assertTrue(whiteClass.hasActedThisTurn);
		assertFalse(whiteClass.eligibleToAct);
		assertTrue(whiteClass.didAttack.value);
		assertEquals(0, whiteClass.enemiesDestroyed.value);
		assertSame(whiteClass, classes.findClass(white));
	}

	@Test
	public void anEnemyExitDuringTheActionCountsOneKill() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		Entity black = new Entity("black");
		LogicalOwnership whiteOwnership = ownership(white, p1.name.value);
		LogicalOwnership blackOwnership = ownership(black, "p2");
		UnitClass whiteClass = infantry(white);
		UnitClass blackClass = infantry(black);
		shareBoardContext(whiteClass, whiteOwnership);
		shareBoardContext(blackClass, blackOwnership);

		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));
		assertTrue(classes.transferData(black, signal(new Bool(false), blackClass.remainsOnBoard)));

		assertEquals(1, whiteClass.enemiesDestroyed.value);
		assertFalse(blackClass.remainsOnBoard.value);
	}

	@Test
	public void friendlyExitDuringTheActionDoesNotCountAKill() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		Entity friendly = new Entity("friendly");
		LogicalOwnership whiteOwnership = ownership(white, p1.name.value);
		LogicalOwnership friendlyOwnership = ownership(friendly, p1.name.value);
		UnitClass whiteClass = infantry(white);
		UnitClass friendlyClass = infantry(friendly);
		shareBoardContext(whiteClass, whiteOwnership);
		shareBoardContext(friendlyClass, friendlyOwnership);

		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));
		classes.transferData(friendly, signal(new Bool(false), friendlyClass.remainsOnBoard));

		assertEquals(0, whiteClass.enemiesDestroyed.value);
		assertFalse(friendlyClass.remainsOnBoard.value);
	}

	@Test
	public void infantryKeepsTheTurnOnlyForUnactedAdjacentUnits() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		Entity adjacent = new Entity("adjacent");
		Entity far = new Entity("far");
		LogicalOwnership whiteOwnership = ownership(white, p1.name.value);
		LogicalOwnership adjacentOwnership = ownership(adjacent, p1.name.value);
		LogicalOwnership farOwnership = ownership(far, p1.name.value);

		UnitClass whiteClass = infantry(white);
		UnitClass adjacentClass = infantry(adjacent);
		UnitClass farClass = infantry(far);
		shareBoardContext(whiteClass, whiteOwnership);
		shareBoardContext(adjacentClass, adjacentOwnership);
		shareBoardContext(farClass, farOwnership);

		whiteClass.cellColumn = 3;
		whiteClass.cellRow = 4;
		adjacentClass.cellColumn = 4;
		adjacentClass.cellRow = 4;
		farClass.cellColumn = 5;
		farClass.cellRow = 4;

		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity enemy = new Entity("enemy");
		LogicalOwnership enemyOwnership = ownership(enemy, "p2");
		UnitClass enemyClass = infantry(enemy);
		shareBoardContext(enemyClass, enemyOwnership);
		classes.transferData(enemy, signal(new Bool(false), enemyClass.remainsOnBoard));

		assertSame(p1, classes.keepTurnPlayer(p1, players.lstPlayers));
		assertEquals(1, classes.activationCandidates.size());
		assertSame(adjacentClass, classes.activationCandidates.get(0));
	}

	@Test
	public void infantryDoesNotKeepTheTurnWithoutACandidate() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		Entity far = new Entity("far");
		LogicalOwnership whiteOwnership = ownership(white, p1.name.value);
		LogicalOwnership farOwnership = ownership(far, p1.name.value);
		UnitClass whiteClass = infantry(white);
		UnitClass farClass = infantry(far);
		shareBoardContext(whiteClass, whiteOwnership);
		shareBoardContext(farClass, farOwnership);

		whiteClass.cellColumn = 0;
		whiteClass.cellRow = 0;
		farClass.cellColumn = 5;
		farClass.cellRow = 5;

		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity enemy = new Entity("enemy");
		LogicalOwnership enemyOwnership = ownership(enemy, "p2");
		UnitClass enemyClass = infantry(enemy);
		shareBoardContext(enemyClass, enemyOwnership);
		classes.transferData(enemy, signal(new Bool(false), enemyClass.remainsOnBoard));

		assertNull(classes.keepTurnPlayer(p1, players.lstPlayers));
		assertTrue(classes.activationCandidates.isEmpty());
	}

	@Test
	public void keptInfantryTurnSwitchesOffNonCandidateUnits() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		Entity adjacent = new Entity("adjacent");
		Entity far = new Entity("far");
		LogicalOwnership whiteOwnership = ownership(white, p1.name.value);
		LogicalOwnership adjacentOwnership = ownership(adjacent, p1.name.value);
		LogicalOwnership farOwnership = ownership(far, p1.name.value);
		adjacentOwnership.isControlled.value = true;
		farOwnership.isControlled.value = true;

		UnitClass whiteClass = infantry(white);
		UnitClass adjacentClass = infantry(adjacent);
		UnitClass farClass = infantry(far);
		shareBoardContext(whiteClass, whiteOwnership);
		shareBoardContext(adjacentClass, adjacentOwnership);
		shareBoardContext(farClass, farOwnership);

		whiteClass.cellColumn = 0;
		whiteClass.cellRow = 0;
		adjacentClass.cellColumn = 1;
		adjacentClass.cellRow = 0;
		farClass.cellColumn = 7;
		farClass.cellRow = 0;

		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity enemy = new Entity("enemy");
		LogicalOwnership enemyOwnership = ownership(enemy, "p2");
		UnitClass enemyClass = infantry(enemy);
		shareBoardContext(enemyClass, enemyOwnership);
		classes.transferData(enemy, signal(new Bool(false), enemyClass.remainsOnBoard));

		assertSame(p1, classes.keepTurnPlayer(p1, players.lstPlayers));
		classes.onTurnStarted(p1);

		assertTrue(adjacentClass.eligibleToAct);
		assertFalse(whiteClass.eligibleToAct);
		assertFalse(farClass.eligibleToAct);
		assertTrue(adjacentOwnership.isControlled.value);
		assertFalse(farOwnership.isControlled.value);
	}

	@Test
	public void cavalryMoveWithoutContactKeepsTheTurnForTheSameUnit() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity horse = new Entity("horse");
		ownership(horse, p1.name.value);
		UnitClass horseClass = cavalry(horse);

		horseClass.cellColumn = 0;
		horseClass.cellRow = 0;

		classes.transferData(horse, signal(new Vector2(), horseClass.actionStarted));

		assertSame(p1, classes.keepTurnPlayer(p1, players.lstPlayers));
		assertEquals(1, classes.activationCandidates.size());
		assertSame(horseClass, classes.activationCandidates.get(0));
		assertFalse(horseClass.movementOnlyNextAction);
	}

	@Test
	public void cavalryThreeKillAttackGrantsMovementOnlyFollowUp() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity horse = new Entity("horse");
		ownership(horse, p1.name.value);
		UnitClass horseClass = cavalry(horse);

		classes.transferData(horse, signal(new Vector2(), horseClass.actionStarted));

		for (int i = 0; i < 3; i++) {
			Entity enemy = new Entity("enemy" + i);
			LogicalOwnership enemyOwnership = ownership(enemy, "p2");
			UnitClass enemyClass = infantry(enemy);
			shareBoardContext(enemyClass, enemyOwnership);
			classes.transferData(enemy, signal(new Bool(false), enemyClass.remainsOnBoard));
		}

		assertSame(p1, classes.keepTurnPlayer(p1, players.lstPlayers));
		assertEquals(3, horseClass.enemiesDestroyed.value);
		assertTrue(horseClass.movementOnlyNextAction);
		assertEquals(1, classes.activationCandidates.size());
		assertSame(horseClass, classes.activationCandidates.get(0));
	}

	@Test
	public void cavalryMovementOnlyFollowUpAllowsTheSameUnitToAttackAgain() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity horse = new Entity("horse");
		ownership(horse, p1.name.value);
		UnitClass horseClass = cavalry(horse);
		horseClass.movementOnlyNextAction = true;

		classes.transferData(horse, signal(new Vector2(), horseClass.actionStarted));

		assertSame(p1, classes.keepTurnPlayer(p1, players.lstPlayers));
		assertFalse(horseClass.movementOnlyNextAction);
		assertEquals(1, classes.activationCandidates.size());
		assertSame(horseClass, classes.activationCandidates.get(0));
	}

	@Test
	public void cavalryMovementOnlyFollowUpFailsIfItContactsSomebody() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity horse = new Entity("horse");
		ownership(horse, p1.name.value);
		UnitClass horseClass = cavalry(horse);
		horseClass.movementOnlyNextAction = true;

		classes.transferData(horse, signal(new Vector2(), horseClass.actionStarted));
		classes.transferData(horse, signal(new Vector2(), horseClass.wasAttacked));

		assertNull(classes.keepTurnPlayer(p1, players.lstPlayers));
		assertFalse(horseClass.movementOnlyNextAction);
		assertTrue(classes.activationCandidates.isEmpty());
	}

	@Test
	public void minigunKeepsTheTurnAfterTwoKillsWithoutAdjacentGate() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity gun = new Entity("gun");
		ownership(gun, p1.name.value);
		UnitClass gunClass = minigun(gun);

		classes.transferData(gun, signal(new Vector2(), gunClass.actionStarted));

		for (int i = 0; i < 2; i++) {
			Entity enemy = new Entity("enemy" + i);
			LogicalOwnership enemyOwnership = ownership(enemy, "p2");
			UnitClass enemyClass = infantry(enemy);
			shareBoardContext(enemyClass, enemyOwnership);
			classes.transferData(enemy, signal(new Bool(false), enemyClass.remainsOnBoard));
		}

		assertSame(p1, classes.keepTurnPlayer(p1, players.lstPlayers));
		assertEquals(2, gunClass.enemiesDestroyed.value);
		assertTrue(classes.activationCandidates.isEmpty());
	}

	@Test
	public void hitDoesNotConsumeDurabilityOrRemoveUnit() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		ownership(white, p1.name.value);
		UnitClass whiteClass = infantry(white);
		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity gun = new Entity("gun");
		ownership(gun, "p2");
		UnitClass gunClass = minigun(gun);
		gunClass.durabilitySpent = gunClass.hitsToDestroy.value - 1;

		classes.transferData(gun, signal(new Vector2(), gunClass.wasAttacked));

		assertFalse(gunClass.destroyed);
		assertTrue(gunClass.remainsOnBoard.value);
		assertEquals(gunClass.hitsToDestroy.value - 1, gunClass.durabilitySpent);
		assertEquals(0, whiteClass.enemiesDestroyed.value);
	}

	@Test
	public void minigunSurvivableExitReturnsToTheDeskAndCountsAKnock() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		ownership(white, p1.name.value);
		UnitClass whiteClass = infantry(white);
		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity gun = new Entity("gun");
		LogicalOwnership gunOwnership = ownership(gun, "p2");
		UnitClass gunClass = minigun(gun);
		shareBoardContext(gunClass, gunOwnership);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertEquals(1, whiteClass.enemiesDestroyed.value);
		assertTrue(gunClass.remainsOnBoard.value);
		assertTrue(gunClass.lastKnownOnBoard);
		assertFalse(gunClass.destroyed);
		assertEquals(1, gunClass.durabilitySpent);
		assertTrue(gunOwnership.isExist.value);
	}

	@Test
	public void minigunFinalDeskExitIsNotReturnedAndCountsAKill() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		ownership(white, p1.name.value);
		UnitClass whiteClass = infantry(white);
		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity gun = new Entity("gun");
		LogicalOwnership gunOwnership = ownership(gun, "p2");
		UnitClass gunClass = minigun(gun);
		shareBoardContext(gunClass, gunOwnership);
		gunClass.durabilitySpent = gunClass.hitsToDestroy.value - 1;

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertEquals(1, whiteClass.enemiesDestroyed.value);
		assertFalse(gunClass.remainsOnBoard.value);
		assertFalse(gunClass.lastKnownOnBoard);
		assertTrue(gunClass.destroyed);
		assertEquals(gunClass.hitsToDestroy.value, gunClass.durabilitySpent);
		assertFalse(gunOwnership.isExist.value);
	}

	@Test
	public void minigunDurabilityIsConsumedByDeskExitsUntilTheFinalExit() {
		player("p1");
		player("p2");

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertTrue(gunClass.remainsOnBoard.value);
		assertEquals(1, gunClass.durabilitySpent);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertTrue(gunClass.remainsOnBoard.value);
		assertEquals(2, gunClass.durabilitySpent);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertFalse(gunClass.remainsOnBoard.value);
		assertTrue(gunClass.destroyed);
		assertEquals(3, gunClass.durabilitySpent);
	}

	@Test
	public void finalDeskExitStopsMovingAndLosesControlDuringTheAction() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		ownership(white, p1.name.value);
		UnitClass whiteClass = infantry(white);
		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity gun = new Entity("gun");
		LogicalOwnership gunOwnership = ownership(gun, "p2");
		UnitClass gunClass = minigun(gun);
		shareBoardContext(gunClass, gunOwnership);
		gunClass.durabilitySpent = gunClass.hitsToDestroy.value - 1;

		PhysicalCircle shape = new PhysicalCircle();
		shape.movement.set(1, 0, 0);
		shape.movement.currentSpeed.value = 10;
		gun.addComponent(shape);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertTrue(gunClass.destroyed);
		assertFalse(gunClass.remainsOnBoard.value);
		assertFalse(gunOwnership.isControlled.value);
		assertEquals(1, whiteClass.enemiesDestroyed.value);
		assertFalse(shape.movement.isMoving());
	}

	@Test
	public void minigunDeskExitsCountEachTimeTheCheckerIsKnockedOffTheDesk() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity white = new Entity("white");
		ownership(white, p1.name.value);
		UnitClass whiteClass = infantry(white);
		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));

		Entity gun = new Entity("gun");
		LogicalOwnership gunOwnership = ownership(gun, "p2");
		UnitClass gunClass = minigun(gun);
		shareBoardContext(gunClass, gunOwnership);

		for (int exit = 1; exit <= gunClass.hitsToDestroy.value; exit++) {
			classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));
			assertEquals(exit, whiteClass.enemiesDestroyed.value);
			assertEquals(exit, gunClass.durabilitySpent);
		}

		assertFalse(gunClass.remainsOnBoard.value);
		assertTrue(gunClass.destroyed);
		assertFalse(gunOwnership.isExist.value);
	}

	@Test
	public void destroyedUnitIsNotControlledWhenTurnStarts() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity gun = new Entity("gun");
		LogicalOwnership gunOwnership = ownership(gun, p1.name.value);
		UnitClass gunClass = minigun(gun);
		shareBoardContext(gunClass, gunOwnership);
		gunOwnership.isControlled.value = true;
		gunClass.destroyed = true;
		gunClass.remainsOnBoard.value = false;
		gunClass.lastKnownOnBoard = false;

		classes.onTurnStarted(p1);

		assertFalse(gunOwnership.isControlled.value);
		assertFalse(gunClass.eligibleToAct);
	}

	@Test
	public void destroyedUnitCannotStartAnAction() {
		player("p1");
		player("p2");

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);
		gunClass.destroyed = true;
		gunClass.remainsOnBoard.value = false;
		gunClass.lastKnownOnBoard = false;

		assertTrue(classes.transferData(gun, signal(new Vector2(), gunClass.actionStarted)));
		assertFalse(classes.actionOpen);
		assertNull(classes.lastActedUnit);
	}

	@Test
	public void roundFinishedResetsClassCombatState() {
		LogicalPlayer p1 = player("p1");
		Entity gun = new Entity("gun");
		ownership(gun, p1.name.value);
		UnitClass gunClass = minigun(gun);
		gunClass.durabilitySpent = 2;
		gunClass.exitProcessedThisAction = true;
		gunClass.hasReturnPosition = true;
		gunClass.destroyed = true;
		gunClass.contactedThisAction = true;
		gunClass.movementOnlyNextAction = true;
		gunClass.hasActedThisTurn = true;
		gunClass.eligibleToAct = false;
		gunClass.enemiesDestroyed.value = 3;

		classes.onRoundFinished(p1);

		assertEquals(0, gunClass.durabilitySpent);
		assertFalse(gunClass.exitProcessedThisAction);
		assertFalse(gunClass.hasReturnPosition);
		assertFalse(gunClass.destroyed);
		assertFalse(gunClass.contactedThisAction);
		assertFalse(gunClass.movementOnlyNextAction);
		assertFalse(gunClass.hasActedThisTurn);
		assertTrue(gunClass.eligibleToAct);
		assertEquals(0, gunClass.enemiesDestroyed.value);
	}

	@Test
	public void roundFinishedClearsActedFlagsFromInactiveUnitsAfterThePreviousRound() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity gun = new Entity("gun");
		ownership(gun, p1.name.value);
		UnitClass gunClass = minigun(gun);
		gunClass.destroyed = true;
		gunClass.remainsOnBoard.value = false;
		gunClass.lastKnownOnBoard = false;

		classes.onTurnStarted(p1);
		assertTrue(gunClass.hasActedThisTurn);
		assertFalse(gunClass.eligibleToAct);

		classes.onRoundFinished(p1);
		assertFalse(gunClass.hasActedThisTurn);
		assertTrue(gunClass.eligibleToAct);
	}

	@Test
	public void roundFinishedBeforeTurnStartKeepsRespawningUnitsUnacted() {
		LogicalPlayer p1 = player("p1");
		player("p2");

		Entity gun = new Entity("gun");
		ownership(gun, p1.name.value);
		UnitClass gunClass = minigun(gun);
		gunClass.destroyed = true;
		gunClass.remainsOnBoard.value = false;
		gunClass.lastKnownOnBoard = false;
		gunClass.hasActedThisTurn = true;
		gunClass.eligibleToAct = false;

		classes.onRoundFinished(p1);
		classes.onTurnStarted(p1);

		assertFalse(gunClass.hasActedThisTurn);
		assertTrue(gunClass.eligibleToAct);
	}

	@Test
	public void normalTurnResetsThePlayerClassFlags() {
		LogicalPlayer p1 = player("p1");
		LogicalPlayer p2 = player("p2");

		Entity black = new Entity("black");
		LogicalOwnership blackOwnership = ownership(black, p2.name.value);
		UnitClass blackClass = infantry(black);
		shareBoardContext(blackClass, blackOwnership);
		blackClass.hasActedThisTurn = true;
		blackClass.eligibleToAct = false;

		classes.onTurnStarted(p2);

		assertFalse(blackClass.hasActedThisTurn);
		assertTrue(blackClass.eligibleToAct);
	}

	@Test
	public void boardingUpdatesTheCurrentCellForAUnitThatWasAlreadyOnBoard() {
		player("p1");
		player("p2");

		LogicalGrid grid = addGrid(8, 8);
		CellGrid targetCell = grid.cells[5][3];

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);
		gunClass.position.x.value = targetCell.position.getX();
		gunClass.position.y.value = targetCell.position.getY();
		gunClass.lastKnownOnBoard = true;
		gunClass.cellColumn = 0;
		gunClass.cellRow = 0;

		classes.transferData(gun, signal(new Bool(true), gunClass.remainsOnBoard));

		assertEquals(5, gunClass.cellColumn);
		assertEquals(3, gunClass.cellRow);
	}

	@Test
	public void survivingDeskExitRestoresTheSavedReturnSpotWhenItIsFree() {
		player("p1");
		player("p2");

		LogicalGrid grid = addGrid(2, 1);
		CellGrid returnCell = grid.cells[0][0];

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);
		setReturnSpot(gunClass, returnCell.position.getX(), returnCell.position.getY());
		gunClass.position.x.value = 1000;
		gunClass.position.y.value = 1000;

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertTrue(gunClass.remainsOnBoard.value);
		assertEquals(0, gunClass.cellColumn);
		assertEquals(0, gunClass.cellRow);
		assertEquals(returnCell.position.getX(), gunClass.position.x.value, 0.001);
		assertEquals(returnCell.position.getY(), gunClass.position.y.value, 0.001);
	}

	@Test
	public void survivingDeskExitMovesToNearestFreeCellWhenReturnSpotIsOccupied() {
		player("p1");
		player("p2");

		LogicalGrid grid = addGrid(2, 1);
		CellGrid targetCell = grid.cells[0][0];
		CellGrid freeCell = grid.cells[1][0];

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);
		setReturnSpot(gunClass, targetCell.position.getX(), targetCell.position.getY());
		gunClass.position.x.value = 1000;
		gunClass.position.y.value = 1000;

		Entity occupant = new Entity("occupant");
		ownership(occupant, "p1");
		UnitClass occupantClass = infantry(occupant);
		occupantClass.position.x.value = targetCell.position.getX();
		occupantClass.position.y.value = targetCell.position.getY();
		occupantClass.cellColumn = 0;
		occupantClass.cellRow = 0;
		occupantClass.lastKnownOnBoard = true;

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertTrue(gunClass.remainsOnBoard.value);
		assertEquals(1, gunClass.cellColumn);
		assertEquals(0, gunClass.cellRow);
		assertEquals(freeCell.position.getX(), gunClass.position.x.value, 0.001);
		assertEquals(freeCell.position.getY(), gunClass.position.y.value, 0.001);
	}

	@Test
	public void survivingDeskExitKeepsLogicalUnitInsideTheFieldAfterReturn() {
		player("p1");
		player("p2");

		LogicalField field = addField(100, 100);

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);
		setReturnSpot(gunClass, 10, 10);
		gunClass.position.x.value = 1000;
		gunClass.position.y.value = 1000;

		LogicalUnit logicalUnit = new LogicalUnit();
		logicalUnit.inField.value = false;
		logicalUnit.lstFieldsToCheck.add(field);
		gun.addComponent(logicalUnit);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertTrue(gunClass.remainsOnBoard.value);
		assertTrue(logicalUnit.inField.value);
		assertEquals(10, gunClass.position.x.value, 0.001);
		assertEquals(10, gunClass.position.y.value, 0.001);
	}

	@Test
	public void successfulInfantryActionKeepsTheFollowUpMoveAvailableThroughHandlerPlayers() {
		LogicalPlayer p1 = player("p1");
		LogicalPlayer p2 = player("p2");
		p2.gotAccess.value = false;
		p2.isInProgress.value = false;

		Entity white = new Entity("white");
		Entity adjacent = new Entity("adjacent");
		Entity far = new Entity("far");
		LogicalOwnership whiteOwnership = ownership(white, p1.name.value);
		LogicalOwnership adjacentOwnership = ownership(adjacent, p1.name.value);
		LogicalOwnership farOwnership = ownership(far, p1.name.value);

		UnitClass whiteClass = infantry(white);
		UnitClass adjacentClass = infantry(adjacent);
		UnitClass farClass = infantry(far);
		shareBoardContext(whiteClass, whiteOwnership);
		shareBoardContext(adjacentClass, adjacentOwnership);
		shareBoardContext(farClass, farOwnership);

		whiteClass.cellColumn = 3;
		whiteClass.cellRow = 4;
		adjacentClass.cellColumn = 4;
		adjacentClass.cellRow = 4;
		farClass.cellColumn = 5;
		farClass.cellRow = 4;

		Entity enemy = new Entity("enemy");
		LogicalOwnership enemyOwnership = ownership(enemy, "p2");
		UnitClass enemyClass = infantry(enemy);
		shareBoardContext(enemyClass, enemyOwnership);

		Entity otherEnemy = new Entity("other-enemy");
		ownership(otherEnemy, "p2");

		classes.transferData(white, signal(new Vector2(), whiteClass.actionStarted));
		classes.transferData(enemy, signal(new Bool(false), enemyClass.remainsOnBoard));

		players.transferData(whiteOwnership.entity, signal(new Bool(false), whiteOwnership.isCurrentlyControlled));
		players.transferData(p1.entity, signal(new Bool(false), p1.isInProgress));

		assertTrue(p1.isInProgress.value);
		assertFalse(p2.isInProgress.value);
		assertFalse(p1.turnActionPending);

		assertTrue(adjacentClass.eligibleToAct);
		assertFalse(whiteClass.eligibleToAct);
		assertFalse(farClass.eligibleToAct);

		assertTrue(adjacentOwnership.isControlled.value);
		assertFalse(whiteOwnership.isControlled.value);
		assertFalse(farOwnership.isControlled.value);
	}

	@Test
	public void finalDeskExitMarksLogicalUnitOutsideTheField() {
		player("p1");
		player("p2");

		LogicalField field = addField(100, 100);

		Entity gun = new Entity("gun");
		ownership(gun, "p1");
		UnitClass gunClass = minigun(gun);
		gunClass.durabilitySpent = gunClass.hitsToDestroy.value - 1;
		gunClass.position.x.value = 10;
		gunClass.position.y.value = 10;

		LogicalUnit logicalUnit = new LogicalUnit();
		logicalUnit.inField.value = true;
		logicalUnit.lstFieldsToCheck.add(field);
		gun.addComponent(logicalUnit);

		classes.transferData(gun, signal(new Bool(false), gunClass.remainsOnBoard));

		assertFalse(gunClass.remainsOnBoard.value);
		assertTrue(gunClass.destroyed);
		assertFalse(logicalUnit.inField.value);
	}

	private LogicalGrid addGrid(int columns, int rows) {
		Entity entity = new Entity("grid");
		LogicalGrid grid = new LogicalGrid();
		grid.columnsNumber.value = columns;
		grid.rowsNumber.value = rows;
		grid.cells = new CellGrid[columns][rows];
		grid.size.x.value = columns * 10;
		grid.size.y.value = rows * 10;
		grid.setCellsParams();

		entity.addComponent(grid);
		Engine.getEntityStorage().lstEntities.add(entity);

		return grid;
	}

	private LogicalField addField(double width, double height) {
		Entity entity = new Entity("field");
		LogicalField field = new LogicalField();
		field.size.x.value = width;
		field.size.y.value = height;

		entity.addComponent(field);
		Engine.getEntityStorage().lstEntities.add(entity);

		return field;
	}

	private void setReturnSpot(UnitClass unit, double x, double y) {
		unit.returnX = x;
		unit.returnY = y;
		unit.hasReturnPosition = true;
	}
}
