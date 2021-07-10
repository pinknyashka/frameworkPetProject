package ru.framework2d.graphics.draw;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.DataContext;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.graphics.OutputGraphicalInterface;
import android.util.Log;

public class SubBasicGraphical extends ComponentsHandler {
	
	private static final String LOG_TAG = "Output";
	
	private GraphicalPeriphery graphicalPeriphery;
	
	public SubBasicGraphical() {
		super(1, 
				(Class <Component>) OutputGraphicalInterface.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			
			if (engine instanceof GraphicalPeriphery) {
				
				graphicalPeriphery = (GraphicalPeriphery) engine; 
				
				break;
			}
		}
	}

	
	
	private boolean transfered;
	
	private DataContext transferedContext;
	
	private OutputGraphicalInterface graphical;
	
	@Override
	public boolean transferLocalData(Component component, DataInterface data) {
		
		transfered = false;
		
		graphical = (OutputGraphicalInterface) component;
		transferedContext = data.getContext(); 
		
		if (transferedContext == graphical.position.getContext()) {
			transfered = true;
			if (data instanceof Position3) graphical.position.set((Position3) data);
			else if (data instanceof Point2) {
				if (data instanceof Position2) graphical.position.set((Position2) data);
				else graphical.position.set((Point2) data);
			} 
			graphicalPeriphery.update();
		} 
		else if (transferedContext == graphical.localPos.getContext()) {
			transfered = true;
			if (data instanceof Vector2) {
				graphical.localPos.setAlpha((float)((Vector2) data).computeAngle());
				Log.d(LOG_TAG, masterEngine.name + ": set " + graphical.getShortClassName() + ".angle <" 
								+ data.getContext().name + "> to " 
								+ ((Vector2) data).getX() + " : " + ((Vector2) data).getY());
				graphicalPeriphery.update();
			} 
			else if (data instanceof Position3) {
				graphical.localPos.set((Position3) data);
			} 
			else if (data instanceof Point2) {
				if (data instanceof Position2) graphical.localPos.set((Position2) data);
				else graphical.localPos.set((Point2) data);
			} 
		}
		
		if (data instanceof Fractional) {
			if (transferedContext == graphical.transparency.getContext()) {
				transfered = true;
				graphical.transparency.value = ((Fractional) data).value;
				/*Log.d(LOG_TAG, masterHandler.name + ": set " + graphical.master.name + ".transparancy <" 
								+ data.getContext().name + "> to " + graphical.transparency.value);*/
				graphicalPeriphery.update();
			} 
			else if (transferedContext == graphical.localTransparency.getContext()) {
				transfered = true;
				graphical.localTransparency.value = ((Fractional) data).value;
				/*Log.d(LOG_TAG, masterHandler.name + ": set " + graphical.master.name 
								+ ".localTransparancy <" + data.getContext().name + "> to " 
								+ graphical.transparency.value);*/
				graphicalPeriphery.update();
			}
		} 
		else if (data instanceof Bool) {
			if (transferedContext == graphical.isVisible.getContext()) {
				transfered = true;
				graphical.isVisible.value = ((Bool) data).value;
				/*Log.d(LOG_TAG, masterHandler.name + ": set " + graphical.master.name + ".isVisible <" 
								+ data.getContext().name + "> to " + graphical.isVisible.value);*/
				graphicalPeriphery.update();
			} 
			else if (transferedContext == graphical.isLocalVisible.getContext()) {
				transfered = true;
				graphical.isLocalVisible.value = ((Bool) data).value;
				/*Log.d(LOG_TAG, masterHandler.name + ": set " + graphical.master.name + ".isLocalVisible <" 
								+ data.getContext().name + "> to " + graphical.isVisible.value);*/
				graphicalPeriphery.update();
			}
		} 
		
		return transfered;
	}

	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		return false;
	}
}
