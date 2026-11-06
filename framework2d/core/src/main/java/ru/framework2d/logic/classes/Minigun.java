package ru.framework2d.logic.classes;

/**
 * Minigun class (plans/logics-plan.md).
 *
 * Destroying two or more enemies grants an additional turn. It absorbs three
 * attacks before it is removed, its hit counter resets when combat restarts,
 * and it returns to the board every time it leaves it (up to
 * {@link #maxRespawns}).
 */
public class Minigun extends UnitClass {

	public final static String SHORT_CLASS_NAME = "minigun";

	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Minigun() {
		killsForBonusTurn.value = 2;
		adjacencyRange.value = 0;
		hitsToDestroy.value = 3;
		maxRespawns.value = 3;
	}
}
