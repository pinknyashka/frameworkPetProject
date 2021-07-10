package ru.framework2d.graphics.draw.sprite;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.DataInterface;
import ru.framework2d.graphics.GraphicalPeriphery;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class SubSpriteDraw extends ComponentsHandler {
	
	private Paint paint;
	
	private GraphicalPeriphery graphicalPeriphery;
	
	
	
	public SubSpriteDraw() {
		super(1, (Class <Component>) OutputGraphicalSprite.class.asSubclass(Component.class));
		
		for (Engine engine : enginesManager.lstEngines) {
			
			if (engine instanceof GraphicalPeriphery) {
				
				graphicalPeriphery = (GraphicalPeriphery) engine; 
				paint = graphicalPeriphery.getPaint();
				
				break;
			}
		}
	}
	
	
	
	@Override
	public Component connectComponent(Component component) {
		
		if (component instanceof OutputGraphicalSprite) {
			
			OutputGraphicalSprite newSprite = (OutputGraphicalSprite) component;
			//masterEngine.getResources()
			newSprite.sprite = Engine.resources.getSprite(
					newSprite.spriteName.value, 
					newSprite.spriteCollumnsNum.value, 
					newSprite.spriteRowsNum.value);
			
			newSprite.lstLinkedSubHandlers.add(this);
			
		} 
		return component;
	}
	
	
	
	private Canvas canvas;
	
	private OutputGraphicalSprite picture;
	
	private float sx, sy;

	private long currentTick = -1;
	
	private Rect drawingArea = new Rect();
	
	@Override
	public boolean toHandleComponent(Component component) {
		
		boolean handled = false;
		
		if (currentTick != enginesScheduler.currentTick) {
			canvas = graphicalPeriphery.getCanvas();
			currentTick = enginesScheduler.currentTick;
		}
		
		if (canvas != null) {
			
			picture = (OutputGraphicalSprite) component;
			
			sx = (float) picture.size.x.value / 2;		
			sy = (float) picture.size.y.value / 2;
			
			drawingArea.set((int)-sx, (int)-sy, (int) sx, (int) sy);
			
			canvas.drawBitmap(
					picture.sprite.spriteImage, 
					picture.sprite.lstSpritesRects.get(picture.currentSpriteNo.value), 
					drawingArea, 
					paint
					);
			
			handled = true;
		}
		return handled;
	}
	
	
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean transfered = false;
		
		OutputGraphicalSprite picture = (OutputGraphicalSprite) reflection;
		
		if (data.getContext() == picture.currentSpriteNo.getContext()) {
			
			transfered = true;
			
			picture.currentSpriteNo.setValue(data);
			
//			Log.d("Output", masterEngine.name + ": set " + picture.master.name + ".spriteNo <" 
//							+ data.getContext().name + "> to " + picture.transparency.value);
			graphicalPeriphery.update();
		}
		return transfered;
	}
	
	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		return false;
	}

}
