package ru.framework2d.graphics.animation;

import java.util.ArrayList;
import java.util.TimerTask;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;

import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;

import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.graphics.OutputGraphicalInterface;

import android.util.Log;

public class HandlerAnimation extends ComponentsHandler {

	GraphicalPeriphery graphicalPeriphery;
	
	ArrayList <OutputGraphicalAnimation> lstStartedAnimations = new ArrayList <OutputGraphicalAnimation>();

    private static final String LOG_TAG = "Output";
    
    
    
    public static final int INFINITY = -1;
    
    
    
    public HandlerAnimation() {
		super(1, (Class <Component>) OutputGraphicalAnimation.class.asSubclass(Component.class));
		
		getContexts();
		
		for (Engine engine : enginesManager.lstEngines) {
			if (engine instanceof GraphicalPeriphery) {
				graphicalPeriphery = (GraphicalPeriphery) engine; 
				break;
			}
		}
		
		loadEntities(entityStorage);
	}
    
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		
    	for (Entity entity : objectManager.lstEntities) {
    		
	    	for (OutputGraphicalInterface graphical : entity.lstGraphicals) {
	    		
	    		if (graphical instanceof OutputGraphicalAnimation) {
	    			
					connectComponent(graphical);
	    		} 
	    	}
		}
		return true;
	}
	
	public boolean transferData(Entity object, DataInterface data) {
		//Log.d(LOG_TAG, name + ": transfer data " + data.getName() + " in " + object.name);
		
		boolean transfered = false;
		
		for (Component component : object.lstGraphicals) {
			if (component instanceof OutputGraphicalAnimation) {
				OutputGraphicalAnimation animation = (OutputGraphicalAnimation) component;
				if (data instanceof Position3) {
					//transfered = true;
					//animation.position.set((Position3) data);
					//outputDirect.setWorkState(true);
				} 
				else if (data instanceof Bool) {
					transfered = true;
					
					if (data.getContext() == animation.activation.getContext()) {
						lstStartedAnimations.add(animation);
						setWorkState(true);
						if (((Bool)data).value) {
							if (animation.from.value < animation.to.value) {
								animation.currentValue.value = animation.from.value;
							} 
							else {
								animation.currentValue.value = animation.to.value;
							}
							animation.step.value = Math.abs(animation.step.value);
						} 
						else {
							if (animation.from.value > animation.to.value) {
								animation.currentValue.value = animation.from.value;
							} 
							else {
								animation.currentValue.value = animation.to.value;
							}
							animation.step.value = -Math.abs(animation.step.value);
						}
						
						if (lstStartedAnimations.size() == 1) {
							timerMark = System.currentTimeMillis();
							//animation.currentValue.value = animation.startValue.value;
							Log.d(LOG_TAG, name + ": setWorkState " + animation.entity.name + ".activationTrigger <" + data.getContext().name + ">");
						}
						Log.d(LOG_TAG, name + ": set " + animation.entity.name + ".activationTrigger <" + data.getContext().name + ">");
					} 
					else if (data.getContext() == animation.isLocalVisible.getContext()) {
						//animation.isLocalVisible.value = ((Bool) data).value;
						/*Log.d(LOG_TAG, name + ": set " + picture.spriteName + ".isLocalVisible <" + data.getContext().name 
											+ "> to " + picture.isVisible.value);*/
						//outputDirect.setWorkState(true);
					}
				}	
			} 
		} 
		//Log.d(LOG_TAG, name + ": transfer done in " + (System.currentTimeMillis() - timerMark1) + " ms;");
		return transfered;
	}
	 
	class AnimationTask extends TimerTask {
	    public void run() { //что делаем по таймеру 
	    	iterate();
    	}
    }
	
	
	
	private ArrayList <OutputGraphicalAnimation> lstFinishedAnimations = new ArrayList <OutputGraphicalAnimation>();
	
	protected void iterate() {
    	long timerMark2 = System.currentTimeMillis();
    	int pastTime = (int) (timerMark2 - timerMark);
    	timerMark = timerMark2;
    	
		for (OutputGraphicalAnimation animation : lstStartedAnimations) {
			float intervalRatio = (float) pastTime / (animation.interval.value * 1000);
			animation.currentValue.value += animation.step.value * intervalRatio; 
			
			if (animation.step.value > 0) {
				if (animation.from.value < animation.to.value) {
					if (animation.currentValue.value > animation.to.value) {
						if (animation.loops.value == INFINITY) {
							animation.currentValue.value += animation.to.value - animation.from.value;
						} 
						else {
							animation.currentValue.value = animation.to.value;
							lstFinishedAnimations.add(animation);
						}
					}
				} 
				else {
					if (animation.currentValue.value > animation.from.value) {
						if (animation.loops.value == INFINITY) {
							animation.currentValue.value += animation.to.value - animation.from.value;
						} 
						else {
							animation.currentValue.value = animation.from.value;
							lstFinishedAnimations.add(animation);
						}
					}
				}
			} 
			else {
				if (animation.from.value > animation.to.value) {
					if (animation.currentValue.value < animation.to.value) {
						if (animation.loops.value == INFINITY) {
							animation.currentValue.value += animation.to.value - animation.from.value;
						} 
						else {
							animation.currentValue.value = animation.to.value;
							lstFinishedAnimations.add(animation);
						}
					}
				} 
				else {
					if (animation.currentValue.value < animation.from.value) {
						if (animation.loops.value == INFINITY) {
							animation.currentValue.value += animation.to.value - animation.from.value;
						} 
						else {
							animation.currentValue.value = animation.from.value;
							lstFinishedAnimations.add(animation);
						}
					}
				}
			}
			
			/*if (animation.currentValue.value <= animation.finalValue.value && animation.step.value < 0) {
				if (animation.isNeedToRepeat.value) {
					animation.currentValue.value += animation.finalValue.value - animation.startValue.value;
				} else {
					animation.currentValue.value = animation.finalValue.value;
					lstFinishedAnimations.add(animation);
				}
			} else if (animation.currentValue.value >= animation.finalValue.value && animation.step.value > 0) {
				if (animation.isNeedToRepeat.value) {
					animation.currentValue.value -= animation.finalValue.value - animation.startValue.value;
				} else {
					animation.currentValue.value = animation.finalValue.value;
					lstFinishedAnimations.add(animation);
				
			}*/
			animation.currentValue.commit(this, animation.entity);
		} 
		lstStartedAnimations.removeAll(lstFinishedAnimations);
		lstFinishedAnimations.clear();
    	
		setWorkState(!lstStartedAnimations.isEmpty());
		
    	//Log.d(LOG_TAG, name + ": step done in "+ (System.currentTimeMillis() - timerMark2) + " ms;");
    	masterEngine.subEngineWorkDone(this);
	}
	
	protected void onFinish() {
		
	}
	
	
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);

		if (component instanceof OutputGraphicalAnimation) {
			graphicalPeriphery.update();
		}
		return component;
	}

}