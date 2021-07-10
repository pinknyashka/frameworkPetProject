package ru.framework2d.physics;

import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import android.util.Log;

public class IntervalPhysics extends Engine {

	private static final String LOG_TAG = "Physical";

	//static parameters:
	//proportion between pixels and physical model
	public static double PHYSICAL_UNIT = 1.0d;
	//maximum calculation interval
	public static int INTERVAL_LIMIT = 90;
	//maximum impulse that you can give to shape
	public static float IMPULSE_LIMIT = 50;
	//basic interval that speed will be 1 to 1 
	public static int BASE_PHYSICAL_INTERVAL = 17;

	public static int MINIMAL_CALCULATION_INTERVAL = 2;
	
	
	//dynamic variables:
	public static long START_ITERATION_TIMER_MARK = 0; 

	public static int PROCESSED_INTERVAL = 17;
	
	public static int REMAINING_PHYSICAL_INTERVAL = 17;

	
	
	
    public IntervalPhysics() {
    	super();
    	
    	START_ITERATION_TIMER_MARK = System.currentTimeMillis();
	}
    
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		return true;
	}

	
	
	@Override
	public void setOptions(TagOptions options) {
		
		for (Property property : options.lstPropertys) { 
			
			String name = property.name;
			String value = property.data.toString();
			
			if (name.equalsIgnoreCase("ph_unit")) {
				PHYSICAL_UNIT = (float) Double.parseDouble(value);
			} 
			else if (name.equalsIgnoreCase("interval_limit")) {
				INTERVAL_LIMIT = (int) Double.parseDouble(value);
			} 
			else if (name.equalsIgnoreCase("impulse_limit")) {
				IMPULSE_LIMIT = (float) Double.parseDouble(value);
			}
			
		}
	}
	


	@Override 
	protected void doWork() {

		int delayFromLastIteration = (int)(System.currentTimeMillis() - START_ITERATION_TIMER_MARK);
		
    	START_ITERATION_TIMER_MARK = System.currentTimeMillis();
		
		if (delayFromLastIteration <= MINIMAL_CALCULATION_INTERVAL) {
			delayFromLastIteration += MINIMAL_CALCULATION_INTERVAL + 1;
		}
		
		REMAINING_PHYSICAL_INTERVAL += delayFromLastIteration;
		
		Log.d(LOG_TAG, name + ": increase remaining interval for " + delayFromLastIteration +
				 " ms to " + REMAINING_PHYSICAL_INTERVAL + " ms;");
		
    	if (REMAINING_PHYSICAL_INTERVAL > INTERVAL_LIMIT) {   
    		
    		Log.w(LOG_TAG, name + ": interval " + REMAINING_PHYSICAL_INTERVAL + 
    				" ms is more than limit. Set interval to " + INTERVAL_LIMIT + " ms;");
    		
    		REMAINING_PHYSICAL_INTERVAL = INTERVAL_LIMIT;
    	}
    	
    	PROCESSED_INTERVAL = REMAINING_PHYSICAL_INTERVAL;
	}
	
	
	
	private String solverLog = "";
	
	private int logIterationNo;
	
	@Override 
	protected void onFinish() {
		
		REMAINING_PHYSICAL_INTERVAL -= PROCESSED_INTERVAL;
    	
    	long stepTimerCut = System.currentTimeMillis() - START_ITERATION_TIMER_MARK;
    	
    	solverLog += "step done in: " + stepTimerCut + " ms; " 
    				+ "processed: " + PROCESSED_INTERVAL + " ms; " 
    				+ "remaining: " + REMAINING_PHYSICAL_INTERVAL + " ms;\n";
    	
    	setProcessState(false);
    	
		if (REMAINING_PHYSICAL_INTERVAL <= BASE_PHYSICAL_INTERVAL) {
			
			Log.d(LOG_TAG, solverLog + " solver did " + logIterationNo + " iteration(s); ");
			
	    	solverLog = "";
			logIterationNo = 0;
			masterEngine.subEngineWorkDone(this);
		} 
		else {
			Log.d(LOG_TAG, "Repeat; processed: " + PROCESSED_INTERVAL + " ms; " 
								+ " remaining: " + REMAINING_PHYSICAL_INTERVAL + " ms;");
			logIterationNo++;
	    	enginesScheduler.askToRepeat(this);
		}
	}

}
