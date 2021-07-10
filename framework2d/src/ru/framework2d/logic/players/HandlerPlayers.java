package ru.framework2d.logic.players;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;
import android.util.Log;

public class HandlerPlayers extends ComponentsHandler {

	private static final String LOG_TAG = "Logical";
	
	ArrayList <LogicalPlayer> lstPlayers = new ArrayList <LogicalPlayer>();
	ArrayList <LogicalOwnership> lstOwnership = new ArrayList <LogicalOwnership>();

	
	
	public HandlerPlayers() {
		super(2, (Class <Component>) LogicalPlayer.class.asSubclass(Component.class), 
				(Class <Component>) LogicalOwnership.class.asSubclass(Component.class));
		getContexts();
		loadEntities(entityStorage);
	}
	
	private boolean respawning = true;
	
	@Override
	public boolean transferData(Entity object, DataInterface data) {
		
		Log.d(LOG_TAG, name + ": transfer data " + data.getName() 
				+ "<" + data.getContext().name + "> in " + object.name);
		
		boolean transfered = false;
		boolean controlPassed = false;
		boolean pointsAreCounted = false;
		
		long timerMark1 = System.currentTimeMillis();
		
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
							} 
							else {
								owner.lstActiveOwnership.remove(ownership);
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
							long timerMark2 = System.currentTimeMillis();
							
							if (!((Bool) data).value) {
								
								ownership.owner.gotAccess.value = false;
								
								for (LogicalOwnership ownershipItem : ownership.owner.lstOwnership) {
									ownershipItem.isControlled.value = false;
									ownershipItem.isControlled.commit(this, ownershipItem.entity);
								}
								
								if (!ownership.owner.isWaitForTurnOver.value) {
									ownership.owner.isInProgress.value = false;
									
									LogicalPlayer nextPlayer = lstPlayers.get((lstPlayers.indexOf(ownership.owner) + 1) % lstPlayers.size());
									nextPlayer.gotAccess.value = true;
									nextPlayer.isInProgress.value = true;
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
						long timerMark2 = System.currentTimeMillis();
						
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
										
										giveControlToPlayer(winner);
										
									}
									
									winner.wonRounds.commit(this, winner.entity);
									winner.finishedRoundTrigger.commit(this, winner.entity);
									respawning = true;
									
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
										losingPlayer.finishedRoundTrigger.commit(this, losingPlayer.entity);
										respawning = true;
									}
									
									for (LogicalPlayer aPlayer : lstPlayers) {
										aPlayer.numOwnershipAtRoundStart.value = aPlayer.lstOwnership.size();
									}
								} 
								else if (survivedPlayers == 0) { //draw
									
									for (LogicalPlayer owner : lstPlayers) {
										owner.wonRounds.commit(this, owner.entity);
										owner.finishedRoundTrigger.commit(this, owner.entity);
										respawning = true;
										
										for (LogicalOwnership ownershipItem : owner.lstOwnership) {
											ownershipItem.isOwnerDrawRound.value = true;
											ownershipItem.isOwnerDrawRound.commit(this, ownershipItem.entity);
										}
										
										owner.numOwnershipAtRoundStart.value = owner.lstOwnership.size();
									}
								}
							}
							
							/*next player turn*/
							if (player.isInProgress.value && !controlPassed) {
								//правило передачи хода поместить в подобработчик 
								
								controlPassed = true;
								if (!respawning) {
								
									if (player.isGetTurnIfFlawless.value) {
										
										//is this turn was flawless?
										if (player.lstActiveOwnership.size() == player.numOwnershipAtRoundStart.value) {
											boolean anotherPLayerLostOwnership = false;
											for (int i = 1; i < lstPlayers.size(); i++) {
												LogicalPlayer losingPlayer = lstPlayers.get(
														(lstPlayers.indexOf(player) + i) 
														% lstPlayers.size());
												if (losingPlayer.lstActiveOwnership.size() < losingPlayer.numOwnershipAtRoundStart.value) {
													anotherPLayerLostOwnership = true;	
												} 
											}
											if (anotherPLayerLostOwnership) {
												giveControlToPlayer(player);
											} 
											else {
												LogicalPlayer nextPlayer = lstPlayers.get((lstPlayers.indexOf(player) + 1) % lstPlayers.size());
												giveControlToPlayer(nextPlayer);
											}
											
										} 
										else {
											LogicalPlayer nextPlayer = lstPlayers.get((lstPlayers.indexOf(player) + 1) % lstPlayers.size());
											giveControlToPlayer(nextPlayer);
										}
										for (LogicalPlayer aPlayer : lstPlayers) {
											aPlayer.numOwnershipAtRoundStart.value = aPlayer.lstActiveOwnership.size();
										}
									} 
									else {
										LogicalPlayer nextPlayer = lstPlayers.get((lstPlayers.indexOf(player) + 1) % lstPlayers.size());
										giveControlToPlayer(nextPlayer);
									}
								}
								else {
									respawning = false;
								}

							} 
						}
						Log.d(LOG_TAG, name + ": player.isInProgress<" + data.getContext().name + ">: " 
								+ (System.currentTimeMillis() - timerMark2) 
								+ " ms; player items: " + player.lstOwnership.size() + "; ");
					}
				}
			}
		} 
		Log.d(LOG_TAG, name + ": transfer done in " + (System.currentTimeMillis() - timerMark1) + " ms;");
		return transfered;
	}
	private void giveControlToPlayer(LogicalPlayer player) {
		for (LogicalOwnership ownershipItem : player.lstOwnership) {
			ownershipItem.isControlled.value = true;
			ownershipItem.isControlled.commit(this, ownershipItem.entity);
		}
		player.isInProgress.value = true;
		
		for (int i = 1; i < lstPlayers.size(); i++) {
			LogicalPlayer nextPlayer = lstPlayers.get(
					(lstPlayers.indexOf(player) + i) 
					% lstPlayers.size());
			if (nextPlayer.isInProgress.value) {
				nextPlayer.gotAccess.value = false;
				nextPlayer.isInProgress.value = false;
				for (LogicalOwnership ownershipItem : nextPlayer.lstOwnership) {
					ownershipItem.isControlled.value = false;
					ownershipItem.isControlled.commit(this, ownershipItem.entity);
				}
			}
		}
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
