package ru.framework2d.logic.turns;

import java.util.ArrayList;

import ru.framework2d.logic.players.LogicalPlayer;

/**
 * The pre-release rule (plans/logics-plan.md, plans/Pre-release-plan.md §7): a unit acts once,
 * then the turn passes to the next player. Nothing keeps the turn here; class abilities plug in
 * through a {@link TurnRules} implementation that answers the same question.
 */
public class SequentialTurnRules implements TurnRules {

	@Override
	public LogicalPlayer keepTurnPlayer(LogicalPlayer justActed, ArrayList <LogicalPlayer> players) {
		return null;
	}

	@Override
	public void onTurnStarted(LogicalPlayer player) { }

	@Override
	public String rulesName() {
		return "sequential";
	}
}
