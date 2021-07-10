package ru.framework2d.touches.areas.switcher;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.touches.InputTouchInterface;
import android.util.Log;

public class SubSwitcher extends ComponentsHandler {
	
	public SubSwitcher() {
		super(1, (Class <Component>) TouchableAreaSwitcher.class.asSubclass(Component.class));
		
	}
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean handled = false;
		TouchableAreaSwitcher switcher = (TouchableAreaSwitcher) reflection;
		
		if (data.getContext() == switcher.isSelected.getContext()) {
			if (data instanceof Bool) {
				handled = true; // passed
				
				if (switcher.isSelected.value) {
					switcher.isOn.value = !switcher.isOn.value;
					switcher.isOn.commit(masterEngine, switcher.entity);
					for (ComponentsHandler subHandler : masterHandler.lstSubHandlers) { 
						for (InputTouchInterface touch : switcher.entity.lstTouches) {
							for (Class <Component> handledComponentClass : subHandler.lstProcessingComponents) {
								
								//if (subHandler.handledComponentClass.isAssignableFrom(touch.getClass()) && subHandler != this) {
								if (handledComponentClass.isAssignableFrom(touch.getClass())  && subHandler != this) {
									subHandler.transferLocalData(touch, switcher.isOn);
								}
							}
						}
					}
					Log.d("Input", fullName + ": Set " + switcher.entity.name + ".isOn to " + 
							switcher.isOn.value);
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
