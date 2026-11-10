package ru.framework2d.touches;
import ru.framework2d.core.Engine;
import ru.framework2d.core.agents.EngineLocks;
import ru.framework2d.core.agents.EngineResource;
import ru.framework2d.core.agents.LockBusyException;
import ru.framework2d.core.agents.LockMode;
import ru.framework2d.core.agents.ResourceLease;
import ru.framework2d.compat.Log;
import ru.framework2d.compat.MotionEvent;

public class DirectTouches extends Engine implements TouchPeriphery {
	

	

	public DirectTouches() {
		super();
		
		loadEntities(entityStorage);
	}
	
	
	
	@Override
	protected void onFinish() {
		masterEngine.subEngineWorkDone(this);
	}


	
	public MotionEventListener touchHandler;
	
	public boolean gotUnhandledEvent = false;
	
	private MotionEvent event;
	
	private long lastLockWarning;
	
	public void toHandleEvent(MotionEvent event) {
		
		ResourceLease lease;
		try {
			lease = EngineLocks.tryAcquire(EngineResource.TOUCH, LockMode.WRITE, "touch event");
		} catch (LockBusyException e) {
			long now = System.currentTimeMillis();
			if (now - lastLockWarning > 2000L) {
				Log.w("core", "Touch event dropped: " + e.describe());
				lastLockWarning = now;
			}
			return;
		}
		
		try {
			setWorkState(true);
			this.event = event;
			gotUnhandledEvent = true;
			if (touchHandler != null) touchHandler.handleMotionEvent(event);
			enginesScheduler.startWork(3);
		} finally {
			lease.close();
		}
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
