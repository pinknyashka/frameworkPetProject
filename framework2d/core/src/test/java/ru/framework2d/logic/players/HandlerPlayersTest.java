package ru.framework2d.logic.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.logic.turns.FlawlessTurnRules;
import ru.framework2d.logic.turns.SequentialTurnRules;
import ru.framework2d.testsupport.TestSupport;

/**
 * {@link HandlerPlayers} ownership binding, point arithmetic and turn rotation.
 *
 * Components are built through the real factory path ({@code createComponent}), so every data
 * field gets the name and the {@code DataContext} the handler compares by identity. A signal is
 * then a data object carrying that same context: from the handler's point of view that is
 * indistinguishable from another component committing a field of the same context.
 *
 * Since phase 1 of plans/turn-based-logics-plan.md the turn passing rule is a separate object
 * ({@link ru.framework2d.logic.turns.SequentialTurnRules} by default, {@link FlawlessTurnRules}
 * for the retired "attack until the first mistake" behaviour). A turn only closes when the
 * player has an open action, i.e. a unit was released first.
 */
public class HandlerPlayersTest {

	private HandlerPlayers players;

	@Before
	public void setUp() {
		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		players = new HandlerPlayers();
	}

	@After
	public void tearDown() {
		players = null;
		TestSupport.resetIntervalPhysics();
	}

	private Property prop(String name, DataInterface data) {
		return new Property(name, data);
	}

	private LogicalPlayer player(String name, Property... properties) {
		TagOptions options = new TagOptions(LogicalPlayer.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("name", new Text(name)));
		for (Property property : properties) {
			options.lstPropertys.add(property);
		}
		return (LogicalPlayer) players.createComponent(new Entity(name), options);
	}

	private LogicalOwnership ownership(String entityName, String ownerName) {
		TagOptions options = new TagOptions(LogicalOwnership.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("ownerName", new Text(ownerName)));
		return (LogicalOwnership) players.createComponent(new Entity(entityName), options);
	}

	private LogicalOwnership ownershipOf(LogicalPlayer player, String entityName) {
		return ownership(entityName, player.name.value);
	}

	private <T extends DataInterface> T signal(T value, DataInterface field) {
		value.setContext(field.getContext());
		return value;
	}

	/** The shot: the selected unit of this player is released outside its own touch area. */
	private void release(LogicalOwnership owned) {
		players.transferData(owned.entity, signal(new Bool(false), owned.isCurrentlyControlled));
	}

	/** The logic field reports that no unit is moving any more. */
	private void endTurn(LogicalPlayer player) {
		players.transferData(player.entity, signal(new Bool(false), player.isInProgress));
	}

	/** One whole action of the shipped flow: a unit is released, then everything settles. */
	private void actAndSettle(LogicalPlayer player) {
		release(player.lstOwnership.get(0));
		endTurn(player);
	}

	private void kill(LogicalOwnership owned) {
		players.transferData(owned.entity, signal(new Bool(false), owned.isExist));
	}

	@Test
	public void registrationLinksOwnershipToPlayerByName() {
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned = ownership("man", "p1");

		assertEquals(1, players.lstPlayers.size());
		assertEquals(1, players.lstOwnership.size());
		assertSame(p1, owned.owner);
		assertSame(owned, p1.lstOwnership.get(0));
		assertSame(owned, p1.lstActiveOwnership.get(0));
		assertTrue(p1.gotControllingOwnership.value);
		assertEquals(1, p1.numOwnershipAtRoundStart.value);

		// connecting the same component twice must not duplicate any list
		players.connectComponent(p1);
		players.connectComponent(owned);
		assertEquals(1, players.lstPlayers.size());
		assertEquals(1, players.lstOwnership.size());
		assertEquals(1, p1.lstOwnership.size());
	}

	@Test
	public void ownershipParsedBeforeItsPlayerIsLinkedByTheBackFillPass() {
		// map order is free: an ownership parsed first is picked up by the reflection left
		// running in connectComponent (HandlerPlayers.java:373-392)
		LogicalOwnership owned = ownership("man", "p1");
		assertNull(owned.owner);

		ownership("deadeye", "ghost");

		// an ownership with no entity gets its owner field but never enters player lists (:401)
		LogicalOwnership detached = new LogicalOwnership();
		detached.ownerName.value = "p1";
		players.connectComponent(detached);

		LogicalPlayer p1 = player("p1");

		assertSame(p1, owned.owner);
		assertSame(owned, p1.lstOwnership.get(0));
		assertEquals(1, p1.lstActiveOwnership.size());
		assertTrue(p1.gotControllingOwnership.value);

		assertNull(players.lstOwnership.get(1).owner);
		assertSame(p1, detached.owner);
		assertEquals(1, p1.lstOwnership.size());
	}

	@Test
	public void existTransferMovesTheOwnershipInAndOutOfTheActiveList() {
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned = ownership("man", "p1");

		assertTrue(players.transferData(owned.entity, signal(new Bool(false), owned.isExist)));
		assertFalse(owned.isExist.value);
		assertTrue(p1.lstActiveOwnership.isEmpty());
		assertEquals(1, p1.lstOwnership.size());

		assertTrue(players.transferData(owned.entity, signal(new Bool(true), owned.isExist)));
		assertTrue(owned.isExist.value);
		assertEquals(1, p1.lstActiveOwnership.size());
	}

	@Test
	public void existTransferTogglesControllingOwnershipOnlyForANonWaitingPlayer() {
		// isWaitForTurnOver gates the whole gotControllingOwnership block (:73)
		LogicalPlayer idle = player("idle");
		LogicalOwnership idleMan = ownership("idleMan", "idle");
		kill(idleMan);
		assertFalse(idleMan.isExist.value);
		assertTrue(idle.gotControllingOwnership.value);
		players.transferData(idleMan.entity, signal(new Bool(true), idleMan.isExist));
		assertTrue(idle.gotControllingOwnership.value);

		LogicalPlayer acting = player("acting", prop("isWaitForTurnOver", new Bool(false)));
		LogicalOwnership actingMan = ownership("actingMan", "acting");
		kill(actingMan);
		assertFalse(acting.gotControllingOwnership.value);

		players.transferData(actingMan.entity, signal(new Bool(true), actingMan.isExist));
		assertTrue(acting.gotControllingOwnership.value);
		assertEquals(1, acting.lstActiveOwnership.size());
	}

	@Test(expected = NullPointerException.class)
	public void existTransferOfAnOwnerlessOwnershipThrowsNullPointerException() {
		// HandlerPlayers.java:67 dereferences ownership.owner without a guard
		LogicalOwnership ghost = ownership("ghostMan", "nobody");
		players.transferData(ghost.entity, signal(new Bool(true), ghost.isExist));
	}

	@Test
	public void positionTransferMovesEveryOwnershipRegardlessOfItsContext() {
		// unlike the player branch this one has no context check at all (:47-50)
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned = ownership("man", "p1");
		assertSame(p1, owned.owner);

		Entity piece = new Entity("piece");
		piece.addComponent(owned);
		piece.addComponent(p1);

		Position3 where = signal(new Position3(30, 40, 5), owned.isExist);
		assertTrue(players.transferData(piece, where));
		assertEquals(30d, owned.position.getX(), 0d);
		assertEquals(40d, owned.position.getY(), 0d);
		assertEquals(5d, owned.position.getZ(), 0d);

		// the player branch only logs a move it never performs (:136-144)
		assertEquals(0d, p1.position.getX(), 0d);
	}

	@Test
	public void isControlledTransferFollowsItsContext() {
		LogicalOwnership owned = ownership("man", "p1");

		assertTrue(players.transferData(owned.entity, signal(new Bool(false), owned.isControlled)));
		assertFalse(owned.isControlled.value);

		// the same flag reached through a foreign context is not ours: the handler answers
		// false, which is also what unsubscribes it from that field on a commit
		assertFalse(players.transferData(owned.entity, signal(new Bool(true), owned.position)));
		// nothing moves, so the value from the previous transfer survives
		assertFalse(owned.isControlled.value);
	}

	@Test
	public void currentlyControlledSignalIsConsumedButItsOwnFlagIsNeverWritten() {
		LogicalPlayer p1 = player("p1", prop("isWaitForTurnOver", new Bool(false)));
		LogicalOwnership owned = ownershipOf(p1, "man");
		// a second player keeps the rotation of :114 away from p1 itself
		player("p2", prop("isWaitForTurnOver", new Bool(false)));
		owned.isCurrentlyControlled.value = true;
		assertNotNull(owned.isCurrentlyControlled.getContext());

		assertTrue(players.transferData(owned.entity,
				signal(new Bool(false), owned.isCurrentlyControlled)));

		assertFalse(owned.isControlled.value);
		assertFalse(p1.gotAccess.value);
		assertFalse(p1.isInProgress.value);
		// pinned gap: the signal is read but ownership.isCurrentlyControlled is never
		// assigned, so it keeps the value of whoever selected the unit (:97-122)
		assertTrue(owned.isCurrentlyControlled.value);
	}

	@Test
	public void turnOverOfAPlayerWaitingForTurnOverClearsAccessButDoesNotRotate() {
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned = ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man2");

		assertTrue(players.transferData(owned.entity,
				signal(new Bool(false), owned.isCurrentlyControlled)));

		assertFalse(p1.gotAccess.value);
		assertFalse(owned.isControlled.value);
		assertTrue(p1.isInProgress.value);
		assertTrue(p2.isInProgress.value);
	}

	@Test
	public void turnOverRotatesControlToTheNextRegisteredPlayerAndWrapsAround() {
		LogicalPlayer p1 = player("p1", prop("isWaitForTurnOver", new Bool(false)));
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2", prop("isWaitForTurnOver", new Bool(false)));
		LogicalOwnership owned2 = ownershipOf(p2, "man2");
		owned2.isControlled.value = false;

		assertTrue(players.transferData(owned1.entity,
				signal(new Bool(false), owned1.isCurrentlyControlled)));

		assertFalse(p1.isInProgress.value);
		assertFalse(p1.gotAccess.value);
		assertFalse(owned1.isControlled.value);
		assertTrue(p2.isInProgress.value);
		assertTrue(p2.gotAccess.value);
		assertTrue(owned2.isControlled.value);

		// a turn over the last registered player wraps around to the first one (:114)
		LogicalPlayer p3 = player("p3", prop("isWaitForTurnOver", new Bool(false)));
		LogicalOwnership owned3 = ownershipOf(p3, "man3");

		assertTrue(players.transferData(owned3.entity,
				signal(new Bool(false), owned3.isCurrentlyControlled)));

		assertFalse(p3.isInProgress.value);
		assertFalse(owned3.isControlled.value);
		assertTrue(p1.isInProgress.value);
		// only the previous owner is switched off, a third player is left as it was
		assertTrue(p2.isInProgress.value);
	}

	@Test(expected = ArithmeticException.class)
	public void turnRotationWithoutRegisteredPlayersDividesByZero() {
		// indexOf an unregistered owner is -1, so the rotation of :114 computes 0 % 0
		LogicalPlayer p1 = player("p1", prop("isWaitForTurnOver", new Bool(false)));
		LogicalOwnership owned = ownershipOf(p1, "man");
		players.lstPlayers.clear();

		players.transferData(owned.entity, signal(new Bool(false), owned.isCurrentlyControlled));
	}

	@Test
	public void settlingWithoutAnActionDoesNotRotateTheTurn() {
		// map settling and respawn settling also end the field movement; only a released unit
		// opens an action, so a bare isInProgress=false must not pass the turn
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man2");

		endTurn(p1);
		endTurn(p1);

		assertTrue(p1.isInProgress.value);
		assertTrue(p2.isInProgress.value);
		assertTrue(p1.gotAccess.value);
		assertTrue(owned1.isControlled.value);
		assertEquals(0, p1.wonRounds.value);
	}

	@Test
	public void aReleasedUnitAloneDoesNotRotateTheTurn() {
		// the shot leaves the action open until the field reports that the units settled
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man2");

		release(owned1);

		assertTrue(p1.turnActionPending);
		assertTrue(p1.isInProgress.value);
		assertFalse(p1.gotAccess.value);
		assertFalse(owned1.isControlled.value);
	}

	@Test
	public void firstActionOfTheMapHandsTheTurnToTheNextPlayer() {
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		LogicalOwnership owned2 = ownershipOf(p2, "man2");
		owned2.isControlled.value = false;

		actAndSettle(p1);

		assertFalse(p1.isInProgress.value);
		assertFalse(p1.gotAccess.value);
		assertFalse(owned1.isControlled.value);
		assertTrue(p2.isInProgress.value);
		assertTrue(owned2.isControlled.value);
		assertFalse(p1.turnActionPending);
		assertEquals(1, p1.numOwnershipAtRoundStart.value);
		assertEquals(1, p2.numOwnershipAtRoundStart.value);
	}

	@Test
	public void theActingPlayerCannotActAgainBeforeTheTurnComesBack() {
		// sequential turns: one action per turn, so after the settle nothing of p1 is clickable
		// and a second settle without a new release changes nothing
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		ownershipOf(p1, "man3");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man2");

		actAndSettle(p1);

		assertFalse(owned1.isControlled.value);
		assertFalse(p1.gotAccess.value);
		assertFalse(p1.turnActionPending);

		endTurn(p1);

		assertFalse(p1.gotAccess.value);
		assertFalse(owned1.isControlled.value);
		assertTrue(p2.isInProgress.value);
	}

	@Test
	public void sequentialRulePassesTheTurnAfterAnAttackThatChangedNothing() {
		// the retired rule kept the turn while the attacker was flawless; the sequential rule
		// used by default passes the turn after every single action
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		ownershipOf(p1, "man2");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man3");
		ownershipOf(p2, "man4");

		endTurn(p1);
		actAndSettle(p1);

		assertFalse(p1.isInProgress.value);
		assertFalse(owned1.isControlled.value);
		assertTrue(p2.isInProgress.value);
		assertTrue(p2.lstOwnership.get(0).isControlled.value);
		assertEquals(0, p1.wonRounds.value);
		assertEquals(0, p2.wonRounds.value);
	}

	@Test
	public void flawlessRuleKeepsTheTurnWhenAnOpponentLostUnits() {
		players.turnRules = new FlawlessTurnRules();

		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		ownershipOf(p1, "man2");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man3");
		LogicalOwnership lostMan = ownershipOf(p2, "man4");

		endTurn(p1);
		kill(lostMan);
		actAndSettle(p1);

		// both sides still hold a unit, so nobody wins: the retired flawless rule keeps the turn
		// with p1 because every opponent dropped a unit
		assertTrue(p1.isInProgress.value);
		assertTrue(owned1.isControlled.value);
		assertFalse(p2.isInProgress.value);
		assertFalse(lostMan.isControlled.value);
		assertEquals(0, p1.wonRounds.value);
		assertEquals(0, p2.wonRounds.value);
		// a finished turn re-baselines on the ACTIVE units
		assertEquals(2, p1.numOwnershipAtRoundStart.value);
		assertEquals(1, p2.numOwnershipAtRoundStart.value);
	}

	@Test
	public void flawlessRuleGivesUpTheTurnWhenTheActingPlayerLostAUnit() {
		// linking an ownership refreshes the baseline, so only a unit LOST after the first
		// settle makes the active count differ from it
		players.turnRules = new FlawlessTurnRules();

		LogicalPlayer p1 = player("p1");
		LogicalOwnership leftMan = ownershipOf(p1, "man");
		ownershipOf(p1, "man4");
		LogicalPlayer p2 = player("p2");
		LogicalOwnership rightMan = ownershipOf(p2, "man2");
		ownershipOf(p2, "man5");

		endTurn(p1);
		kill(leftMan);
		actAndSettle(p1);

		// 1 active against a baseline of 2 is not flawless: the plain next player rule applies
		assertFalse(p1.isInProgress.value);
		assertTrue(p2.isInProgress.value);
		assertFalse(leftMan.isControlled.value);
		assertTrue(rightMan.isControlled.value);
		assertEquals(0, p1.wonRounds.value);
		assertEquals(1, p1.numOwnershipAtRoundStart.value);
	}

	@Test
	public void isGetTurnIfFlawlessFalseAlwaysPassesTheTurn() {
		players.turnRules = new FlawlessTurnRules();

		LogicalPlayer p1 = player("p1", prop("isGetTurnIfFlawless", new Bool(false)));
		ownershipOf(p1, "man");
		ownershipOf(p1, "man3");
		LogicalPlayer p2 = player("p2", prop("isGetTurnIfFlawless", new Bool(false)));
		ownershipOf(p2, "man2");

		actAndSettle(p1);

		assertFalse(p1.isInProgress.value);
		assertTrue(p2.isInProgress.value);
		// every closed turn re-baselines on the active units
		assertEquals(2, p1.numOwnershipAtRoundStart.value);
		assertEquals(1, p2.numOwnershipAtRoundStart.value);
	}

	@Test
	public void turnRuleIsSelectedFromTheHandlerOptions() {
		assertTrue(players.turnRules instanceof SequentialTurnRules);

		TagOptions options = new TagOptions("HandlerPlayers");
		options.lstPropertys.add(new Property("turnRule", new Text("flawless")));
		players.setOptions(options);
		assertEquals("flawless", players.turnRules.rulesName());

		options = new TagOptions("HandlerPlayers");
		options.lstPropertys.add(new Property("turnRule", new Text("sequential")));
		players.setOptions(options);
		assertEquals("sequential", players.turnRules.rulesName());

		// an unknown name is reported and the previous rule survives
		options = new TagOptions("HandlerPlayers");
		options.lstPropertys.add(new Property("turnRule", new Text("who-knows")));
		players.setOptions(options);
		assertEquals("sequential", players.turnRules.rulesName());
	}

	@Test
	public void roundEndWithASingleSurvivorScoresTheVictory() {
		LogicalPlayer p1 = player("p1");
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		kill(ownershipOf(p2, "man2"));

		endTurn(p1);

		assertEquals(1, p1.wonRounds.value);
		assertEquals(0, p2.wonRounds.value);
		assertTrue(p1.isInProgress.value);
		assertFalse(p2.isInProgress.value);
		assertTrue(p1.gotControllingOwnership.value);
		assertFalse(p2.gotControllingOwnership.value);
		// a round end baselines on the TOTAL units of a player, active or not (:245)
		assertEquals(1, p2.numOwnershipAtRoundStart.value);
	}

	@Test
	public void reachingTheVictoryTargetResetsEveryPlayerToTheirStartPoints() {
		LogicalPlayer p1 = player("p1",
				prop("pointsNeedToVictory", new Numeral(1)),
				prop("startPoints", new Numeral(5)));
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2", prop("startPoints", new Numeral(3)));
		kill(ownershipOf(p2, "man2"));

		endTurn(p1);

		assertEquals(5, p1.wonRounds.value);
		assertEquals(3, p2.wonRounds.value);
	}

	@Test
	public void negativeVictoryPointsCountDownAndResetAtTheTarget() {
		// the shipped map plays the second side down with pointsGaveForRoundVictory = -1,
		// which takes the inverted comparison of :194-200
		LogicalPlayer p1 = player("p1",
				prop("pointsGaveForRoundVictory", new Numeral(-1)),
				prop("pointsNeedToVictory", new Numeral(1)),
				prop("wonRounds", new Numeral(1)),
				prop("startPoints", new Numeral(5)));
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2",
				prop("startPoints", new Numeral(3)),
				prop("pointsLostForRoundLose", new Numeral(0)));
		kill(ownershipOf(p2, "man2"));

		endTurn(p1);

		assertEquals(5, p1.wonRounds.value);
		assertEquals(3, p2.wonRounds.value);
	}

	@Test
	public void negativeVictoryPointsAboveTheTargetDoNotReset() {
		LogicalPlayer p1 = player("p1",
				prop("pointsGaveForRoundVictory", new Numeral(-1)),
				prop("pointsNeedToVictory", new Numeral(1)),
				prop("wonRounds", new Numeral(5)),
				prop("startPoints", new Numeral(5)));
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2",
				prop("canGetNegativePoints", new Bool(true)),
				prop("pointsLostForRoundLose", new Numeral(0)),
				prop("wonRounds", new Numeral(9)),
				prop("startPoints", new Numeral(3)));
		kill(ownershipOf(p2, "man2"));

		endTurn(p1);

		assertEquals(4, p1.wonRounds.value);
		assertEquals(9, p2.wonRounds.value);
	}

	@Test
	public void loserPenaltyGoesBelowZeroOnlyWithCanGetNegativePoints() {
		LogicalPlayer winner = player("winner");
		ownershipOf(winner, "winnerMan");
		LogicalPlayer soft = player("soft", prop("canGetNegativePoints", new Bool(true)));
		kill(ownershipOf(soft, "softMan"));
		LogicalPlayer stone = player("stone", prop("canGetNegativePoints", new Bool(false)));
		kill(ownershipOf(stone, "stoneMan"));

		endTurn(winner);

		assertEquals(1, winner.wonRounds.value);
		assertEquals(-1, soft.wonRounds.value);
		assertEquals(0, stone.wonRounds.value);
	}

	@Test
	public void loserPenaltyIsFlooredAtStartPointsWhenTheLossIsTooBig() {
		LogicalPlayer p1 = player("p1", prop("wonRounds", new Numeral(2)));
		ownershipOf(p1, "man");
		LogicalPlayer payer = player("payer", prop("canGetNegativePoints", new Bool(false)));
		payer.wonRounds.value = 5;
		kill(ownershipOf(payer, "payerMan"));
		LogicalPlayer floored = player("floored", prop("canGetNegativePoints", new Bool(false)));
		floored.wonRounds.value = 2;
		floored.pointsLostForRoundLose.value = 3;
		kill(ownershipOf(floored, "flooredMan"));

		endTurn(p1);

		assertEquals(3, p1.wonRounds.value);
		assertEquals(4, payer.wonRounds.value);
		assertEquals(0, floored.wonRounds.value);
	}

	@Test
	public void victoryBonusIsSuppressedWhenAnEnemyAlreadyGotPoints() {
		LogicalPlayer p1 = player("p1", prop("winGavePointsOnlyIfEnemyHaveNoPoints", new Bool(true)));
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2",
				prop("wonRounds", new Numeral(4)),
				prop("startPoints", new Numeral(0)),
				prop("pointsLostForRoundLose", new Numeral(0)));
		kill(ownershipOf(p2, "man2"));

		endTurn(p1);

		assertEquals(0, p1.wonRounds.value);
		// a loss of 0 does not subtract, it resets the loser onto its start points (:235)
		assertEquals(0, p2.wonRounds.value);
	}

	@Test
	public void drawMarksEveryOwnershipAndClearsControllingOwnership() {
		LogicalPlayer p1 = player("p1");
		LogicalOwnership owned1 = ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		LogicalOwnership owned2 = ownershipOf(p2, "man2");

		kill(owned1);
		kill(owned2);
		endTurn(p1);

		assertTrue(owned1.isOwnerDrawRound.value);
		assertTrue(owned2.isOwnerDrawRound.value);
		assertFalse(p1.gotControllingOwnership.value);
		assertFalse(p2.gotControllingOwnership.value);
		assertEquals(0, p1.wonRounds.value);
		// a draw sets respawning, so the turn of a drawn round is never rotated
		assertTrue(p1.isInProgress.value);
		assertTrue(p2.isInProgress.value);
	}

	@Test
	public void deadScoreFieldsAreNeverTouchedByTheHandler() {
		LogicalPlayer p1 = player("p1");
		p1.points.value = 100;
		p1.pointsGaveForFlawlessVictory.value = 7;
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		LogicalOwnership owned2 = ownershipOf(p2, "man2");
		owned2.isOwnerWonGame.value = true;

		kill(owned2);
		endTurn(p1);
		endTurn(p1);

		assertEquals(100, p1.points.value);
		assertEquals(7, p1.pointsGaveForFlawlessVictory.value);
		assertTrue(owned2.isOwnerWonGame.value);
		assertEquals(2, p1.wonRounds.value);
	}

	@Test
	public void triggerCommitUnlinksTheHandlerFromThatField() {
		LogicalPlayer p1 = player("p1");
		assertTrue(p1.finishedRoundTrigger.lstLinkedHandlers.contains(players));

		// the handler has no Trigger branch, and DataInterface.java:134-137 drops every
		// handler that answers a commit with false
		p1.finishedRoundTrigger.commit(null, p1.entity);

		assertFalse(p1.finishedRoundTrigger.lstLinkedHandlers.contains(players));
	}

	@Test
	public void transferDataRequiresBothAContextAndAnEntity() {
		LogicalOwnership owned = ownership("man", "p1");

		try {
			players.transferData(owned.entity, new Bool(false));
			fail("a data without a context must not reach the handler");
		} catch (NullPointerException expected) {
			// HandlerPlayers.java:34-35 dereferences data.getContext() to build the log line
		}

		try {
			players.transferData(null, signal(new Bool(false), owned.isExist));
			fail("a null entity must not reach the handler");
		} catch (NullPointerException expected) {
			// the same log line dereferences object.name
		}
	}
}
