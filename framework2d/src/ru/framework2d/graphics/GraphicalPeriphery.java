package ru.framework2d.graphics;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;

public interface GraphicalPeriphery {
	
	public Paint getPaint();
	
	public void setPaint(Paint paint);

	
	
    public void setDrawState(boolean drawInProgress);
    
    public boolean isDrawInProgress();

	public abstract void draw(Canvas c);
	
	public Canvas getCanvas();
	
	
	
	public ArrayList <OutputGraphicalInterface> getListGraphicals();
	
	public void addGraphical(OutputGraphicalInterface graphical);
	
	public void update();
	
}
