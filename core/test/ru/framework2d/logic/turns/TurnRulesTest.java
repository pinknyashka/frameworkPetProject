package ru.framework2d.logic.turns;

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
import ru.framework2d.logic.players.HandlerPlayers;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.logic.players.LogicalPlayer;
import ru.framework2d.testsupport.TestSupport;

/**
 * The turn rules themselves (phase 1 of plans/turn-based-logics-plan.md). The players handler
 * decides WHEN a turn closes; the rule object decides WHO gets it, so the class abilities of
 * phase 2/3 have exactly one place to plug into.
 */
public class TurnRulesTest {

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

	private LogicalPlayer player(String name, Property... properties) {
		TagOptions options = new TagOptions(LogicalPlayer.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("name", new Text(name)));
		for (Property property : properties) {
			options.lstPropertys.add(property);
		}
		return (LogicalPlayer) players.createComponent(new Entity(name), options);
	}

	private LogicalOwnership ownershipOf(LogicalPlayer player, String entityName) {
		TagOptions options = new TagOptions(LogicalOwnership.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("ownerName", new Text(player.name.value)));
		return (LogicalOwnership) players.createComponent(new Entity(entityName), options);
	}

	/** An ownership leaves the field: the shared exist context reports false. */
	private void kill(LogicalOwnership owned) {
		Bool signal = new Bool(false);
		signal.setContext(owned.isExist.getContext());
		players.transferData(owned.entity, signal);
	}

	@Test
	public void sequentialRuleNeverKeepsTheTurn() {
		LogicalPlayer p1 = player("p1");
		ownershipOf(p1, "man");
		ownershipOf(p1, "man2");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man3");
		kill(ownershipOf(p2, "man4"));

		SequentialTurnRules rules = new SequentialTurnRules();

		assertNull(rules.keepTurnPlayer(p1, players.lstPlayers));
		assertNull(rules.keepTurnPlayer(p2, players.lstPlayers));
		assertEquals("sequential", rules.rulesName());
	}

	@Test
	public void sequentialRuleIgnoresTheFlawlessFlag() {
		// the shipped map still sets isGetTurnIfFlawless, the sequential rule must not read it
		LogicalPlayer p1 = player("p1");
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		kill(ownershipOf(p2, "man2"));

		assertTrue(p1.isGetTurnIfFlawless.value);
		assertNull(new SequentialTurnRules().keepTurnPlayer(p1, players.lstPlayers));
	}

	@Test
	public void flawlessRuleKeepsTheTurnOfAnAttackerThatLostNothing() {
		LogicalPlayer p1 = player("p1");
		ownershipOf(p1, "man");
		ownershipOf(p1, "man2");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man3");
		kill(ownershipOf(p2, "man4"));

		assertEquals(2, p1.numOwnershipAtRoundStart.value);
		assertSame(p1, new FlawlessTurnRules().keepTurnPlayer(p1, players.lstPlayers));
	}

	@Test
	public void flawlessRuleStopsKeepingTheTurnAfterTheAttackerLostAUnit() {
		LogicalPlayer p1 = player("p1");
		LogicalOwnership ownLost = ownershipOf(p1, "man");
		ownershipOf(p1, "man2");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man3");
		kill(ownershipOf(p2, "man4"));

		FlawlessTurnRules rules = new FlawlessTurnRules();
		assertSame(p1, rules.keepTurnPlayer(p1, players.lstPlayers));

		kill(ownLost);

		// 1 active against a baseline of 2 is the "first mistake": the series ends
		assertEquals(1, p1.lstActiveOwnership.size());
		assertEquals(2, p1.numOwnershipAtRoundStart.value);
		assertNull(rules.keepTurnPlayer(p1, players.lstPlayers));
	}

	@Test
	public void flawlessRulePassesWhenNobodyLostAnything() {
		LogicalPlayer p1 = player("p1");
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		ownershipOf(p2, "man2");

		assertNull(new FlawlessTurnRules().keepTurnPlayer(p1, players.lstPlayers));
	}

	@Test
	public void flawlessRuleIsDisabledByItsOwnFlag() {
		LogicalPlayer p1 = player("p1", new Property("isGetTurnIfFlawless", new Bool(false)));
		ownershipOf(p1, "man");
		LogicalPlayer p2 = player("p2");
		kill(ownershipOf(p2, "man2"));

		assertFalse(p1.isGetTurnIfFlawless.value);
		assertNull(new FlawlessTurnRules().keepTurnPlayer(p1, players.lstPlayers));
	}

	@Test
	public void rulesTolerateAPlayerThatIsNotRegistered() {
		LogicalPlayer ghost = new LogicalPlayer();
		ghost.name.value = "ghost";

		assertNull(new SequentialTurnRules().keepTurnPlayer(ghost, players.lstPlayers));
		assertNull(new FlawlessTurnRules().keepTurnPlayer(null, players.lstPlayers));
	}
}
