package ru.framework2d.touches.areas;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Point2;
import ru.framework2d.touches.WorldPointer;
import android.util.Log;

public class SubPointer extends ComponentsHandler {
	
	private static final String LOG_TAG = "Input";
	
	public SubPointer () {
		super(1, (Class <Component>) WorldPointer.class.asSubclass(Component.class), 
				(Class <Component>) TouchableArea.class.asSubclass(Component.class));
	}
	
	private ArrayList <TouchableArea> lstAreas = new ArrayList <TouchableArea>();
	
	@Override
	public Component connectComponent(Component component) {
		if (component instanceof TouchableArea) {
			TouchableArea newArea = (TouchableArea) component;
			if (!lstAreas.contains(newArea) && !newArea.entity.isPrototype) {
				lstAreas.add(newArea);
			}
			newArea.lstPointers = new ArrayList <WorldPointer> ();
		}
		return component;
	}
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean handled = false;
		WorldPointer pointer = (WorldPointer) reflection;
		if (data instanceof Bool) {
			if (data.getContext() == pointer.isTouches.getContext()) {
				handled = true;
				if (pointer.isTouches.value) {
					
					for (TouchableArea area : lstAreas) {
						if (area.isClikable.value && area.touched(pointer.position)) {
							Log.d(LOG_TAG, name + ": " + area.entity.name + " selected;");
							
							if (!area.lstPointers.contains(pointer)) {
								area.lstPointers.add(pointer);
								Log.d(LOG_TAG, name + ": "  + pointer.entity.name + " now linked with " 
										+ area.entity.name + " (" + area.lstPointers.size() + ");");
							} //else Log.e(LOG_TAG, name + ": contains pointer before add;");
							
							if (!pointer.lstSelectedAreas.contains(area)) pointer.lstSelectedAreas.add(area);
							
							if (!area.isSelected.value) {
								area.isSelected.value = true;
								
								for (ComponentsHandler subHandler : masterHandler.lstSubHandlers) {
									for (Class <Component> handledComponentClass : subHandler.lstProcessingComponents) {
										
										if (area.getClass().isAssignableFrom(handledComponentClass)) {
											subHandler.transferLocalData(area, area.isSelected);
										}
									}
								}
								area.isSelected.commit(masterEngine, area.entity);
								
							}
						}
					} 
					
				} 
				else {
					
					for (int i = 0; i < pointer.lstSelectedAreas.size(); i++) {
						TouchableArea selectedArea = pointer.lstSelectedAreas.get(i);
					
						
						Log.d(LOG_TAG, name + ": "  + pointer.entity.name + " no more linked with " 
								+ selectedArea.entity.name + "(" + selectedArea.lstPointers.size() + ");");
						
						if (selectedArea.lstPointers.size() == 1) {
							selectedArea.isSelected.value = false;
							
							for (ComponentsHandler subHandler : masterHandler.lstSubHandlers) {
								for (Class <Component> handledComponentClass : subHandler.lstProcessingComponents) {
									
									if (handledComponentClass.isAssignableFrom(selectedArea.getClass())) {
										subHandler.transferLocalData(selectedArea, selectedArea.isSelected);
									}
								}
							}
							Log.d(LOG_TAG, name + ": " + selectedArea.entity.name + " now is not selected;");
							if (!selectedArea.touched(pointer.position)) {
								selectedArea.isSelected.commit(masterEngine, selectedArea.entity);
							}
						} 
						pointer.lstSelectedAreas.remove(i--);
						selectedArea.lstPointers.remove(pointer);
					}
					
				}
			} 
			else if (data.getContext() == pointer.isCurrentlyMoving.getContext()) {
				pointer.isCurrentlyMoving.value = ((Bool) data).value;
				handled = true;
			}
		} 
		else if (data instanceof Point2) {
			if (data.getContext() == pointer.position.getContext()) {
				handled = true;
				for (TouchableArea selectedArea : pointer.lstSelectedAreas) {
					for (ComponentsHandler subHandler : masterHandler.lstSubHandlers) {
						for (Class <Component> handledComponentClass : subHandler.lstProcessingComponents) {
							
							if (handledComponentClass.isAssignableFrom(selectedArea.getClass())) {
								subHandler.transferLocalData(selectedArea, pointer.position);
							}
						}
					}
				}
			}
		}
		return handled;
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
