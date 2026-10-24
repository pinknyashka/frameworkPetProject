package ru.framework2d.core;

import java.util.Timer;
import java.util.TimerTask;

import ru.framework2d.compat.Log;

public class EnginesScheduler extends Engine {
	
	
	public long currentTick;

	
	
	public EnginesScheduler() {
		super();
		
		// Daemon timer: it must not keep the JVM alive after the render bridge closes the window.
		taskTimer = new Timer("Scheduler", true);
		
		currentTick = 0;
	}
	
	
	/**
	 * Call for start scheduling. Possibly that scheduler will ignore this call if drawing is in process
	 * @param delay - delay before it's starts 
	 */
	@Override
	public boolean startWork(int delay) {
		
		if (!isInProcess() && haveWork() && !isDrawCalled) {
			
			if (EnginesManager.PARALLEL_WORK) {
				taskTimer.schedule(new ScheduleTask(), delay);
			}
			else {
				iterate();
			}
			return true;
		}
		return false;
	}

	class ScheduleTask extends TimerTask {
		
	    public void run() {
	    	if (!isDrawCalled) {
	    		iterate();
	    	}
	    }

    }	
    
	
	
	private long logTimerMark = System.currentTimeMillis();
	
	public boolean isDrawCalled = false;

	@Override
	protected void doWork() {
		
		currentTick++;
		logTimerMark = System.currentTimeMillis();
		
	}
	
	@Override
	protected void onFinish() {
		if (!isDrawCalled) {
			isDrawCalled = true;
			startDraw();
		}
	}
	

	
	/**
	 * Direct finished part of his work, and ask for repeat
	 * @param direct - direct asking for repeat
	 */
	public void askToRepeat(Engine direct) {
		
		if (direct.haveWork()) {
			
			if (!direct.isInProcess()) {
				
				Log.d("core", "Scheduler: Direct is finished iteration; start new one;");
				int delay = 1;
				direct.startWork(delay);
			} 
			else {
				Log.w("core", "Scheduler: Direct ignore command cz was in process;");
			}
    	}
	}
	
	
	
	public long mark = System.currentTimeMillis();
	
	private void startDraw() {
		
		// Draw is driven by the libGDX render loop; no surface invalidation needed.
		mark = System.currentTimeMillis();
	}
	
	/**
	 * Call this function to notify Scheduler that draw is over. Starts next iteration
	 */
	public void overDraw() {
		
		isDrawCalled = false;
		
		Log.d("core", "Scheduler: All step done in " + 
				(System.currentTimeMillis() - logTimerMark) + " ms;");
		
		int delay = 3;
		startWork(delay);
	}
}
