package ru.framework2d.touches;

import android.view.MotionEvent;

public interface TouchPeriphery {
	
	public int getMaximumTouchesNumber();
	
	public void setTouchHandler(MotionEventListener handler);

	
	
	public boolean gotUnhandledEvent();

	public void setEventState(boolean unhandled);

	public MotionEvent getEvent();
	
	
	
	public void toHandleEvent(MotionEvent event);
	
}
