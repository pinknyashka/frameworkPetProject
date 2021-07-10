package ru.framework2d.core;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

// interface which controls the engines manager
public abstract class Engine {	// интерфейс с которым работает планировщик
	
	public String name = "";

	public String fullName = "";
	

	
	/*
	 * Every 4 static fields sets by EnginesManager in his own creation
	 * They all exist in single copy and will never change during whole game 
	 */
	protected static GameResources resources;
	
	protected static EntityStorage entityStorage;
	
	protected static EnginesManager enginesManager;
	
	protected static EnginesScheduler enginesScheduler;
	
	
	
	public long timerMark;
	
	
	
	protected Timer taskTimer; 

	
	
	public Engine() {
		
		name = this.getClass().getSimpleName();

		timerMark = System.currentTimeMillis(); 
		
		if (EnginesManager.PARALLEL_WORK) {
			taskTimer = new Timer(name);
		}
	}
	
	
	
	public boolean loadEntities(EntityStorage entityStorage) {
		return false;
	}

	
	
	boolean breaked = false; 
	
	public boolean askForBreak(int timeForBreak) {
		
		breaked = true;
		
		return breaked;
	}

	
	
	private boolean haveWork = false;
	
	public boolean haveWork() {
		return haveWork;
	}
	
	public void setWorkState(boolean haveWork) {
		
		this.haveWork = haveWork;
		
		if  (masterEngine != null) {
			
			if (haveWork) {
				masterEngine.setWorkState(true);
			} 
			else {
				boolean gotWork = false;
				
				for (Engine subengine : masterEngine.lstSubEngines) {
					if (subengine.haveWork()) {
						gotWork = true;
						break;
					}
				}
				
				if (!gotWork) masterEngine.setWorkState(false);
			}
		}
	}
	
	
	
	private boolean inProcess = false;
	
	public boolean isInProcess() {
		return inProcess;
	}
	
	public void setProcessState(boolean inProcess) {
		this.inProcess = inProcess;
	}
	
	
	
	public int averageRunTime = 0;
	
	
	
	public static Engine createEngine(TagOptions options, Class<Engine> engineClass) {
		
		Engine newEngine = null;
		
		try {
			
			if (engineClass != null) {
				Log.d("core", "gonna create engine " + engineClass.getName());
				newEngine = engineClass.newInstance();
			}
			else Log.e("core", "Engine class is null");
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		if (newEngine != null) {
			newEngine.setOptions(options);
		}
		
		return newEngine;
	} 
	
	
	
	public void setOptions(TagOptions options) {
		// let to simply override this
	}


	
	public Engine masterEngine;
	
	public ArrayList <Engine> lstSubEngines = new ArrayList <Engine>();
	
	public void addSubengine(Engine subengine) {
		
		if (!lstSubEngines.contains(subengine)) {

			lstSubEngines.add(subengine);
			subengine.masterEngine = this; 
		}
	}
	
	public ArrayList <Engine> lstStartedSubEngines = new ArrayList <Engine>();

	
	
	/**
	 * Call to iterate current procedure 
	 * @param delay - delay before start iteration in parallel mode
	 * @return the state of busy, false if there is nothing to do
	 */
	public boolean startWork(int delay) {
		
		if (haveWork) {
		
			if (EnginesManager.PARALLEL_WORK) {
				taskTimer.schedule(new WorkTask(), delay);
			}
			else {
				iterate();
			}
			return true;
			
		}
		return false;
	}

	public class WorkTask extends TimerTask {
	    public void run() {
	    	iterate();
    	}
    }
	
	private String enginesLog;
	
	protected void iterate() {
		
    	setProcessState(true);
    	
    	
    	
    	doWork();
    	
    	
    	
    	enginesLog = "";
    	for (Engine handler : lstSubEngines) {
    		enginesLog += handler.name + "; ";
    	}
    	
    	Log.d("core", name + ": start with " + lstSubEngines.size() + " subengines: " + enginesLog);
    	
    	enginesLog = name + ": finish; ";
    	
    	
    	
    	block = false;
    	
    	if (EnginesManager.PARALLEL_WORK) {
    		
	    	int subEnginesWontWorkNum = 0;
	    	
	    	lstStartedSubEngines.addAll(lstSubEngines);
			
			for (Engine subEngine : lstSubEngines) {
				
				if (!subEngine.startWork(1)) {
					subEngineWorkDone(subEngine);
					subEnginesWontWorkNum++;
				} 
				
			}
			
	    	Log.d("core", name + ": " + subEnginesWontWorkNum + " subengines wont work;");
	    	
    	} 
    	else {
    		
    		int handlersWontWorkNum = 0;
    		
    		for (Engine handler : lstSubEngines) {
    			if (!handler.startWork(0)) {
    				handlersWontWorkNum++;
    			} else enginesLog += handler.name + ", ";  
    		}

    		if (handlersWontWorkNum == lstSubEngines.size()) {
    			Log.d("core", enginesLog + handlersWontWorkNum + " handlers wont work;");
				setWorkState(false);
			}
    		else Log.d("core", enginesLog + " works;");
    		
			workDone();
    		
    	}
    	
    	
	}
	
	
	
	protected void doWork() {
		
	}


	
	protected void onFinish() {
		
	}

	
	
	protected final ReentrantLock perHandlerLock = new ReentrantLock();

	public void subEngineWorkDone(Engine subEngine) {
		
		if (EnginesManager.PARALLEL_WORK) {
			
			try {
				
				if (perHandlerLock.tryLock() || perHandlerLock.tryLock(5, TimeUnit.MILLISECONDS) 
						|| perHandlerLock.tryLock(5, TimeUnit.MILLISECONDS)) {
					
					try {
						
						lstStartedSubEngines.remove(subEngine);
						
						Log.d("core", name + ": " + subEngine.name + " done; " + 
								lstStartedSubEngines.size() + " handler(s) still working; ");
						
						if (lstStartedSubEngines.isEmpty()) {
							workDone();
						} 
						else Log.d("core", name + ": " + lstStartedSubEngines.get(0).name);
						
					} finally {
						perHandlerLock.unlock();
					}
					
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		} 
	}
	
	
	
	protected boolean block = false;
	
	protected void workDone() {
		
    	onFinish();
    	
		Lock lock = new ReentrantLock();
		
		if (lock.tryLock()) {
			
	    	try {
	    		
	        	if (!block) {
	        		
		    		block = true;
		    		setProcessState(false);
		    		
		    		if (masterEngine != null) masterEngine.subEngineWorkDone(this);
	        	}
	        	
	    	} finally {
	    		lock.unlock();
	    	}
			
		}
	}
}
