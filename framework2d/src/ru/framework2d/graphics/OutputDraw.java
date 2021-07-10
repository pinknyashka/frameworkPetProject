package ru.framework2d.graphics;

import java.util.ArrayList;

import ru.framework2d.core.Engine;
import ru.framework2d.core.GameSurface;
import ru.framework2d.touches.WorldPointer;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class OutputDraw extends Engine implements GraphicalPeriphery {

    private static final String LOG_TAG = "Output";

	public OutputDraw() {
		super();
		
		if (resources.gameSurface != null) {
			((GameSurface) resources.gameSurface).setGraphicalPeriphery(this);	
		}
		
		loadEntities(entityStorage);
		
//		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//			DirectOutputInterface.IS_MAIN_THREAD_DRAW = true;
//		}
	}


	
	protected Canvas canvas;
	
	public Canvas getCanvas() {
		return canvas;
	}

	private long timerMark = System.currentTimeMillis();
	
	private OutputGraphicalInterface graphical;
	
	private int graphicalsNum = 0;

	public void draw(Canvas c) {
		
		canvas = c;
		timerMark = System.currentTimeMillis();
		isDrawingProgress = true;
		
		Log.d(LOG_TAG, "Start draw " + (timerMark - enginesScheduler.mark) + " ms later than call;");
		
		graphicalsNum = lstGraphicals.size();
		
		for (int graphicalNo = 0; graphicalNo < graphicalsNum; graphicalNo++) {
			
			graphical = lstGraphicals.get(graphicalNo);
			
			graphical.draw(c, paint);
		}
		
		canvas = null;
		isDrawingProgress = false;
		
		Log.d(LOG_TAG, "Over draw; Spent " + (System.currentTimeMillis() - timerMark) 
						+ " ms for draw " + lstGraphicals.size() + " graphicals;");
		
		enginesScheduler.overDraw();
	}

	
	
	protected Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	public void setPaint(Paint paint) {
		this.paint = paint;
	}
	
	public Paint getPaint() {
		return paint;
	}

	
	
	protected boolean isDrawingProgress = false;

	public void setDrawState(boolean drawInProgress) {
		isDrawingProgress = drawInProgress;
	}

	public boolean isDrawInProgress() {
		return isDrawingProgress;
	}

	
	
	public ArrayList <OutputGraphicalInterface> lstGraphicals = new ArrayList <OutputGraphicalInterface>();
	
	public ArrayList<OutputGraphicalInterface> getListGraphicals() {
		return lstGraphicals;
	}

	public void addGraphical(OutputGraphicalInterface graphical) {
		
		if (!graphical.entity.isPrototype) {
			
			if (graphical.entity.parent != null) {
					
				int lastGraphicalNo = graphical.entity.parent.lstGraphicals.size() - 1;
				OutputGraphicalInterface lastGraphical = 
						graphical.entity.parent.lstGraphicals.get(lastGraphicalNo);
				
				if (!lastGraphical.lstChildren.contains(graphical)) {
					lastGraphical.lstChildren.add(graphical);
				}
				
				graphical.parentGraphical = lastGraphical;
				
			}
			else if (!lstGraphicals.contains(graphical) && !(graphical instanceof WorldPointer)) {
				lstGraphicals.add(graphical);
			}
		}
	}



	public void update() {
		setWorkState(true);
	}
	
	@Override
	protected void onFinish() {
		masterEngine.subEngineWorkDone(this);
	}

}
