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
 * Durability is consumed by desk exits, not by contacts or hits. If the unit
 * leaves the desk while durability remains, the handler restores it to the
 * position it had before the hit, or to the nearest free cell if needed.
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

	/** Separate desk exits needed to remove this unit. Infantry/Cavalry 1, Minigun 3. */
	public Numeral hitsToDestroy = new Numeral(1);

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

	/** Desk exits already spent from the current durability. */
	public int durabilitySpent = 0;

	/** Position the unit should return to if it leaves the desk with durability left. */
	public double returnX = 0;
	public double returnY = 0;
	public double returnAlpha = 0;
	public boolean hasReturnPosition = false;

	/** True once the current desk exit has been resolved for the active action. */
	public boolean exitProcessedThisAction = false;

	/** True once durability is exhausted. */
	public boolean destroyed = false;

	/** True if the current action contacted somebody (Cavalry movement-only check). */
	public boolean contactedThisAction = false;

	/** Handler-local copy of the last board state, useful because field commits mutate the Bool. */
	public boolean lastKnownOnBoard = true;

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
		durabilitySpent = 0;
		exitProcessedThisAction = false;
		hasReturnPosition = false;
		destroyed = false;
		contactedThisAction = false;
		movementOnlyNextAction = false;
	}

	public void clearActivationCandidates() {
		activationCandidates.clear();
	}

	protected UnitClass() { }
}
