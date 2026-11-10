package ru.framework2d.logic.turns;

import java.util.ArrayList;

import ru.framework2d.logic.players.LogicalPlayer;

/**
 * The retired "continuous attacks until the first mistake" rule, kept so old maps and the old
 * regression expectations can still opt into it explicitly ({@code turnRule = "flawless"} on the
 * players handler, or by assigning this object in a test).
 *
 * The acting player keeps the turn while it is <i>flawless</i>: it still owns every unit it owned
 * at the round baseline and at least one opponent lost a unit. The first lost unit of its own -
 * the "mistake" - ends the series. This is exactly the block that used to sit inline in
 * {@code HandlerPlayers} (git history: {@code HandlerPlayers.java:272-297}).
 */
public class FlawlessTurnRules implements TurnRules {

	@Override
	public LogicalPlayer keepTurnPlayer(LogicalPlayer justActed, ArrayList <LogicalPlayer> players) {
		if (justActed == null || !justActed.isGetTurnIfFlawless.value) return null;

		if (justActed.lstActiveOwnership.size() != justActed.numOwnershipAtRoundStart.value) return null;

		for (int i = 1; i < players.size(); i++) {
			LogicalPlayer other = players.get((players.indexOf(justActed) + i) % players.size());
			if (other.lstActiveOwnership.size() < other.numOwnershipAtRoundStart.value) {
				return justActed;
			}
		}
		return null;
	}

	@Override
	public void onTurnStarted(LogicalPlayer player) { }

	@Override
	public String rulesName() {
		return "flawless";
	}
}
