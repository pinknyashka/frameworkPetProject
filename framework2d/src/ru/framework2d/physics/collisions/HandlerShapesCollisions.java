package ru.framework2d.physics.collisions;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.core.InteractionHandler;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Movement3;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.PhysicalInterface;
import android.util.Log;

public class HandlerShapesCollisions extends ComponentsHandler {
	
	ArrayList <PhysicalShape> lstShapes = new ArrayList <PhysicalShape>();
	ArrayList <PhysicalShape> lstActive = new ArrayList <PhysicalShape>();
	
	int currentTimeToCollision = 0;
	
	private static final String LOG_TAG = "Physics";
	
	public HandlerShapesCollisions() {
		super();
		
		getContexts();
		loadEntities(entityStorage);
	}

	
	
    @Override 
    protected void iterate() {
    	
    	resolveCollisions();
    	
		stepUp();
		
		setWorkState(!lstActive.isEmpty());
		
		masterEngine.subEngineWorkDone(this);
    }
    
    
    
	private void resolveCollisions() {
		
		int logIterationsNum = 0;
		int logCollisionsNum = 0;
		long activeTimeMark = 0;
		long summaryTimeMark = System.currentTimeMillis();

		PhysicalShape stackedShape = null;

		for (int activeNo = 0; activeNo < lstActive.size(); activeNo++) {
			
			PhysicalShape active = lstActive.get(activeNo);
			
			activeTimeMark = System.currentTimeMillis();
			
			int iteration = 0;
			
			for (int shapeNo = 0; shapeNo < lstShapes.size(); shapeNo++) {
				
				PhysicalShape shape = lstShapes.get(shapeNo);
				
				if (active != shape && shape.isEnabled.value && shape != stackedShape) {
					
					logIterationsNum++;
					
					if (isCollisionHappenedAndSolved(active, shape)) {
						
						logCollisionsNum++;
				    	iteration++;
			    		//counting iterations to refuse stack
			    		if (iteration < 6) {
			    			
			    			shapeNo = -1; //start from first shape
			    			
			    			//change current activeNo if his position in list is changed 
			    			//after add new active
			    			activeNo = lstActive.lastIndexOf(active);
			    		}
			    		else {
			    			Log.e(LOG_TAG, name + ": " + active.entity.name + " is stacked;");
			    			
			    			stackedShape = active;
			    			
			    			activeNo--;
			    			if (activeNo < 0) {
			    				activeNo = 0;
			    			}
			    			else {
				    			lstActive.remove(active);
				    			lstActive.add(0, active);
			    			}
			    			
			    			shapeNo = lstShapes.size();
			    		}
					}
				}
			}
			
    		if (iteration > 1) Log.w(LOG_TAG, name + ": " + active.entity.name + " did " + iteration + " iterations;");
    		
			Log.d(LOG_TAG, name + ": No " + activeNo + " (" + active.entity.name + "): "  
					+ (System.currentTimeMillis() - activeTimeMark) 
					+ " ms; ");	
		}
		
		Log.d(LOG_TAG, name + ": " + (System.currentTimeMillis() - summaryTimeMark) 
				+ " ms; actives " + lstActive.size() 
				+ "; iterations " + logIterationsNum 
				+ "; collisions " + logCollisionsNum);
		
	}
	
	private boolean isCollisionHappenedAndSolved(PhysicalShape active, PhysicalShape shape) {
		
		PhysicalShape firstShape = active;
		PhysicalShape secondShape = shape;
		
		InteractionHandler handlerForShapes = active.getBroadphaseHandler();
		
		if (handlerForShapes != null 
				&& handlerForShapes.isInteractionPossible(firstShape, secondShape)) {
			
			handlerForShapes = firstShape.getInteractionHandler(secondShape.getComponentClass());
			
			if (handlerForShapes == null) {
				
				firstShape = shape;
				secondShape = active;
				
				handlerForShapes = firstShape.getInteractionHandler(
						secondShape.getComponentClass());
			}
			if (handlerForShapes != null 
					&& handlerForShapes.isInteractionHappened(firstShape, secondShape)) {
				
				handlerForShapes.toHandleInteraction(firstShape, secondShape);
				
		    	if (shape.movement.isMoving()) {
		    		addActiveShape(shape);
		    	}
		    	return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unused")
	private boolean isCollisionHappenedAndSolvedExtended(PhysicalShape active, PhysicalShape shape) {
		
		PhysicalShape firstShape = active;
		PhysicalShape secondShape = shape;
		
		InteractionHandler handlerForShapes = firstShape.getInteractionHandler(
				secondShape.getComponentClass());
		
		if (handlerForShapes == null) {
			
			firstShape = shape;
			secondShape = active;
			
			handlerForShapes = firstShape.getInteractionHandler(
					secondShape.getComponentClass());
		}
		
		if (handlerForShapes != null 
				&& handlerForShapes.isInteractionPossible(firstShape, secondShape) 
				&& handlerForShapes.isInteractionHappened(firstShape, secondShape)) {
			
			handlerForShapes.toHandleInteraction(firstShape, secondShape);
			
	    	if (shape.movement.isMoving()) {
	    		addActiveShape(shape);
	    	}
	    	return true;
		}
		return false;
	}
	
	
	
	private void stepUp() {
		
		Log.d(LOG_TAG, name + ": update;");
		
		if (!lstActive.isEmpty()) {
			
			setWorkState(true);
			
			boolean stepProcessed = IntervalPhysics.REMAINING_PHYSICAL_INTERVAL - 
							IntervalPhysics.PROCESSED_INTERVAL <= 
							IntervalPhysics.BASE_PHYSICAL_INTERVAL;

			double ratio = (double) IntervalPhysics.PROCESSED_INTERVAL 
					/ (double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;

			for (int activeNo = 0; activeNo < lstActive.size(); activeNo++) {
				
				PhysicalShape activeShape = (PhysicalShape) lstActive.get(activeNo);
				
				if (activeShape.isActive() && 
						activeShape.step(IntervalPhysics.PROCESSED_INTERVAL, ratio)) {
					
					if (stepProcessed) {
						activeShape.position.commit(this, activeShape.entity);
					}
				} 
				else {
					activeShape.movement.stop();
					lstActive.remove(activeShape);
					activeNo--;
				}
			}
		}
	}
	
	
	
	private boolean addActiveShape(PhysicalShape shape) {
		
		int movingNo = lstActive.lastIndexOf(shape);
		
		if (movingNo < 0) {
			
			lstActive.add(shape);
			
			setWorkState(true);
			if (lstActive.size() == 1) {
				
				Log.d(LOG_TAG, name + ": handler got work now;");
				
				IntervalPhysics.START_ITERATION_TIMER_MARK = System.currentTimeMillis();
				
				enginesScheduler.startWork(0);
			}
			
			return true;
		} 
		else {
			
			setWorkState(true);
			lstActive.add(lstActive.get(movingNo));
			lstActive.remove(movingNo);
			
			return false;
		}
	}

	@Override
	public boolean transferData(Entity object, DataInterface data) {
		
		Log.d(LOG_TAG, name + ": transfer: " + object.name + ": " + data.getName());
		
		boolean transfered = false;
		
		for (Component reflection : object.lstPhysicals) {
			
			if (reflection instanceof PhysicalShape) {
				
				PhysicalShape shape = (PhysicalShape) reflection;
				
				if (data instanceof Vector2) {
					
					if (data.getContext() == shape.impulse.getContext()) {
						
						transfered = true;
						
						Vector2 direct = (Vector2) data;
						
						Log.d(LOG_TAG, name + ": got vector " + direct.getX() + " : " + direct.getY() + 
								"; in_process = " + masterEngine.isInProcess() + 
								"; have work = " + masterEngine.haveWork());
						
						double length = direct.length2(); 
						
						if (length > 0) {
							double impulse = length * IntervalPhysics.IMPULSE_LIMIT;
							
							setImpulseToShape(object, direct, impulse);
						} 
					} 
					else if (data.getContext() == shape.movement.getContext()) {
						
						transfered = true;
						
						shape.movement.setMovement((Movement3) data);
						
						if (!shape.movement.isMoving()) {
							lstActive.remove(shape);
							if (shape.occupiedArea.isAabb) shape.occupiedArea.validateAabb();
						}
					}
				} 
				else if (data instanceof Bool) {
					
					if (data.getContext() == shape.isEnabled.getContext()) {
						
						transfered = true;
						
						Log.d(LOG_TAG, name + ": set " + data.getName() + " to " + ((Bool) data).value);
						
						shape.isEnabled.value = ((Bool) data).value;
					}
				} 
				else if (data instanceof Position3) {
					
					if (data.getContext() == shape.position.getContext()) {
						
						transfered = true;
						
						shape.position.set((Position3) data);
						
						if (shape.occupiedArea.isAabb) shape.occupiedArea.validateAabb();
						
						Log.d(LOG_TAG, name + ": set " + data.getName() + ";");
					}
				}
			}
		} 
		return transfered;
	}

	
	
	void setImpulseToShape(Entity object, Vector2 direct, double p) {
		
		for (PhysicalInterface physicalInterface : object.lstPhysicals) {
			
			if (physicalInterface instanceof PhysicalShape) {
				
				Log.d(LOG_TAG, name + ": set impulse to shape: " + physicalInterface.getName() + 
						"; x=" + direct.getX() + "; y=" + direct.getY() + "; p=" + p 
						+ "; phys_unit=" + IntervalPhysics.PHYSICAL_UNIT);
				
				physicalInterface.setImpulse(direct, p);
				
				physicalInterface.movement.commit(this, physicalInterface.entity);
				
				addActiveShape((PhysicalShape) physicalInterface);
			}
		}
	}

	
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof PhysicalShape) {
			
			if (component.entity != null && !component.entity.isPrototype) {
				lstShapes.add((PhysicalShape) component);
				if (((PhysicalShape) component).isActive()) {
					lstActive.add((PhysicalShape) component);
				}
			}
		}
		return component;
	}

	public Component createComponent(Entity master, TagOptions options) {
		return super.createComponent(master, options);
	}
}
