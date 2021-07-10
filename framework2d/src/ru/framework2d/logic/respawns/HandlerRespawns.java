package ru.framework2d.logic.respawns;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Trigger;
import android.util.Log;

public class HandlerRespawns extends ComponentsHandler {
	
	ArrayList <LogicalRespawner> lstRespawners = new ArrayList <LogicalRespawner>();
	ArrayList <LogicalRespawn> lstRespawns = new ArrayList <LogicalRespawn>();
	ArrayList <Entity> lstPrototypes = new ArrayList <Entity>();
	
	private static final String LOG_TAG = "Logical";
	
	public HandlerRespawns() {
		super(2, (Class <Component>) LogicalRespawn.class.asSubclass(Component.class), 
				(Class <Component>) LogicalRespawner.class.asSubclass(Component.class));
		
		getContexts();
		loadEntities(entityStorage);
	}
	
	
	
	protected void iterate() {
    	for (LogicalRespawner respawner : lstRespawners) {
    		if (respawner.prototype != null) {
	    		while (respawner.number.value > respawner.lstRespawns.size()) {
	    			enginesManager.loadEntity(
	    					entityStorage.createEntityByPrototype(respawner.prototype));
	    		}
    		}
    	}
    	
    	setWorkState(false);
    	
    	masterEngine.subEngineWorkDone(this);
	}
	
	@Override
	public boolean transferData(Entity object, DataInterface data) {
		Log.d(LOG_TAG, name + ": transfer data " + data.getName() 
							+ "<" + data.getContext().name + "> in " + object.name);
		long timerMark1 = System.currentTimeMillis();
		
		boolean transfered = false;
		
		for (Component reflection : object.lstLogicals) {
			if (reflection instanceof LogicalRespawn) {
				
				LogicalRespawn respawn = (LogicalRespawn) reflection;
				if (data instanceof Position3) {
					if (data.getContext() == respawn.respawningPosition.getContext()) {
						transfered = true;
						Position3 where = (Position3) data;
						respawn.respawningPosition.set(where);
						/*respawn.position.set(respawn.respawningPosition);
						respawn.position.commit(this, respawn.master);*/
						Log.d(LOG_TAG, name + ": moving respawn to " + where.getX() + " : " + where.getY());
					}
				} 
				else if (data instanceof Trigger) {
					if (data.getContext() == respawn.respawningTrigger.getContext()) {
						transfered = true;
						respawn.position.set(respawn.respawningPosition);
						respawn.position.commit(this, respawn.entity);
						Log.d(LOG_TAG, name + ": respawning trigger<" + data.getContext().name + "> switched; ");
					}
				}
				
			} 
			else if (reflection instanceof LogicalRespawner) {
				
				LogicalRespawner respawner = (LogicalRespawner) reflection;
				if (data instanceof Trigger) {
					if (data.getContext() == respawner.respawningTrigger.getContext()) {
						transfered = true;
						for (LogicalRespawn respawn : respawner.lstRespawns) {
							respawn.position.set(respawn.respawningPosition);
							respawn.position.commit(this, respawn.entity);
							Log.d(LOG_TAG, name + ": respawning " + respawn.entity.name + " switched; ");
						}
						Log.d(LOG_TAG, name + ": respawning trigger<" + data.getContext().name + "> switched; ");
					}
				}
				
			}
		}
		Log.d(LOG_TAG, name + ": transfer done in "+ (System.currentTimeMillis() - timerMark1) + " ms;");
		return transfered;
	}

	private boolean respawnWithoutRespawner;
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (!component.entity.isPrototype) {
			
			if (component instanceof LogicalRespawn) {
				
				if (!lstRespawns.contains(component)) {
					
					LogicalRespawn respawn = (LogicalRespawn) component;
					
					lstRespawns.add(respawn);
					
					setWorkState(true);
					
					if (!respawn.respawnerName.value.contentEquals("")) {
						
						for (LogicalRespawner respawner : lstRespawners) {
							
							if (respawn.respawnerName.value.contentEquals(respawner.name.value)) {
								
								respawn.respawner = respawner;
								if (!respawner.lstRespawns.contains(respawn)) {
									respawner.lstRespawns.add(respawn);
								}
							}
						}
						if (respawn.respawner == null) {
							respawnWithoutRespawner = true;
						}
					}
				}
			} 
			else if (component instanceof LogicalRespawner) {
				
				if (!lstRespawners.contains(component)) {
					
					LogicalRespawner respawner = (LogicalRespawner) component;
					
					lstRespawners.add(respawner);
					
					setWorkState(true);
					
					if (respawnWithoutRespawner) {
						
						boolean stillHaveWithout = false;
						
						for (LogicalRespawn respawn : lstRespawns) {
							
							if (respawn.respawner == null) {
								
								if (respawn.respawnerName.value.contentEquals(respawner.name.value)) {
									
									respawn.respawner = respawner;
									if (!respawner.lstRespawns.contains(respawn)) {
										respawner.lstRespawns.add(respawn);
									}
									respawnWithoutRespawner = false;
								} 
								else stillHaveWithout = true;
							}
						}
						if (stillHaveWithout) respawnWithoutRespawner = true;
					}
				}
			}
		} 
		else {
			if (component instanceof LogicalRespawn) {
				if (!lstPrototypes.contains(component.entity)) {
					
					lstPrototypes.add(component.entity);
					LogicalRespawn respawn = (LogicalRespawn) component;
					
					setWorkState(true);
					
					if (!respawn.respawnerName.value.contentEquals("")) {
						for (LogicalRespawner respawner : lstRespawners) {
							if (respawn.respawnerName.value.contentEquals(respawner.name.value)) {
								respawner.prototype = respawn.entity;
							}
						}
					}
				}
			}
		}
		return component;
	}
}
