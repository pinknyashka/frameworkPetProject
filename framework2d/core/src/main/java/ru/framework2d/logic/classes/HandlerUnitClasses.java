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
import ru.framework2d.logic.fields.LogicalField;
import ru.framework2d.logic.fields.LogicalUnit;
import ru.framework2d.logic.grids.CellGrid;
import ru.framework2d.logic.grids.LogicalGrid;
import ru.framework2d.logic.players.HandlerPlayers;
import ru.framework2d.physics.collisions.PhysicalShape;
import ru.framework2d.logic.players.LogicalOwnership;
import ru.framework2d.logic.players.LogicalPlayer;
import ru.framework2d.logic.turns.TurnRules;

public class HandlerUnitClasses extends ComponentsHandler implements TurnRules {

	private static final String LOG_TAG = "Logical";

	public final ArrayList <UnitClass> lstUnitClasses = new ArrayList <UnitClass>();

	public UnitClass lastActedUnit = null;
	public String actionOwnerName = "";
	public boolean actionOpen = false;
	private boolean roundResetPending = false;

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
			Log.i(LOG_TAG, name + ": class action signal <" + context.name + "> in " + object.name);
			startAction(unit);
			return true;
		}

		if (context == unit.wasAttacked.getContext()) {
			Log.i(LOG_TAG, name + ": class attack signal <" + context.name + "> in " + object.name);
			recordAttack(unit);
			return true;
		}

		if (data instanceof Bool && context == unit.remainsOnBoard.getContext()) {
			Log.i(LOG_TAG, name + ": class exist signal <" + context.name + "> in " + object.name
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
		if (!belongsTo(acting, justActed.name.value)) return null;

		if (acting instanceof Cavalry) return keepCavalryTurn(acting, justActed);
		if (acting instanceof Minigun) return keepMinigunTurn(acting, justActed);
		return keepKillBasedTurn(acting, justActed);
	}

	@Override
	public void onRoundFinished(LogicalPlayer player) {
		roundResetPending = true;

		for (UnitClass unit : lstUnitClasses) {
			stopMovement(unit.entity);
			unit.resetCombatDamage();
			unit.resetMovementStats();
			unit.didAttack.value = false;
			unit.enemiesDestroyed.value = 0;
			unit.clearActivationCandidates();
			unit.lastKnownOnBoard = unit.remainsOnBoard.value;
		}

		activationCandidates.clear();
		actionOpen = false;
		lastActedUnit = null;
		actionOwnerName = "";

		Log.i(LOG_TAG, name + ": class combat state reset after round end"
				+ (player == null ? "" : " for " + player.name.value)
				+ "; eligibility reset for " + lstUnitClasses.size() + " unit(s);");
	}

	@Override
	public void onTurnStarted(LogicalPlayer player) {
		boolean resetRound = roundResetPending;
		boolean gated = !resetRound && !activationCandidates.isEmpty();
		UnitClass acting = lastActedUnit;

		int eligibleCount = 0;
		int controlledCount = 0;
		int activeCount = 0;

		if (player != null) {
			for (UnitClass unit : lstUnitClasses) {
				if (!belongsTo(unit, player.name.value)) continue;

				boolean activeUnit = !unit.destroyed && unit.remainsOnBoard.value;
				if (activeUnit) activeCount++;

				if (resetRound) {
					if (activeUnit) {
						unit.lastKnownOnBoard = true;
						updateCurrentCell(unit);
						updateReturnSpot(unit);
						unit.resetMovementStats();
						unit.eligibleToAct = true;
						setControlled(unit, true);
					} 
					else {
						unit.lastKnownOnBoard = false;
						unit.eligibleToAct = true;
						unit.hasActedThisTurn = false;
						setControlled(unit, false);
					}
				} 
				else if (gated) {
					boolean candidate = activeUnit && activationCandidates.contains(unit);
					unit.eligibleToAct = candidate;
					unit.hasActedThisTurn = unit == acting || !activeUnit;

					if (candidate) {
						updateReturnSpot(unit);
						setControlled(unit, true);
					} 
					else {
						setControlled(unit, false);
					}
				} 
				else if (activeUnit) {
					unit.resetMovementStats();
					updateReturnSpot(unit);
					unit.eligibleToAct = true;
					setControlled(unit, true);
				}
				else {
					unit.eligibleToAct = false;
					unit.hasActedThisTurn = true;
					setControlled(unit, false);
				}

				if (unit.eligibleToAct) eligibleCount++;
				if (isControlled(unit)) controlledCount++;
			}
		}

		Log.i(LOG_TAG, name + ": turn start: " + (player == null ? "?" : player.name.value)
				+ " gated=" + gated
				+ " roundReset=" + resetRound
				+ " active=" + activeCount
				+ " eligible=" + eligibleCount
				+ " controlled=" + controlledCount
				+ " candidates=" + activationCandidates.size()
				+ " lastActed=" + (acting == null ? "none" : acting.entity.name) + ";");

		roundResetPending = false;
		activationCandidates.clear();
		actionOpen = false;
		lastActedUnit = null;
		actionOwnerName = "";
	}

	@Override
	public String rulesName() {
		return "class-abilities";
	}

	private LogicalPlayer keepKillBasedTurn(UnitClass acting, LogicalPlayer justActed) {
		if (acting.killsForBonusTurn.value <= 0) return null;
		if (acting.destroyed || !acting.remainsOnBoard.value) {
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " bonus is not kept for "
					+ acting.entity.name + " (unit is off board after "
					+ acting.enemiesDestroyed.value + " kill(s));");
			return null;
		}
		if (acting.enemiesDestroyed.value < acting.killsForBonusTurn.value) {
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " bonus is not kept for "
					+ acting.entity.name + " (kills " + acting.enemiesDestroyed.value + "/"
					+ acting.killsForBonusTurn.value + ");");
			return null;
		}

		acting.clearActivationCandidates();
		updateCurrentCell(acting);

		Log.i(LOG_TAG, name + ": turn: checking kill rule for " + acting.entity.name
				+ " owner " + justActed.name.value
				+ " kills " + acting.enemiesDestroyed.value + "/" + acting.killsForBonusTurn.value
				+ " currentCell (" + acting.cellColumn + ", " + acting.cellRow + ")"
				+ " startCell (" + acting.actionStartColumn + ", " + acting.actionStartRow + ")"
				+ " range " + acting.adjacencyRange.value + ";");

		for (UnitClass candidate : lstUnitClasses) {
			if (candidate == acting) continue;
			if (!belongsTo(candidate, justActed.name.value)) continue;
			if (candidate.hasActedThisTurn) {
				Log.i(LOG_TAG, name + ": candidate skipped: " + candidate.entity.name
						+ " already acted this turn;");
				continue;
			}
			if (candidate.destroyed || !candidate.remainsOnBoard.value) {
				Log.i(LOG_TAG, name + ": candidate skipped: " + candidate.entity.name
						+ " is not active (destroyed=" + candidate.destroyed
						+ ", onBoard=" + candidate.remainsOnBoard.value + ");");
				continue;
			}

			updateCurrentCell(candidate);

			if (acting.adjacencyRange.value <= 0) continue;

			int distance = chebyshevDistance(candidate.cellColumn, candidate.cellRow,
					acting.actionStartColumn, acting.actionStartRow);
			boolean inside = distance <= acting.adjacencyRange.value;

			if (inside) {
				activationCandidates.add(candidate);
				acting.activationCandidates.add(candidate);
			}

			Log.i(LOG_TAG, name + ": candidate: " + candidate.entity.name
					+ " cell (" + candidate.cellColumn + ", " + candidate.cellRow + ")"
					+ " pos=(" + candidate.position.x.value + ", " + candidate.position.y.value + ")"
					+ " distance " + distance + "/" + acting.adjacencyRange.value
					+ " accepted=" + inside + ";");
		}

		if (acting.adjacencyRange.value > 0 && activationCandidates.isEmpty()) {
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " bonus is not kept for "
					+ acting.entity.name + " (no adjacent candidates after "
					+ acting.enemiesDestroyed.value + " kill(s));");
			return null;
		}

		logKeep(justActed, acting);
		return justActed;
	}

	private LogicalPlayer keepMinigunTurn(UnitClass acting, LogicalPlayer justActed) {
		if (acting.destroyed || !acting.remainsOnBoard.value) return null;
		if (acting.killsForBonusTurn.value <= 0) return null;
		if (acting.enemiesDestroyed.value < acting.killsForBonusTurn.value) return null;

		acting.clearActivationCandidates();
		activationCandidates.clear();
		logKeep(justActed, acting);
		return justActed;
	}

	private LogicalPlayer keepCavalryTurn(UnitClass acting, LogicalPlayer justActed) {
		if (acting.destroyed || !acting.remainsOnBoard.value) return null;

		acting.clearActivationCandidates();
		updateCurrentCell(acting);

		boolean engaged = acting.contactedThisAction || acting.enemiesDestroyed.value > 0;

		if (acting.movementOnlyNextAction) {
			acting.movementOnlyNextAction = false;

			if (engaged) {
				Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " cavalry bonus movement failed"
						+ " (contacted during movement-only action);");
				return null;
			}

			activationCandidates.add(acting);
			acting.activationCandidates.add(acting);
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " keeps the turn with cavalry "
					+ acting.entity.name + " after movement-only follow-up;");
			return justActed;
		}

		if (!engaged) {
			activationCandidates.add(acting);
			acting.activationCandidates.add(acting);
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " keeps the turn with cavalry "
					+ acting.entity.name + " after a move without contact;");
			return justActed;
		}

		if (acting.killsForBonusTurn.value > 0
				&& acting.enemiesDestroyed.value >= acting.killsForBonusTurn.value) {
			acting.movementOnlyNextAction = true;
			activationCandidates.add(acting);
			acting.activationCandidates.add(acting);
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " keeps the turn with cavalry "
					+ acting.entity.name + " (kills " + acting.enemiesDestroyed.value
					+ ", next action is movement-only);");
			return justActed;
		}

		return null;
	}

	private void logKeep(LogicalPlayer player, UnitClass acting) {
		Log.i(LOG_TAG, name + ": turn: " + player.name.value + " keeps the turn with "
				+ acting.entity.name + " (class " + acting.getShortClassName()
				+ ", kills " + acting.enemiesDestroyed.value
				+ ", candidates " + activationCandidates.size() + ");");
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

	private boolean startAction(UnitClass unit) {
		if (!unit.remainsOnBoard.value || unit.destroyed) {
			Log.w(LOG_TAG, name + ": class action refused: " + unit.entity.name
					+ " is not an active unit on the board;");
			return false;
		}

		roundResetPending = false;
		actionOpen = true;
		lastActedUnit = unit;
		actionOwnerName = ownerNameOf(unit);

		for (UnitClass other : lstUnitClasses) {
			other.exitProcessedThisAction = false;
		}

		updateCurrentCell(unit);
		updateReturnSpot(unit);
		unit.actionStartColumn = unit.cellColumn;
		unit.actionStartRow = unit.cellRow;
		unit.hasActedThisTurn = true;
		unit.eligibleToAct = false;
		unit.didAttack.value = true;
		unit.contactedThisAction = false;
		unit.enemiesDestroyed.value = 0;
		unit.remainsOnBoard.value = true;
		unit.lastKnownOnBoard = true;
		unit.clearActivationCandidates();

		String suffix = unit.movementOnlyNextAction ? " (movement-only follow-up)" : "";
		Log.i(LOG_TAG, name + ": class action started: " + unit.entity.name + " owner " + actionOwnerName
				+ " cell (" + unit.actionStartColumn + ", " + unit.actionStartRow + ")"
				+ " pos=(" + unit.position.x.value + ", " + unit.position.y.value + ")"
				+ suffix + ";");
		return true;
	}

	private void recordAttack(UnitClass unit) {
		unit.contactedThisAction = true;
		unit.didAttack.value = true;
	}

	private void handleExistence(UnitClass unit, boolean exists) {
		if (exists) {
			handleBoarding(unit);
			return;
		}

		handleDeskExit(unit);
	}

	private void handleBoarding(UnitClass unit) {
		unit.remainsOnBoard.value = true;
		unit.lastKnownOnBoard = true;
		unit.exitProcessedThisAction = false;

		syncOwnershipExist(unit, true);
		updateCurrentCell(unit);
		updateReturnSpot(unit);

		if (shouldControlActiveUnit(unit)) {
			setControlled(unit, true);
		}
	}

	private void handleDeskExit(UnitClass unit) {
		if (!unit.lastKnownOnBoard) {
			Log.i(LOG_TAG, name + ": class exit ignored: " + unit.entity.name
					+ " is already known off board;");
			return;
		}

		boolean alreadyProcessed = unit.exitProcessedThisAction;

		unit.remainsOnBoard.value = false;
		unit.lastKnownOnBoard = false;
		stopMovement(unit.entity);

		Log.i(LOG_TAG, name + ": class exit: " + unit.entity.name
				+ " owner=" + ownerNameOf(unit)
				+ " durability=" + unit.durabilitySpent + "/" + unit.hitsToDestroy.value
				+ " duplicate=" + alreadyProcessed
				+ " lastActed=" + (lastActedUnit == null ? "none" : lastActedUnit.entity.name) + ";");

		if (alreadyProcessed) return;

		unit.exitProcessedThisAction = true;
		recordEnemyDeskExit(unit);
		spendDurability(unit);

		if (canReturnToDesk(unit)) {
			returnUnitToDesk(unit);
			return;
		}

		removeUnitFromDesk(unit);
	}

	private void spendDurability(UnitClass unit) {
		if (unit.hitsToDestroy.value > 0 && unit.durabilitySpent < unit.hitsToDestroy.value) {
			unit.durabilitySpent++;
		}
	}

	private boolean canReturnToDesk(UnitClass unit) {
		return unit.hitsToDestroy.value > 0 && unit.durabilitySpent < unit.hitsToDestroy.value;
	}

	private void returnUnitToDesk(UnitClass unit) {
		restoreUnitPosition(unit);

		unit.remainsOnBoard.value = true;
		unit.lastKnownOnBoard = true;
		unit.destroyed = false;
		unit.exitProcessedThisAction = false;

		syncOwnershipExist(unit, true);
		markLogicalUnitOnBoard(unit, true);
		unit.remainsOnBoard.commit(this, unit.entity);

		updateCurrentCell(unit);
		updateReturnSpot(unit);

		if (shouldControlActiveUnit(unit)) {
			setControlled(unit, true);
		}

		Log.i(LOG_TAG, name + ": class unit returned to the desk: " + unit.entity.name
				+ " durability " + unit.durabilitySpent + "/" + unit.hitsToDestroy.value + ";");
	}

	private void removeUnitFromDesk(UnitClass unit) {
		unit.remainsOnBoard.value = false;
		unit.lastKnownOnBoard = false;
		unit.destroyed = true;
		unit.hasReturnPosition = false;

		markLogicalUnitOnBoard(unit, false);
		setControlled(unit, false);
		syncOwnershipExist(unit, false);
	}

	private void recordEnemyDeskExit(UnitClass unit) {
		if (!actionOpen || lastActedUnit == null) {
			Log.i(LOG_TAG, name + ": class exit not counted: " + unit.entity.name
					+ " because no class action is open;");
			return;
		}

		if (unit == lastActedUnit) {
			Log.i(LOG_TAG, name + ": class exit not counted: " + unit.entity.name
					+ " is the last acted unit;");
			return;
		}

		String exitedOwner = ownerNameOf(unit);
		if (exitedOwner.length() == 0) {
			Log.w(LOG_TAG, name + ": class exit not counted: " + unit.entity.name
					+ " has no owner;");
			return;
		}

		if (exitedOwner.contentEquals(actionOwnerName)) {
			Log.i(LOG_TAG, name + ": class exit not counted: " + unit.entity.name
					+ " belongs to action owner " + actionOwnerName + ";");
			return;
		}

		lastActedUnit.enemiesDestroyed.value++;
		Log.i(LOG_TAG, name + ": class enemy knocked off the desk: " + unit.entity.name
				+ " durability " + unit.durabilitySpent + "/" + unit.hitsToDestroy.value
				+ "; knock counted for " + lastActedUnit.entity.name + ";");
	}

	private void updateReturnSpot(UnitClass unit) {
		if (unit == null || unit.destroyed || !unit.lastKnownOnBoard) return;

		updateCurrentCell(unit);

		unit.returnX = unit.position.x.value;
		unit.returnY = unit.position.y.value;
		unit.returnAlpha = unit.position.getAlpha();
		unit.hasReturnPosition = true;
	}

	private void restoreUnitPosition(UnitClass unit) {
		if (unit == null || unit.entity == null) return;

		double targetX = unit.hasReturnPosition ? unit.returnX : unit.position.x.value;
		double targetY = unit.hasReturnPosition ? unit.returnY : unit.position.y.value;
		double targetAlpha = unit.hasReturnPosition ? unit.returnAlpha : unit.position.getAlpha();

		CellGrid cell = findNearestFreeCell(unit, targetX, targetY);
		if (cell != null) {
			targetX = cell.position.getX();
			targetY = cell.position.getY();
		}

		unit.position.x.value = targetX;
		unit.position.y.value = targetY;
		unit.position.setAlpha(targetAlpha);
		applyPositionToEntity(unit.entity, targetX, targetY, targetAlpha);
		unit.position.commit(this, unit.entity);
	}

	private void applyPositionToEntity(Entity object, double x, double y, double alpha) {
		if (object == null) return;

		for (Component component : object.lstComponents) {
			component.position.x.value = x;
			component.position.y.value = y;
			component.position.setAlpha(alpha);
		}
	}

	private CellGrid findNearestFreeCell(UnitClass unit, double x, double y) {
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
						if (cell == null || isCellOccupied(unit, cell)) continue;

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

		return bestCell;
	}

	private boolean isCellOccupied(UnitClass unit, CellGrid cell) {
		if (cell == null) return false;

		for (UnitClass other : lstUnitClasses) {
			if (other == unit || !other.lastKnownOnBoard || other.destroyed) continue;
			if (other.cellColumn == cell.column && other.cellRow == cell.row) return true;

			double otherX = other.hasReturnPosition ? other.returnX : other.position.x.value;
			double otherY = other.hasReturnPosition ? other.returnY : other.position.y.value;
			double halfWidth = cell.width > 0 ? cell.width / 2.0 : 0;
			double halfHeight = cell.height > 0 ? cell.height / 2.0 : 0;

			double dx = cell.position.getX() - otherX;
			double dy = cell.position.getY() - otherY;

			if (halfWidth > 0 && halfHeight > 0
					&& Math.abs(dx) < halfWidth && Math.abs(dy) < halfHeight) {
				return true;
			}
		}

		return false;
	}

	private void stopMovement(Entity entity) {
		if (entity == null) return;

		for (ru.framework2d.physics.PhysicalInterface physical : entity.lstPhysicals) {
			if (physical instanceof PhysicalShape) {
				((PhysicalShape) physical).movement.stop();
			}
		}
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

	private boolean isControlled(UnitClass unit) {
		if (unit == null) return false;

		LogicalOwnership ownership = ownershipOf(unit.entity);
		return ownership != null && ownership.isControlled.value;
	}

	private boolean shouldControlActiveUnit(UnitClass unit) {
		if (unit == null || unit.destroyed || !unit.remainsOnBoard.value) return false;

		LogicalOwnership ownership = ownershipOf(unit.entity);
		if (ownership == null || ownership.owner == null) return false;

		return ownership.owner.isInProgress.value;
	}

	private void syncOwnershipExist(UnitClass unit, boolean exists) {
		if (unit == null) return;

		LogicalOwnership ownership = ownershipOf(unit.entity);
		if (ownership == null || ownership.isExist.value == exists) return;

		ownership.isExist.value = exists;
		if (ownership.owner == null) return;
		ownership.isExist.commit(this, ownership.entity);
	}

	private void markLogicalUnitOnBoard(UnitClass unit, boolean onBoard) {
		if (unit == null || unit.entity == null) return;

		for (ru.framework2d.logic.LogicalInterface logical : unit.entity.lstLogicals) {
			if (!(logical instanceof LogicalUnit)) continue;

			LogicalUnit logicalUnit = (LogicalUnit) logical;

			if (onBoard && !isInsideKnownFields(logicalUnit)) continue;

			if (logicalUnit.inField.value != onBoard) {
				logicalUnit.inField.value = onBoard;
			}
		}
	}

	private boolean isInsideKnownFields(LogicalUnit unit) {
		if (unit == null) return false;

		for (LogicalField field : unit.lstFieldsToCheck) {
			if (field == null || !field.isEnabled.value) continue;

			if (field.checkEscape(unit.position)) return false;
		}

		return true;
	}

	private void setControlled(UnitClass unit, boolean controlled) {
		if (unit == null) return;

		LogicalOwnership ownership = ownershipOf(unit.entity);
		if (ownership == null) return;
		if (ownership.isControlled.value == controlled) return;

		ownership.isControlled.value = controlled;
		ownership.isControlled.commit(this, ownership.entity);
		Log.i(LOG_TAG, name + ": class control: " + unit.entity.name
				+ " controlled=" + controlled
				+ " eligible=" + unit.eligibleToAct
				+ " acted=" + unit.hasActedThisTurn
				+ " cell (" + unit.cellColumn + ", " + unit.cellRow + ");");
	}

	private void updateCurrentCell(UnitClass unit) {
		if (unit == null) return;

		int[] cell = nearestCell(unit.position.x.value, unit.position.y.value);
		if (cell == null) {
			Log.w(LOG_TAG, name + ": no grid cell found for " + unit.entity.name
					+ " at pos=(" + unit.position.x.value + ", " + unit.position.y.value + ");");
			return;
		}

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
		return chebyshevDistance(c1, r1, c2, r2) <= range;
	}

	private int chebyshevDistance(int c1, int r1, int c2, int r2) {
		return Math.max(Math.abs(c1 - c2), Math.abs(r1 - r2));
	}
}
