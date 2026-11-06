package ru.framework2d.logic.classes;

import java.util.ArrayList;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Trigger;
import ru.framework2d.logic.LogicalInterface;

/**
 * Shared representation of a game unit "class" (see plans/logics-plan.md).
 *
 * A class component marks the unit it is attached to as belonging to a special
 * class that bends the default turn rule (each unit acts once, then the turn
 * passes to the opponent). This component only carries the configuration, the
 * inbound signals and the bookkeeping a future turn handler may read and write.
 *
 * Communication follows the engine convention: a producer commits one of the
 * {@link Trigger} signals below; the handler reacts to the commit by context
 * identity. The action metadata ({@link #didAttack}, {@link #enemiesDestroyed},
 * {@link #remainsOnBoard}) is written by the producer before it commits
 * {@link #actionFinished}.
 */
public abstract class UnitClass extends LogicalInterface {

	// ---- configuration (addressable from the map) ---------------------------

	/** Enemies destroyed in a single action that grant this unit a bonus turn. */
	public Numeral killsForBonusTurn = new Numeral(0);

	/** Separate attacks needed to remove this unit (armor). Infantry/Cavalry 1, Minigun 3. */
	public Numeral hitsToDestroy = new Numeral(1);

	/** How many times this unit returns to the board after leaving it (Minigun only). */
	public Numeral maxRespawns = new Numeral(0);

	/**
	 * Chebyshev cell distance from the acting unit's start cell inside which a
	 * friendly unit may take the follow-up move. Infantry 1; 0 means "not
	 * adjacency gated" (any un-acted ally may continue).
	 */
	public Numeral adjacencyRange = new Numeral(0);

	// ---- inbound signals ----------------------------------------------------

	/** Committed when the unit begins an action; snapshots its start cell. */
	public Trigger actionStarted = new Trigger();

	/** Committed when the unit's action is over; the handler resolves the turn rule. */
	public Trigger actionFinished = new Trigger();

	/** Committed when this unit is attacked; the handler applies durability. */
	public Trigger wasAttacked = new Trigger();

	// ---- action metadata (written by the producer before actionFinished) ----

	public Bool didAttack = new Bool(false);
	public Numeral enemiesDestroyed = new Numeral(0);
	public Bool remainsOnBoard = new Bool(true);

	// ---- bookkeeping (read/written by the handler, asserted by tests) -------

	/** The unit already used its one action this turn. */
	public boolean hasActedThisTurn = false;

	/** Cell the unit occupies; kept in sync by the grid wiring (see plans/logics-todo.md). */
	public int cellColumn = 0;
	public int cellRow = 0;

	/** Cell captured on {@link #actionStarted}. */
	public int actionStartColumn = 0;
	public int actionStartRow = 0;

	/** Attacks absorbed so far; removed at {@link #hitsToDestroy}. */
	public int hitsTaken = 0;

	/** True once durability is exhausted. */
	public boolean destroyed = false;

	// ---- outputs the turn system reads --------------------------------------

	/** After a resolve: the acting player keeps the turn (turn does not pass). */
	public boolean keepTurn = false;

	/** Whether this unit is currently eligible to act in the ongoing turn. */
	public boolean eligibleToAct = true;

	/** Cavalry-only: the next action is a movement-only follow-up after a qualifying kill. */
	public boolean movementOnlyNextAction = false;

	/** Infantry-only: friendly infantry that were nearby when this unit got the attack order. */
	public final ArrayList <UnitClass> activationCandidates = new ArrayList <UnitClass>();

	public void resetMovementStats() {
		hasActedThisTurn = false;
		eligibleToAct = true;
		actionStartColumn = cellColumn;
		actionStartRow = cellRow;
	}

	public void resetCombatDamage() {
		hitsTaken = 0;
		destroyed = false;
	}

	public void clearActivationCandidates() {
		activationCandidates.clear();
	}

	protected UnitClass() { }
}
