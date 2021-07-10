package ru.framework2d.touches;

import ru.framework2d.core.Engine;
import ru.framework2d.core.GameSurface;
import android.view.MotionEvent;

public class DirectTouches extends Engine implements TouchPeriphery {
	

	
	public DirectTouches() {
		super();
		
		if (resources.gameSurface != null) {
			((GameSurface) resources.gameSurface).setTouchPeriphery(this);	
		}
		
		loadEntities(entityStorage);
	}
	
	
	
	@Override
	protected void onFinish() {
		masterEngine.subEngineWorkDone(this);
	}


	
	public MotionEventListener touchHandler;
	
	public boolean gotUnhandledEvent = false;
	
	private MotionEvent event;
	
	public void toHandleEvent(MotionEvent event) {
		
		setWorkState(true);
		this.event = event;
		gotUnhandledEvent = true;
		if (touchHandler != null) touchHandler.handleMotionEvent(event);
		enginesScheduler.startWork(3);
	}



	public int getMaximumTouchesNumber() {
		return 10;
	}



	public boolean gotUnhandledEvent() {
		return gotUnhandledEvent;
	}

	public void setEventState(boolean unhandled) {
		gotUnhandledEvent = unhandled;
	}



	public MotionEvent getEvent() {
		return event;
	}



	public void setTouchHandler(MotionEventListener handler) {
		touchHandler = handler;
	}

}
