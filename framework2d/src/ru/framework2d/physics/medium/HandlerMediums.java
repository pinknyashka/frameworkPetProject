package ru.framework2d.physics.medium;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.InteractionHandler;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Movement3;
import ru.framework2d.data.Position3;
import android.util.Log;

public class HandlerMediums extends ComponentsHandler {
	
	ArrayList <PhysicalBody> lstBody = new ArrayList <PhysicalBody>();
	ArrayList <PhysicalMedium> lstMediums = new ArrayList <PhysicalMedium>();

	ArrayList <PhysicalBody> lstActiveBody = new ArrayList <PhysicalBody>();

	private static final String LOG_TAG = "Physical";

	public HandlerMediums() {
		super();
		
		getContexts();
		loadEntities(entityStorage);
	}

	
	
	@Override
	protected void iterate() {
		
		findInteractions();
		
		setWorkState(!lstActiveBody.isEmpty());
		
		masterEngine.subEngineWorkDone(this);
	}
	
	protected void findInteractions() {
		
		Log.d(LOG_TAG, name + ": run; active objects number = " + lstActiveBody.size());
		
		for (int activeNo = 0; activeNo < lstActiveBody.size(); activeNo++) {
			PhysicalBody activeBody = lstActiveBody.get(activeNo);
			
			//Log.d(TAG, name+": got active "+activeBody.getName()+" in "+activeBody.pntrMaster.name+"; "+activeBody.enabled.value);
			
			if (activeBody.isActive()) {
				
				for (PhysicalMedium medium : lstMediums) {
					
					if (medium.isEnabled.value) {
						
						InteractionHandler handlerForPhysicals = medium.getInteractionHandler(activeBody.getComponentClass());
						
						if (handlerForPhysicals != null) {
							
							if (handlerForPhysicals.isInteractionHappened(medium, activeBody)) {
								
								handlerForPhysicals.toHandleInteraction(medium, activeBody);
							}
						}
					}
				}
				
			} 
			else {
				Log.d(LOG_TAG, name + ": remove " + activeBody.getName() + " in " + activeBody.entity.name + 
						"; enabled = " + activeBody.isEnabled.value);
				lstActiveBody.remove(activeBody); 
				activeNo--;
			}
		}
		Log.d(LOG_TAG, name + ": finish;");
	}
	

	
	@Override
	public boolean transferData(Entity object, DataInterface data) {
		Log.d(LOG_TAG, name + ": transfer: " + object.name + ": " + data.getName());
		 
		boolean transfered = false;
		 
		for (Component reflection : object.lstPhysicals) {
			if (reflection instanceof PhysicalBody) {
				PhysicalBody body = (PhysicalBody) reflection;
				
				if (data instanceof Movement3) {
					if (data.getContext() == body.movement.getContext()) {
						transfered = true;
						body.movement.setMovement((Movement3) data);
						
						if (((Movement3) data).isMoving()) {
							
							if (!lstActiveBody.contains(body)) lstActiveBody.add(body);
							
							setWorkState(true);
						} 
					}
				} 
				else if (data instanceof Position3) { 
					if (data.getContext() == body.position.getContext()) {
						transfered = true;
						body.position.set((Position3) data);
					}
				} 
				else if (data instanceof Bool) { 
					if (data.getContext() == body.isEnabled.getContext()) {
						transfered = true;
						//Log.d(LOG_TAG, name + ": set " + data.getName() + " to " + ((Bool) data).value);
						body.isEnabled.value = ((Bool) data).value;
						body.movement.stop();
						lstActiveBody.remove(body);
						setWorkState(!lstActiveBody.isEmpty());
					}
				}
			}
		} 
		return transfered;
	}

	
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof PhysicalMedium) {
			
			if (!lstMediums.contains((PhysicalMedium) component)) {

				lstMediums.add((PhysicalMedium) component);
			}
		} 
		else if (component instanceof PhysicalBody) {
			
			PhysicalBody body = (PhysicalBody) component; 
			
			if (!lstBody.contains(body)) {

				lstBody.add(body);
				if (body.isActive()) lstActiveBody.add(body);
			}
		}	
		
		return component;
	}

}
