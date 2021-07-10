package ru.framework2d.touches.areas.drag;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Point2;
import ru.framework2d.touches.WorldPointer;

public class SubDrag extends ComponentsHandler {
	public SubDrag() {
		super(1, (Class <Component>) TouchableAreaDrag.class.asSubclass(Component.class));
	}
	
	@Override
	public boolean transferLocalData(Component component, DataInterface data) {
		
		boolean handled = false;
		TouchableAreaDrag dragedArea = (TouchableAreaDrag) component;
		
		if (data instanceof Bool) {
			if (data.getContext() == dragedArea.draged.getContext()) {
				dragedArea.draged.value = ((Bool) data).value;
				handled = true;
			} 
			else if (data.getContext() == dragedArea.isClikable.getContext()) {
				dragedArea.isClikable.value = ((Bool) data).value;
				handled = true;
			}
		} 
		else if (data instanceof Point2) {
			if (dragedArea.lstPointers.size() == 1) {
				
				WorldPointer pointer = dragedArea.lstPointers.get(0);
				
				dragedArea.position.set(pointer.position.x.value - dragedArea.dragPoint.x.value,
										pointer.position.y.value - dragedArea.dragPoint.y.value);
				dragedArea.touchMove.set(pointer.position, dragedArea.position);
				
				dragedArea.position.commit(masterEngine, dragedArea.entity);
				dragedArea.touchMove.commit(masterEngine, dragedArea.entity);
				handled = true;
				
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
