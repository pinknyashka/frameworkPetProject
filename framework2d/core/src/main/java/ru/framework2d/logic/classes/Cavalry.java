package ru.framework2d.logic.classes;

/**
 * Cavalry class (plans/logics-plan.md).
 *
 * A cavalry attack that destroys three enemies resets that same cavalry unit's
 * movement state and gives it one movement-only follow-up turn. The next action
 * for that unit must not attack; after that movement it may attack again. The
 * bonus belongs to the acting cavalry only, not to friendly units.
 */
public class Cavalry extends UnitClass {

	public final static String SHORT_CLASS_NAME = "cavalry";

	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Cavalry() {
		killsForBonusTurn.value = 3;
		adjacencyRange.value = 0;
		hitsToDestroy.value = 1;
		maxRespawns.value = 0;
	}
}
