package ru.framework2d.touches.screen;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.TagOptions;
import ru.framework2d.touches.MotionEventListener;
import ru.framework2d.touches.TouchPeriphery;
import ru.framework2d.touches.WorldPointer;
import android.util.Log;
import android.view.MotionEvent;

public class HandlerTouchScreen extends ComponentsHandler implements MotionEventListener {
	
	private static final String LOG_TAG = "Input";
	 
	public static boolean currently = false;
	
	MotionEvent event;
	
	TouchPeriphery touchPeriphery;
	
	
	
	ArrayList <WorldPointer> lstPointers = new ArrayList <WorldPointer>(); 
	
	ArrayList <TouchFinger> lstTouches = new ArrayList <TouchFinger>(); 
	ArrayList <TouchFinger> lstActiveTouches = new ArrayList <TouchFinger>();
	
	ArrayList <TouchFinger> lstMovedTouches = new ArrayList <TouchFinger>();
	ArrayList <TouchFinger> lstChangeStateTouches = new ArrayList <TouchFinger>();

	
	
	public HandlerTouchScreen() {
		super(2, (Class <Component>) TouchFinger.class.asSubclass(Component.class),
				(Class <Component>) WorldPointer.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			
			if (engine instanceof TouchPeriphery) {
				
				touchPeriphery = (TouchPeriphery) engine;
				touchPeriphery.setTouchHandler(this);
				break;
			}
		}
		
		if (touchPeriphery == null) Log.w("Input", "Touch periphery is empty");
		
		getContexts();
		loadEntities(entityStorage);
	}
	
	@Override
	public boolean loadEntities(EntityStorage objectsModel) {
		findOrCreateTouches(objectsModel, enginesManager);
		return true;
	}
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof WorldPointer) {
			lstPointers.add((WorldPointer) component);
		} 
		else if (component instanceof TouchFinger) {
			lstTouches.add((TouchFinger) component);
		}	
		return component;
	}


	
	void actionDown(MotionEvent event) {
		/*nothing*/
	}
	
	void actionPointerDown(MotionEvent event) {
		
		int i = 0;
		while (lstActiveTouches.contains(lstTouches.get(i))) {
			i++;
		}
		
		TouchFinger currentTouch = lstTouches.get(i);
		
		currentTouch.pointerId = event.getPointerId(event.getActionIndex());
		currentTouch.position.set(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
		
    	/*Lock lock = new ReentrantLock();
		if (lock.tryLock()) {
	    	try {*/
	        	if (!block) {
		    		block = true;
		    		if (!lstMovedTouches.contains(currentTouch)) {
		    			lstMovedTouches.add(currentTouch);	
		    		}
		    		
		    		block = false;
	        	}
	    	/*} finally {
	    		lock.unlock();
	    	}
		}*/
		//lstMovedTouches.add(currentTouch);
		
		currentTouch.oldPosition.set(currentTouch.position);
		
		currentTouch.touched.value = true;
		
		if (lstActiveTouches.size() >= currentTouch.pointerId) {
			lstActiveTouches.add(currentTouch.pointerId, currentTouch);
		} 
		else lstActiveTouches.add(currentTouch);
		
		lstChangeStateTouches.add(currentTouch);
	}
	
	void actionMove(MotionEvent event) {
		
		for (int fingerNo = 0; fingerNo < lstActiveTouches.size(); fingerNo++) {
			lstActiveTouches.get(fingerNo).oldPosition.set(lstActiveTouches.get(fingerNo).position);
			lstActiveTouches.get(fingerNo).position.set(event.getX(fingerNo), event.getY(fingerNo));
			
			/*Lock lock = new ReentrantLock();
			if (lock.tryLock()) {
		    	try {*/
		        	if (!block) {
			    		block = true;
			    		if (!lstMovedTouches.contains(lstActiveTouches.get(fingerNo))) {
			    			lstMovedTouches.add(lstActiveTouches.get(fingerNo));	
			    		}
			    		block = false;
		        	}
		    	/*} finally {
		    		lock.unlock();
		    	}
			}*/
			//lstMovedTouches.add(lstActiveTouches.get(fingerNo));
		}
	}
	
	void actionUp(MotionEvent event) {
		/*nothing*/
	}
	
	void actionPointerUp(MotionEvent event) {
		
		TouchFinger dropedFinger = lstActiveTouches.get(event.getActionIndex());
		
		dropedFinger.touched.value = false;
		lstActiveTouches.remove(dropedFinger);
		lstChangeStateTouches.add(dropedFinger);
	}

	
	
	private boolean block = false;
	
    protected void iterate() {
    	//transfer changes from update
    	long timerMark2 = System.currentTimeMillis();
    	
    	/*for (int fingerNo = 0; fingerNo < lstMovedTouches.size(); fingerNo++) {
    		lstMovedTouches.get(fingerNo).isCurrentlyMoving.value = true;
    		lstMovedTouches.get(fingerNo).isCurrentlyMoving.commit(this, lstMovedTouches.get(fingerNo).master);
    	}*/
    	/*Lock lock = new ReentrantLock();
		if (lock.tryLock()) {
	    	try {*/
	        	if (!block) {
		    		block = true;
		        	for (int fingerNo = 0; fingerNo < lstMovedTouches.size(); fingerNo++) {
		        		lstMovedTouches.get(fingerNo).isCurrentlyMoving.value = true;
		        		lstMovedTouches.get(fingerNo).isCurrentlyMoving.commit(this, lstMovedTouches.get(fingerNo).entity);
		        	}
		    		block = false;
	        	}
	    	/*} finally {
	    		lock.unlock();
	    	}
		}*/
    	/*for (InputTouchFinger finger : lstMovedTouches) {
    		finger.isCurrentlyMoving.value = true;
    		finger.isCurrentlyMoving.commit(this, finger.master);
    	}*/
	        	
    	while (!lstMovedTouches.isEmpty()) {
    		lstMovedTouches.get(0).position.commit(this, lstMovedTouches.get(0).entity);
    		lstMovedTouches.get(0).isCurrentlyMoving.value = false;
    		lstMovedTouches.get(0).isCurrentlyMoving.commit(this, lstMovedTouches.get(0).entity);
			lstMovedTouches.remove(0); 
    	}
    	
    	while (!lstChangeStateTouches.isEmpty()) {
	    	lstChangeStateTouches.get(0).touched.commit(this, lstChangeStateTouches.get(0).entity);
			lstChangeStateTouches.remove(0); 
    	}
    	
    	Log.d(LOG_TAG, name + ": step done in " + (System.currentTimeMillis() - timerMark2) + " ms;");
    	
		setWorkState(false);
		
    	masterEngine.subEngineWorkDone(this);
	}

    
    
	public Component createComponent(Entity master, TagOptions options) {
		
		if (options.name.contentEquals("InputTouchFinger")) {
			
			TouchFinger newFinger = new TouchFinger(lstTouches.size());
			
			newFinger.setFactory(this);
			newFinger.lstLinkedHandlers.add(this);
			
			if (master != null) {
				newFinger.entity = master;
				master.lstTouches.add(newFinger);
			}
			lstTouches.add(newFinger);
			setContextToComponent(newFinger);
			newFinger.setOptions(options);
			
			return newFinger; 
		}
		
		return super.createComponent(master, options);
	}
	

	
	private void findOrCreateTouches(EntityStorage objectsModel, EnginesManager enginesModel) {
		
		int createdObjects = 0;
		
		for (Entity object : objectsModel.lstEntities) {
			
			if (object.name.contains("pointer")) {
				
	    		if (object.lstTouches.isEmpty()) {
	    			
	    			if (lstTouches.size() > createdObjects) {
		    			object.lstTouches.add(lstTouches.get(createdObjects));
		    			lstTouches.get(createdObjects).entity = object;
	    			} 
	    			else {
	    				TouchFinger newFinger = (TouchFinger) enginesModel.createComponentInEntity(object, new TagOptions("InputTouchFinger"));
	    				
	    				if (newFinger != null) lstTouches.add(newFinger);
	    			}
	    			
		    	} 
	    		else {
	    			
		    		for (Component touch : object.lstTouches) {
		    			
		    			if (touch instanceof TouchFinger) {
		    				if (!lstTouches.contains(touch)) {
		    					lstTouches.add((TouchFinger) touch);
		    				}
		    			}
		    			
		    		}
		    		
	    		}
	    		
				createdObjects++;
			}
		}
		
		while (createdObjects < touchPeriphery.getMaximumTouchesNumber()) {
			
			Entity pointer = new Entity("pointer" + createdObjects);
			objectsModel.lstEntities.add(pointer);
			if (lstTouches.size() == touchPeriphery.getMaximumTouchesNumber()) {
	    		pointer.lstTouches.add(lstTouches.get(createdObjects));
	    		lstTouches.get(createdObjects).entity = pointer;
			} 
			else {
				TouchFinger newFinger = (TouchFinger) enginesModel.createComponentInEntity(pointer, new TagOptions("InputTouchFinger"));
				
				if (newFinger != null && !lstTouches.contains(newFinger)) {
					lstTouches.add(newFinger);
				}
				else {
					//Log.d(LOG_TAG,"here: " + lstTouches.size() + "; " + pointer.name);
					newFinger = (TouchFinger) createComponent(pointer, new TagOptions("InputTouchFinger"));
					if (newFinger != null && !lstTouches.contains(newFinger)) {
						lstTouches.add(newFinger);
					}
				}
			}
			createdObjects++;
		}
	}

	public void handleMotionEvent(MotionEvent event) {
		
		if (event != null && touchPeriphery.gotUnhandledEvent()) {
			
			touchPeriphery.setEventState(false);
	    	
	    	int action = event.getActionMasked();
	    	
	    	currently = true;
	    	switch (action) {
	            case MotionEvent.ACTION_DOWN: 
            		actionDown(event);
	            case MotionEvent.ACTION_POINTER_DOWN: 
	            	actionPointerDown(event);
	            break; 
	            case MotionEvent.ACTION_MOVE: 
	            	actionMove(event);
	            break;
	            case MotionEvent.ACTION_UP: 
	            	actionUp(event);
	            case MotionEvent.ACTION_POINTER_UP:
	            	actionPointerUp(event);
	            break;
	        }
	    	currently = false;
		}
		setWorkState(!lstMovedTouches.isEmpty() || !lstChangeStateTouches.isEmpty());
	}

}
