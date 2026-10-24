package ru.framework2d.graphics;

import java.util.ArrayList;

import ru.framework2d.compat.Canvas;
import ru.framework2d.compat.Paint;

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
