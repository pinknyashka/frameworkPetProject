package ru.framework2d.touches.areas;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;
import ru.framework2d.touches.WorldPointer;
import android.util.Log;

public class HandlerTouchSelect extends ComponentsHandler {
	
	private static final String LOG_TAG = "Input";

	ArrayList <TouchableArea> lstTouchableAreas = new ArrayList <TouchableArea>();
	ArrayList <TouchableArea> lstActiveTouchableAreas = new ArrayList <TouchableArea>();
	
	public HandlerTouchSelect() {
		super();

		getContexts();
		loadEntities(entityStorage);
	}
	
	
	
	public Component connectComponent(Component component) {
		
		if (component instanceof WorldPointer) {
			if (!component.lstLinkedHandlers.contains(this)) {
				component.lstLinkedHandlers.add(this);
				for (DataInterface data : component.lstData) data.lstLocalLinkedHandlers.add(this);
			}
			return component;
		} 
		else if (component instanceof TouchableArea) {
			super.connectComponent(component);
			if (!lstTouchableAreas.contains(component)) {
				TouchableArea area = (TouchableArea) component;
				lstTouchableAreas.add(area);
				if (area.isClikable.value) lstActiveTouchableAreas.add(area);
			}
		}	
		return component;
	}

	@Override
	public boolean startWork(int delay) {
		return false;
	}

	@Override
	public boolean transferData(Entity object, DataInterface data) {
		
		boolean transfered = false;
		
		for (Component reflection : object.lstTouches) {
			if (reflection instanceof TouchableArea) {
				TouchableArea touchableArea = (TouchableArea) reflection;
				
				if (data instanceof Position3) {
					if (data.getContext() == touchableArea.position.getContext()) {
						transfered = true;
						Position3 where = (Position3) data;
						touchableArea.position.set(where); 
						touchableArea.position.localCommit(this, touchableArea, data);
					}
				} 
				else if (data instanceof Bool) {
					if (data.getContext() == touchableArea.isClikable.getContext()) {
						transfered = true;
						Bool clikable = (Bool) data;
						touchableArea.isClikable.value = clikable.value;
						
						if (clikable.value) {
							if (!lstActiveTouchableAreas.contains(touchableArea)) lstActiveTouchableAreas.add(touchableArea);
						} 
						else lstActiveTouchableAreas.remove(touchableArea);
						
						touchableArea.isClikable.localCommit(this, touchableArea, data);
						Log.d(LOG_TAG, name + ": set clikable <" + data.getName() + "> in " + touchableArea.entity.name + " to " + clikable.value);
					}
				}
				
			} /*else if (reflection instanceof WorldPointer) {
				//ignore
			}*/
		} 
		return transfered;
	}

	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean handled = false;
		for (ComponentsHandler subHandler : lstSubHandlers) {
			for (Class <Component> handledComponentClass : subHandler.lstProcessingComponents) {
				if (handledComponentClass.isAssignableFrom(reflection.getClass())) {
					if (subHandler.transferLocalData(reflection, data)) {
						handled = true;
					}
				}
			}
		}
		return handled;
	}

	public Component createComponent(Entity master, TagOptions options) {
		return super.createComponent(master, options);
	}
}
