package ru.framework2d.logic.players;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;
import ru.framework2d.compat.Log;
import ru.framework2d.logic.turns.FlawlessTurnRules;
import ru.framework2d.logic.turns.SequentialTurnRules;
import ru.framework2d.logic.turns.TurnRules;

public class HandlerPlayers extends ComponentsHandler {

	private static final String LOG_TAG = "Logical";
	
	/** Registered players in rotation order; public because the turn rules get it as an argument. */
	public ArrayList <LogicalPlayer> lstPlayers = new ArrayList <LogicalPlayer>();
	ArrayList <LogicalOwnership> lstOwnership = new ArrayList <LogicalOwnership>();

	/**
	 * Who plays next when an action ends. Defaults to the sequential rule; unit classes install
	 * their own provider (see HandlerUnitClasses) and a map can ask for the retired rule with
	 * {@code turnRule = "flawless"}.
	 */
	public TurnRules turnRules = new SequentialTurnRules();

	public HandlerPlayers() {
		super(2, (Class <Component>) LogicalPlayer.class.asSubclass(Component.class), 
				(Class <Component>) LogicalOwnership.class.asSubclass(Component.class));
		getContexts();
		loadEntities(entityStorage);
	}

	@Override
	public void setOptions(TagOptions options) {
		Property rule = options.findPropertyByName("turnRule");
		if (rule == null) return;

		String ruleName = rule.data.toString();
		if (ruleName.contentEquals("flawless")) {
			turnRules = new FlawlessTurnRules();
		} else if (ruleName.contentEquals("sequential")) {
			turnRules = new SequentialTurnRules();
		} else {
			Log.w(LOG_TAG, fullName + ": unknown turnRule <" + ruleName + ">; sequential used;");
			return;
		}
		Log.i(LOG_TAG, fullName + ": turn rule is <" + turnRules.rulesName() + ">;");
	}
	
	
	@Override
	public boolean transferData(Entity object, DataInterface data) {
		if (object == null || data == null || data.getContext() == null) return false;
		
		boolean transfered = false;
		boolean controlPassed = false;
		boolean pointsAreCounted = false;
		
		for (Component component : object.lstLogicals) {
			
			if (component instanceof LogicalOwnership) {
				
				if (data instanceof Position3) {
					transfered = true;
					component.position.set((Position3) data);
				} 
				else {
					
					LogicalOwnership ownership = (LogicalOwnership) component;
					
					if (data instanceof Bool) {
						
						if (data.getContext() == ownership.isExist.getContext()) {
							
							transfered = true;
							Log.d(LOG_TAG, name + ": change exist from " + ownership.isExist.value + " to " + ((Bool) data).value);
							
							LogicalPlayer owner = ownership.owner;
							
							ownership.isExist.value = ((Bool) data).value;
							
							if (ownership.isExist.value) {
								if (!owner.lstActiveOwnership.contains(ownership)) owner.lstActiveOwnership.add(ownership);

								if (!owner.gotControllingOwnership.value && !owner.lstActiveOwnership.isEmpty()) {
									Log.d(LOG_TAG, name + ": change " + owner.name + ".gotControllingOwnership to true;");
									owner.gotControllingOwnership.value = true;
									owner.gotControllingOwnership.commit(this, owner.entity);
								}

								if (owner.isInProgress.value) {
									setControlled(ownership, true);
								}
							} 
							else {
								owner.lstActiveOwnership.remove(ownership);
								setControlled(ownership, false);
							}
							
							if (!ownership.owner.isWaitForTurnOver.value) {
								if (owner.gotControllingOwnership.value) {
									if (!ownership.isExist.value) {
										owner.gotControllingOwnership.value = !owner.lstActiveOwnership.isEmpty();
										if (!owner.gotControllingOwnership.value) {
											Log.d(LOG_TAG, name + ": change " + owner.name + ".gotControllingOwnership to false;");
											owner.gotControllingOwnership.commit(this, owner.entity);
										}
									} 
								} 
								else {
									if (ownership.isExist.value) {
										Log.d(LOG_TAG, name + ": change " + owner.name + ".gotControllingOwnership to true;");
										owner.gotControllingOwnership.value = true;
										owner.gotControllingOwnership.commit(this, owner.entity);
									} 
								}
							}
							
						} 
						else if (data.getContext() == ownership.isControlled.getContext()) {
							transfered = true;
							ownership.isControlled.value = ((Bool) data).value;
						} 
						else if (data.getContext() == ownership.isCurrentlyControlled.getContext()) {
							
							transfered = true;
							
							if (!((Bool) data).value) {
								
								ownership.owner.turnActionPending = true;
								Log.i(LOG_TAG, name + ": turn: " + ownership.owner.name.value
										+ " action pending (unit " + ownership.entity.name + " released; active ownership "
										+ ownership.owner.lstActiveOwnership.size() + "/"
										+ ownership.owner.lstOwnership.size() + "; wait for turn over "
										+ ownership.owner.isWaitForTurnOver.value + ");");
								
								ownership.owner.gotAccess.value = false;
								
								for (LogicalOwnership ownershipItem : ownership.owner.lstOwnership) {
									setControlled(ownershipItem, false);
								}
								
								if (!ownership.owner.isWaitForTurnOver.value) {
									ownership.owner.isInProgress.value = false;
									ownership.owner.turnActionPending = false;
									
									LogicalPlayer nextPlayer = nextPlayerByRule(ownership.owner);
									nextPlayer.gotAccess.value = true;
									nextPlayer.isInProgress.value = true;
									turnRules.onTurnStarted(nextPlayer);
									for (LogicalOwnership ownershipItem : nextPlayer.lstOwnership) {
										ownershipItem.isControlled.value = true;
										ownershipItem.isControlled.commit(this, ownershipItem.entity);
									}
								}
							} 
//							Log.d(LOG_TAG, name + ": " + ownership.master.name + ".isCurrentlyControlled<" 
//												+ ownership.isCurrentlyControlled.getContext().name + ">: " 
//												+ (System.currentTimeMillis() - timerMark2) 
//												+ " ms; items: " + ownership.owner.lstOwnership.size() 
//												+ "; logicals" + object.lstLogicals.size());
						} 
					}
				}
			} 
			else if (component instanceof LogicalPlayer) {
				
				LogicalPlayer player = (LogicalPlayer) component;
				
				if (data instanceof Position3) {
					
					transfered = true;
					
					if (data.getContext() == player.position.getContext()) {
						Position3 where = (Position3) data;
						Log.d(LOG_TAG, name + ": moving player?? to " + where.getX() + " : " + where.getY());
					}
					
				} 
				else if (data instanceof Bool) {
					
					if (data.getContext() == player.isInProgress.getContext()) {
						
						transfered = true;
						
						if (player.isWaitForTurnOver.value && !((Bool) data).value) {
							/*check win*/
							if (!pointsAreCounted && player.isLoseRoundIfHaveNoOwnership.value) {
								
								pointsAreCounted = true;
								int survivedPlayers = 0;
								LogicalPlayer winner = null;
								
								for (LogicalPlayer owner : lstPlayers) {
									
									owner.gotControllingOwnership.value = !owner.lstActiveOwnership.isEmpty();
									
									if (!owner.gotControllingOwnership.value) {
										Log.d(LOG_TAG, name + ": " + owner.name + ".gotControllingOwnership = " 
																	+ owner.gotControllingOwnership.value + ";");
										owner.gotControllingOwnership.commit(this, player.entity);
									} 
									else {
										winner = owner;
										survivedPlayers++;
									}
								}
								
								Log.i(LOG_TAG, name + ": turn: round settlement after " + player.name.value
										+ " settled; surviving owners=" + survivedPlayers
										+ "; winner=" + (survivedPlayers == 1 && winner != null ? winner.name.value : "none")
										+ "; active ownership " + activeOwnershipSummary() + ";");
								
								if (survivedPlayers == 1) { // some player wins
									
									if (winner.winGavePointsOnlyIfEnemyHaveNoPoints.value) {
										if (!otherPlayersGotPoints(winner)) {
											winner.wonRounds.value += winner.pointsGaveForRoundVictory.value;		
										}
									} 
									else {
										winner.wonRounds.value += winner.pointsGaveForRoundVictory.value;												
									}
									
									if (winner.pointsGaveForRoundVictory.value > 0) {
										if (winner.wonRounds.value >= winner.pointsNeedToVictory.value) {
											for (LogicalPlayer aPlayer : lstPlayers) {
												aPlayer.wonRounds.value = aPlayer.startPoints.value;
											}
										}
									} 
									else {
										if (winner.wonRounds.value <= winner.pointsNeedToVictory.value) {
											for (LogicalPlayer aPlayer : lstPlayers) {
												aPlayer.wonRounds.value = aPlayer.startPoints.value;
											}
										}
									}
									
									if (winner.isGetTurnIfWon.value) {
										controlPassed = true;
									}
									
									winner.wonRounds.commit(this, winner.entity);
									
									for (int i = 1; i < lstPlayers.size(); i++) {
										
										LogicalPlayer losingPlayer = lstPlayers.get(
												(lstPlayers.indexOf(winner) + i) 
												% lstPlayers.size());
										
										if (losingPlayer.canGetNegativePoints.value) {
											losingPlayer.wonRounds.value -= losingPlayer.pointsLostForRoundLose.value;	
										}
										else {
											if (losingPlayer.pointsLostForRoundLose.value > 0) {
												if (losingPlayer.wonRounds.value - losingPlayer.startPoints.value 
														>= losingPlayer.pointsLostForRoundLose.value) {
													losingPlayer.wonRounds.value -= losingPlayer.pointsLostForRoundLose.value;
												} 
												else losingPlayer.wonRounds.value = losingPlayer.startPoints.value;
											} 
											else {
												if (losingPlayer.wonRounds.value - losingPlayer.startPoints.value 
														<= losingPlayer.pointsLostForRoundLose.value) {
													losingPlayer.wonRounds.value -= losingPlayer.pointsLostForRoundLose.value;
												} 
												else losingPlayer.wonRounds.value = losingPlayer.startPoints.value;
											}
										}
										
										losingPlayer.wonRounds.commit(this, losingPlayer.entity);
									}
									
									for (LogicalPlayer aPlayer : lstPlayers) {
										aPlayer.numOwnershipAtRoundStart.value = aPlayer.lstOwnership.size();
										aPlayer.turnActionPending = false;
									}

									for (LogicalPlayer aPlayer : lstPlayers) {
										turnRules.onRoundFinished(aPlayer);
									}

									winner.finishedRoundTrigger.commit(this, winner.entity);

									for (int i = 1; i < lstPlayers.size(); i++) {
										
										LogicalPlayer losingPlayer = lstPlayers.get(
												(lstPlayers.indexOf(winner) + i) 
												% lstPlayers.size());
										
										losingPlayer.finishedRoundTrigger.commit(this, losingPlayer.entity);
									}

									if (winner.isGetTurnIfWon.value) {
										giveControlToPlayer(winner);
										turnRules.onTurnStarted(winner);
									}
								} 
								else if (survivedPlayers == 0) { //draw
									
									for (LogicalPlayer owner : lstPlayers) {
										owner.wonRounds.commit(this, owner.entity);
										
										for (LogicalOwnership ownershipItem : owner.lstOwnership) {
											ownershipItem.isOwnerDrawRound.value = true;
											ownershipItem.isOwnerDrawRound.commit(this, ownershipItem.entity);
										}
										
										owner.numOwnershipAtRoundStart.value = owner.lstOwnership.size();
										owner.turnActionPending = false;
									}

									for (LogicalPlayer aPlayer : lstPlayers) {
										turnRules.onRoundFinished(aPlayer);
									}

									for (LogicalPlayer owner : lstPlayers) {
										owner.finishedRoundTrigger.commit(this, owner.entity);
									}

									if (!lstPlayers.isEmpty()) {
										controlPassed = true;
										LogicalPlayer nextPlayer = nextSequentialPlayer(player);
										giveControlToPlayer(nextPlayer);
										turnRules.onTurnStarted(nextPlayer);
									}
								}
							}
							
							/*next player turn*/
							if (player.isInProgress.value && !controlPassed) {
								Log.i(LOG_TAG, name + ": turn: settle for " + player.name.value
										+ " (pending=" + player.turnActionPending
										+ ", inProgress=" + player.isInProgress.value
										+ ", active ownership " + activeOwnershipSummary() + ");");
								
								//the turn rule lives in ru.framework2d.logic.turns (see TurnRules);
								//only a closed action of this player may rotate the turn, so that
								//map settling and respawn settling cannot pass the turn
								if (player.turnActionPending) {
									controlPassed = true;
									player.turnActionPending = false;
									
									LogicalPlayer nextPlayer = nextPlayerByRule(player);
									giveControlToPlayer(nextPlayer);
									turnRules.onTurnStarted(nextPlayer);
									
									for (LogicalPlayer aPlayer : lstPlayers) {
										aPlayer.numOwnershipAtRoundStart.value = aPlayer.lstActiveOwnership.size();
									}
								} 
								else {
									Log.i(LOG_TAG, name + ": turn: " + player.name.value 
											+ " units settled with no open action; turn kept by " 
											+ player.name.value + ";");
								}

							} 
						}
					}
				}
			}
		} 
		return transfered;
	}
	private LogicalPlayer nextPlayerByRule(LogicalPlayer justActed) {
		LogicalPlayer keeper = turnRules.keepTurnPlayer(justActed, lstPlayers);
		if (keeper != null) {
			Log.i(LOG_TAG, name + ": turn: " + justActed.name.value + " keeps the turn, rule <" 
					+ turnRules.rulesName() + ">;");
			return keeper;
		}
		LogicalPlayer nextPlayer = lstPlayers.get((lstPlayers.indexOf(justActed) + 1) % lstPlayers.size());
		Log.i(LOG_TAG, name + ": turn: pass from " + justActed.name.value + " to " + nextPlayer.name.value 
				+ ", rule <" + turnRules.rulesName() + ">;");
		return nextPlayer;
	}

	private void giveControlToPlayer(LogicalPlayer player) {
		if (player == null) return;

		Log.i(LOG_TAG, name + ": turn: control given to " + player.name.value
				+ " (active ownership " + player.lstActiveOwnership.size() + "/"
				+ player.lstOwnership.size() + ");");
		player.gotAccess.value = true;
		player.isInProgress.value = true;

		for (LogicalOwnership ownershipItem : player.lstOwnership) {
			setControlled(ownershipItem, ownershipItem.isExist.value);
		}
		
		for (int i = 1; i < lstPlayers.size(); i++) {
			LogicalPlayer nextPlayer = lstPlayers.get(
					(lstPlayers.indexOf(player) + i) 
					% lstPlayers.size());
			if (nextPlayer.isInProgress.value) {
				nextPlayer.gotAccess.value = false;
				nextPlayer.isInProgress.value = false;
				for (LogicalOwnership ownershipItem : nextPlayer.lstOwnership) {
					setControlled(ownershipItem, false);
				}
			}
		}
	}
	
	private String activeOwnershipSummary() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lstPlayers.size(); i++) {
			if (i > 0) sb.append(", ");
			LogicalPlayer player = lstPlayers.get(i);
			sb.append(player.name.value).append(":")
					.append(player.lstActiveOwnership.size()).append("/")
					.append(player.lstOwnership.size());
		}
		return sb.toString();
	}

	private LogicalPlayer nextSequentialPlayer(LogicalPlayer player) {
		int index = lstPlayers.indexOf(player);
		if (index < 0) index = 0;
		return lstPlayers.get((index + 1) % lstPlayers.size());
	}

	private void setControlled(LogicalOwnership ownership, boolean controlled) {
		if (ownership == null || ownership.owner == null) return;
		if (ownership.isControlled.value == controlled) return;

		ownership.isControlled.value = controlled;
		ownership.isControlled.commit(this, ownership.entity);
		Log.i(LOG_TAG, name + ": control: " + ownership.owner.name.value + "." + ownership.entity.name
				+ " controlled=" + controlled
				+ " exists=" + ownership.isExist.value
				+ " inProgress=" + ownership.owner.isInProgress.value + ";");
	}

	private boolean otherPlayersGotPoints(LogicalPlayer player) {
		for (int i = 1; i < lstPlayers.size(); i++) {
			LogicalPlayer nextPlayer = lstPlayers.get(
					(lstPlayers.indexOf(player) + i) 
					% lstPlayers.size());
			
			if (nextPlayer.wonRounds.value != nextPlayer.startPoints.value) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean startWork(int delay) {
		return false;
	}

	private boolean ownershipHaveNoOwner = false;
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof LogicalPlayer) {
			if (!lstPlayers.contains(component)) {
				LogicalPlayer player = (LogicalPlayer) component;
				
				lstPlayers.add(player);
				if (ownershipHaveNoOwner) {
					boolean stillGotOwnershipWithoutOwner = false;
					for (LogicalOwnership ownership : lstOwnership) {
						if (ownership.owner == null) {
							if (player.name.value.contentEquals(ownership.ownerName.value)) {
								ownership.owner = player;
								if (ownership.entity != null && !ownership.entity.isPrototype) {
									player.lstOwnership.add(ownership);
									if (ownership.isExist.value) {
										player.lstActiveOwnership.add(ownership);
										player.gotControllingOwnership.value = true;
										player.numOwnershipAtRoundStart.value = player.lstActiveOwnership.size(); 
									}
								}
							} 
							else stillGotOwnershipWithoutOwner = true;
						}
					}
					if (!stillGotOwnershipWithoutOwner) ownershipHaveNoOwner = false;
				}
			}
		} 
		else if (component instanceof LogicalOwnership) {
			if (!lstOwnership.contains(component)) {
				LogicalOwnership ownership = (LogicalOwnership) component;
				for (LogicalPlayer player : lstPlayers) {
					if (player.name.value.contentEquals(ownership.ownerName.value)) {
						ownership.owner = player;
						if (ownership.entity != null && !ownership.entity.isPrototype) {
							player.lstOwnership.add(ownership);
							if (ownership.isExist.value) {
								player.lstActiveOwnership.add(ownership);
								player.gotControllingOwnership.value = true;
								player.numOwnershipAtRoundStart.value = player.lstActiveOwnership.size(); 
							}
						}
					}
				} 
				if (ownership.owner == null) {
					ownershipHaveNoOwner = true;
					Log.d(LOG_TAG, name + ": created reflection got no owner;");
				}
				lstOwnership.add(ownership);
			}
		}
		return component;
	}
}
