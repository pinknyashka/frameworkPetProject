package ru.framework2d.logic.classes;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.DataContext;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.compat.Log;
import ru.framework2d.logic.grids.CellGrid;
import ru.framework2d.logic.grids.LogicalGrid;
import ru.framework2d.logic.players.HandlerPlayers;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.logic.players.LogicalPlayer;
import ru.framework2d.logic.turns.TurnRules;

public class HandlerUnitClasses extends ComponentsHandler implements TurnRules {

	private static final String LOG_TAG = "Logical";

	public final ArrayList <UnitClass> lstUnitClasses = new ArrayList <UnitClass>();

	public UnitClass lastActedUnit = null;
	public String actionOwnerName = "";
	public boolean actionOpen = false;

	public final ArrayList <UnitClass> activationCandidates = new ArrayList <UnitClass>();
	private HandlerPlayers playersHandler = null;

	@SuppressWarnings("unchecked")
	public HandlerUnitClasses() {
		super(3,
				(Class <Component>) Infantry.class.asSubclass(Component.class),
				(Class <Component>) Cavalry.class.asSubclass(Component.class),
				(Class <Component>) Minigun.class.asSubclass(Component.class));

		getContexts();
		loadEntities(entityStorage);
		installInPlayersHandler();
	}

	@Override
	public void setOptions(ru.framework2d.core.TagOptions options) {
		// phase 2 keeps the map wiring simple; optional per-handler tuning belongs to later phases
	}

	@Override
	public boolean startWork(int delay) {
		return false;
	}

	@Override
	public Component connectComponent(Component component) {
		super.connectComponent(component);

		if (component instanceof UnitClass && component.entity != null) {
			UnitClass unit = (UnitClass) component;

			if (!component.entity.isPrototype && !lstUnitClasses.contains(unit)) {
				lstUnitClasses.add(unit);
			}
		}

		return component;
	}

	@Override
	public boolean transferData(Entity object, DataInterface data) {
		if (object == null || data == null) return false;

		DataContext context = data.getContext();
		if (context == null) return false;

		UnitClass unit = findClass(object);
		if (unit == null) return false;

		if (context == unit.actionStarted.getContext()) {
			Log.d(LOG_TAG, name + ": class action signal <" + context.name + "> in " + object.name);
			startAction(unit);
			return true;
		}

		if (context == unit.wasAttacked.getContext()) {
			Log.d(LOG_TAG, name + ": class attack signal <" + context.name + "> in " + object.name);
			recordAttack(unit);
			return true;
		}

		if (data instanceof Bool && context == unit.remainsOnBoard.getContext()) {
			Log.d(LOG_TAG, name + ": class exist signal <" + context.name + "> in " + object.name
					+ ": " + ((Bool) data).value);
			handleExistence(unit, ((Bool) data).value);
			return true;
		}

		return false;
	}

	@Override
	public LogicalPlayer keepTurnPlayer(LogicalPlayer justActed, ArrayList <LogicalPlayer> players) {
		activationCandidates.clear();

		if (justActed == null || !actionOpen || lastActedUnit == null) return null;

		UnitClass acting = lastActedUnit;
		if (acting.killsForBonusTurn.value <= 0) return null;
		if (!acting.remainsOnBoard.value) return null;
		if (!belongsTo(acting, justActed.name.value)) return null;
		if (acting.enemiesDestroyed.value < acting.killsForBonusTurn.value) return null;

		acting.clearActivationCandidates();
		updateCurrentCell(acting);

		for (UnitClass candidate : lstUnitClasses) {
			if (candidate == acting) continue;
			if (!belongsTo(candidate, justActed.name.value)) continue;
			if (candidate.hasActedThisTurn) continue;
			if (!candidate.remainsOnBoard.value) continue;

			updateCurrentCell(candidate);

			if (acting.adjacencyRange.value == 0
					|| isWithinChebyshev(candidate.cellColumn, candidate.cellRow,
							acting.actionStartColumn, acting.actionStartRow,
							acting.adjacencyRange.value)) {
				activationCandidates.add(candidate);
				acting.activationCandidates.add(candidate);
			}
		}

		if (acting.adjacencyRange.value > 0 && activationCandidates.isEmpty()) {
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " bonus is not kept for "
					+ acting.entity.name + " (no adjacent candidates after "
					+ acting.enemiesDestroyed.value + " kill(s));");
			return null;
		}

		Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " keeps the turn with "
				+ acting.entity.name + " (class " + acting.getShortClassName()
				+ ", kills " + acting.enemiesDestroyed.value
				+ ", candidates " + activationCandidates.size() + ");");

		return justActed;
	}

	@Override
	public void onTurnStarted(LogicalPlayer player) {
		boolean gated = !activationCandidates.isEmpty();
		UnitClass acting = lastActedUnit;

		if (player != null) {
			for (UnitClass unit : lstUnitClasses) {
				if (!belongsTo(unit, player.name.value)) continue;

				if (gated) {
					boolean candidate = activationCandidates.contains(unit);
					unit.eligibleToAct = candidate;
					unit.hasActedThisTurn = unit == acting;

					if (candidate) {
						updateCurrentCell(unit);
					} 
					else {
						setControlled(unit, false);
					}
				} 
				else {
					updateCurrentCell(unit);
					unit.resetMovementStats();
					unit.eligibleToAct = true;
				}
			}
		}

		if (gated) {
			Log.i(LOG_TAG, name + ": turn: " + (player == null ? "?" : player.name.value)
					+ " follow-up is limited to " + activationCandidates.size() + " candidate(s);");
		}

		activationCandidates.clear();
		actionOpen = false;
		lastActedUnit = null;
		actionOwnerName = "";
	}

	@Override
	public String rulesName() {
		return "class-abilities";
	}

	private void installInPlayersHandler() {
		if (enginesManager == null) return;

		for (ComponentsHandler handler : enginesManager.lstHandlers) {
			if (handler instanceof HandlerPlayers) {
				installInto((HandlerPlayers) handler);
				return;
			}
		}

		Log.w(LOG_TAG, name + ": no HandlerPlayers in this map; class turn rules are not installed;");
	}

	void installInto(HandlerPlayers players) {
		if (players == null) return;

		playersHandler = players;

		if (players.turnRules == null
				|| players.turnRules.rulesName().contentEquals("sequential")) {
			players.turnRules = this;
			Log.i(LOG_TAG, name + ": class turn rules installed into " + players.name + ";");
		} 
		else {
			Log.w(LOG_TAG, name + ": HandlerPlayers already uses rule <"
					+ players.turnRules.rulesName() + ">; class rules not installed;");
		}
	}

	UnitClass findClass(Entity object) {
		if (object == null) return null;

		for (ru.framework2d.logic.LogicalInterface logical : object.lstLogicals) {
			if (logical instanceof UnitClass) return (UnitClass) logical;
		}

		return null;
	}

	private void startAction(UnitClass unit) {
		actionOpen = true;
		lastActedUnit = unit;
		actionOwnerName = ownerNameOf(unit);

		updateCurrentCell(unit);
		unit.actionStartColumn = unit.cellColumn;
		unit.actionStartRow = unit.cellRow;
		unit.hasActedThisTurn = true;
		unit.eligibleToAct = false;
		unit.didAttack.value = true;
		unit.enemiesDestroyed.value = 0;
		unit.remainsOnBoard.value = true;
		unit.clearActivationCandidates();

		Log.i(LOG_TAG, name + ": class action started: " + unit.entity.name + " owner " + actionOwnerName
				+ " cell (" + unit.actionStartColumn + ", " + unit.actionStartRow + ");");
	}

	private void recordAttack(UnitClass unit) {
		if (!actionOpen || lastActedUnit == null) {
			unit.remainsOnBoard.value = existsInField(unit);
		}
	}

	private void handleExistence(UnitClass unit, boolean exists) {
		unit.remainsOnBoard.value = exists;

		if (!actionOpen || lastActedUnit == null) return;
		if (exists) return;

		if (unit == lastActedUnit) {
			lastActedUnit.remainsOnBoard.value = false;
			return;
		}

		String exitedOwner = ownerNameOf(unit);
		if (exitedOwner.length() == 0) return;
		if (exitedOwner.contentEquals(actionOwnerName)) return;

		lastActedUnit.enemiesDestroyed.value++;
		Log.i(LOG_TAG, name + ": class kill counted by " + lastActedUnit.entity.name
				+ ": " + unit.entity.name + " left the board;");
	}

	private boolean existsInField(UnitClass unit) {
		if (unit == null || unit.entity == null) return false;

		for (Component component : unit.entity.lstComponents) {
			if (component instanceof ru.framework2d.logic.fields.LogicalUnit) {
				return ((ru.framework2d.logic.fields.LogicalUnit) component).inField.value;
			}
		}

		return true;
	}

	private LogicalOwnership ownershipOf(Entity entity) {
		if (entity == null) return null;

		for (Component component : entity.lstComponents) {
			if (component instanceof LogicalOwnership) return (LogicalOwnership) component;
		}

		return null;
	}

	private String ownerNameOf(UnitClass unit) {
		if (unit == null) return "";

		LogicalOwnership ownership = ownershipOf(unit.entity);
		if (ownership == null) return "";
		if (ownership.owner != null) return ownership.owner.name.value;

		return ownership.ownerName.value;
	}

	private boolean belongsTo(UnitClass unit, String playerName) {
		if (playerName == null || playerName.length() == 0) return false;

		String owner = ownerNameOf(unit);
		return owner.length() > 0 && owner.contentEquals(playerName);
	}

	private void setControlled(UnitClass unit, boolean controlled) {
		if (unit == null) return;

		LogicalOwnership ownership = ownershipOf(unit.entity);
		if (ownership == null) return;
		if (ownership.isControlled.value == controlled) return;

		ownership.isControlled.value = controlled;
		ownership.isControlled.commit(this, ownership.entity);
	}

	private void updateCurrentCell(UnitClass unit) {
		if (unit == null) return;

		int[] cell = nearestCell(unit.position.x.value, unit.position.y.value);
		if (cell == null) return;

		unit.cellColumn = cell[0];
		unit.cellRow = cell[1];
	}

	private int[] nearestCell(double x, double y) {
		EntityStorage storage = getEntityStorage();
		if (storage == null) return null;

		CellGrid bestCell = null;
		double bestDistance = Double.MAX_VALUE;

		for (Entity entity : storage.lstEntities) {
			for (Component component : entity.lstComponents) {
				if (!(component instanceof LogicalGrid)) continue;

				LogicalGrid grid = (LogicalGrid) component;
				if (grid.cells == null || grid.columnsNumber.value <= 0 || grid.rowsNumber.value <= 0) {
					continue;
				}

				for (int c = 0; c < grid.cells.length; c++) {
					if (grid.cells[c] == null) continue;

					int rows = Math.min(grid.rowsNumber.value, grid.cells[c].length);
					for (int r = 0; r < rows; r++) {
						CellGrid cell = grid.cells[c][r];
						if (cell == null) continue;

						double dx = cell.position.getX() - x;
						double dy = cell.position.getY() - y;
						double distance = dx * dx + dy * dy;

						if (distance < bestDistance) {
							bestDistance = distance;
							bestCell = cell;
						}
					}
				}
			}
		}

		if (bestCell == null) return null;

		return new int[] { bestCell.column, bestCell.row };
	}

	private boolean isWithinChebyshev(int c1, int r1, int c2, int r2, int range) {
		return Math.max(Math.abs(c1 - c2), Math.abs(r1 - r2)) <= range;
	}
}
