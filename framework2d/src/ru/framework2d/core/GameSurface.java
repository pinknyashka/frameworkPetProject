package ru.framework2d.core;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.content.Context;

import android.graphics.Canvas;

import ru.framework2d.graphics.GraphicalPeriphery;

import ru.framework2d.touches.TouchPeriphery;


public class GameSurface extends SurfaceView {// implements SurfaceHolder.Callback {
	
 
	
	private TouchPeriphery touchPeriphery;

	public TouchPeriphery getTouchPeriphery() {
		return touchPeriphery;
	}

	public void setTouchPeriphery(TouchPeriphery touchPeriphery) {
		this.touchPeriphery = touchPeriphery;
	}
	
	

	private GraphicalPeriphery graphicalPeriphery;

	public GraphicalPeriphery getGraphicalPeriphery() {
		return graphicalPeriphery;
	}

	public void setGraphicalPeriphery(GraphicalPeriphery graphicalPeriphery) {
		this.graphicalPeriphery = graphicalPeriphery;
	}
	
	
	
	public GameSurface(Context context, EnginesManager enginesManager) {
        super(context);
        
        for (Engine engine : enginesManager.lstEngines) {
			
			if (engine instanceof GraphicalPeriphery) {
				
		        setGraphicalPeriphery((GraphicalPeriphery) engine);
			} 
			else if (engine instanceof TouchPeriphery) {
				
		        setTouchPeriphery((TouchPeriphery) engine);
			}
		}
        
        
        this.setKeepScreenOn(true);
        //this.getHolder().addCallback(this);
        
        /*if (android.os.Build.VERSION.SDK_INT>android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
	    	Log.d(LOG_TAG,"version is highter then ice_cream_sandwich..");
	        try {
		        if (isHardwareAccelerated())
		        	Log.d(LOG_TAG,"graphic is hardware accelerated");
		        else 
		        	Log.d(LOG_TAG,"graphic is not hardware accelerated");
	        } catch (NoSuchMethodError e) {
		    	e.printStackTrace();
	        }
		}else Log.d(LOG_TAG,"version is less then ice_cream_sandwich");*/
    }
	
	
	
	
	@Override
    protected void onDraw(Canvas canvas) {
		
		if (graphicalPeriphery != null) {
			graphicalPeriphery.draw(canvas); 
		}
    }
	
    @Override 
    public boolean onTouchEvent(MotionEvent event) {
    	
    	if (touchPeriphery != null) {
    		touchPeriphery.toHandleEvent(event);
    	}
        return true;
    }
    
    
    
	public void surfaceCreated(SurfaceHolder holder) {
		invalidate();
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		//nothing
	}
	public void surfaceDestroyed(SurfaceHolder holder) {
		//nothing
	}
}
