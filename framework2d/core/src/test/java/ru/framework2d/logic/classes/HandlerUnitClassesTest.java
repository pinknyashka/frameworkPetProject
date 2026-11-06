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
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Text;
import ru.framework2d.data.Vector2;
import ru.framework2d.logic.players.HandlerPlayers;
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
}
