package ru.framework2d.logic.classes;

/**
 * Minigun class (plans/logics-plan.md).
 *
 * Destroying two or more enemies grants an additional turn. It survives the
 * first two desk exits and returns to the board each time; the third exit
 * consumes its durability and removes it.
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
	}
}
