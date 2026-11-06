package ru.framework2d.logic.classes;

/**
 * Infantry class (plans/logics-plan.md).
 *
 * If it destroys an enemy during an attack and stays on the board, the same
 * player keeps the turn but only a friendly unit within one square - including
 * diagonal neighbours - of the infantry's original cell may take the follow-up
 * move.
 */
public class Infantry extends UnitClass {

	public final static String SHORT_CLASS_NAME = "infantry";

	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Infantry() {
		killsForBonusTurn.value = 1;
		adjacencyRange.value = 1;
		hitsToDestroy.value = 1;
		maxRespawns.value = 0;
	}
}
