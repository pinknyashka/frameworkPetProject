package ru.framework2d.graphics.draw;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Entity;
import ru.framework2d.data.DataInterface;
import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.graphics.OutputGraphicalInterface;
import android.graphics.Canvas;
import android.util.Log;

public class HandlerDraw extends ComponentsHandler {

	GraphicalPeriphery graphicalPeriphery;

    private static final String LOG_TAG = "Output";
    
	public HandlerDraw() {
		super();
		
		for (Engine engine : enginesManager.lstEngines) {
			if (engine instanceof GraphicalPeriphery) {
				graphicalPeriphery = (GraphicalPeriphery) engine; 
				break;
			}
		}
		
		getContexts();
		loadEntities(entityStorage);
	}
	
	
	
	void draw(Canvas c) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean toHandleComponent(Component object) {
		
		drawObject((OutputGraphicalInterface) object);
		
		return false;
	}
	

	
	void drawObject(OutputGraphicalInterface g) {
		
		if (g.isVisible.value && g.isLocalVisible.value) {
			
			for (ComponentsHandler subHandler : g.lstProcessingSubHandlers) {
				subHandler.toHandleComponent(g);
			}
			
		}
	}
	
	
	
	private int transferGraphicalsNum = 0;
	
	@Override
	public boolean transferData(Entity entity, DataInterface data) {
		
		//Log.d(LOG_TAG, name + ": transfer data " + data.getName() + " in " + entity.name);
		
		boolean transfered = false;
		
		long timerMark1 = System.currentTimeMillis();
		
		transferGraphicalsNum = entity.lstGraphicals.size();
		for (int componentNo = 0; componentNo < transferGraphicalsNum; componentNo++) {
			
			OutputGraphicalInterface graphical = entity.lstGraphicals.get(componentNo);
			
			boolean currentlyTransfered = false;
			int subNo = 0;
			int subHandlersNum = graphical.lstProcessingSubHandlers.size();
			
			while (!currentlyTransfered && subNo < subHandlersNum) {
				currentlyTransfered = graphical.lstProcessingSubHandlers.get(subNo)
											.transferLocalData(graphical, data);
				subNo++;
			}
			
			if (currentlyTransfered) transfered = true;
			
		} 
		
		//Log.d(LOG_TAG, name + ": transfer done in " + (System.currentTimeMillis() - timerMark1) + " ms;");
		
		return transfered;
	}
	
	
	
	private int localTransferSubNum = 0;
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean transfered = false;
		
		long timerMark1 = System.currentTimeMillis();
		OutputGraphicalInterface graphical = (OutputGraphicalInterface) reflection;
		
		localTransferSubNum = graphical.lstLinkedSubHandlers.size();
		
		for (int subNo = 0; subNo < localTransferSubNum; subNo++) {
			ComponentsHandler subHandler = graphical.lstLinkedSubHandlers.get(subNo);
			if (subHandler.transferLocalData(graphical, data)) {
				transfered = true;
			} 
			else {
				graphical.lstLinkedSubHandlers.remove(subNo--);
				localTransferSubNum--;
			}
		}
		
		Log.d(LOG_TAG, name + ": transfer done in " + (System.currentTimeMillis() - timerMark1) + " ms;");
		return transfered;
	}
	
	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		for (ComponentsHandler subHandler : lstSubHandlers) {
			
			for (Class <Component> handledComponentClass : subHandler.lstProcessingComponents) {
				
				if (handledComponentClass.isAssignableFrom(component.getClass())) {
					
					OutputGraphicalInterface graphical = (OutputGraphicalInterface) component; 
					
					if (!component.entity.isPrototype) {
						
						graphicalPeriphery.addGraphical(graphical);
					}
					
					if (!graphical.lstProcessingEngines.contains(this)) {
						graphical.lstProcessingEngines.add(this);
					}
					if (!graphical.lstProcessingSubHandlers.contains(subHandler)) {
						graphical.lstProcessingSubHandlers.add(subHandler);
					}
					
					setWorkState(true);
				}
			}
		}
		
		return component;
	}
	
}
