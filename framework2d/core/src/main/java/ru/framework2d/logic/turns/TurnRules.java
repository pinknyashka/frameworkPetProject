package ru.framework2d.logic.turns;

import java.util.ArrayList;

import ru.framework2d.logic.players.LogicalPlayer;

/**
 * The rule that decides who gets the turn after an action ended.
 *
 * The engine has no turn object: a turn is closed when the logic field reports that no unit is
 * moving any more (see plans/turn-based-logics-plan.md). {@link ru.framework2d.logic.players.HandlerPlayers}
 * owns that moment and delegates the "who plays next" decision here, so unit classes
 * ({@link ru.framework2d.logic.classes.UnitClass}) can override the default rotation without
 * the players subsystem knowing anything about them.
 */
public interface TurnRules {

	/**
	 * @param justActed the player whose unit has just finished an action
	 * @param players every registered player, in rotation order
	 * @return the player that keeps the turn, or null to pass the turn to the next player
	 */
	LogicalPlayer keepTurnPlayer(LogicalPlayer justActed, ArrayList <LogicalPlayer> players);

	/** Called for the player that got the turn, after control was given to it. */
	void onTurnStarted(LogicalPlayer player);

	/** Short name used in the {@code Logical} turn log lines. */
	String rulesName();
}
