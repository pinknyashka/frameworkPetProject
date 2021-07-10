package ru.framework2d.touches.motion;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.data.DataInterface;
import ru.framework2d.touches.WorldPointer;
import android.util.Log;

public class HandlerMotion extends ComponentsHandler {
	
	private static final String LOG_TAG = "Input";

	public ArrayList <TouchMotion> lstMotions = new ArrayList <TouchMotion>();
	public ArrayList <TouchMotion> lstActiveMotions = new ArrayList <TouchMotion>();
	
	public HandlerMotion() {
		super(2, (Class <Component>) TouchMotion.class.asSubclass(Component.class),
				(Class <Component>) WorldPointer.class.asSubclass(Component.class));
		
		getContexts();
		loadEntities(entityStorage);
	}

	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof TouchMotion) {
			if (!lstMotions.contains(component)) {
				TouchMotion motion = (TouchMotion) component;
				lstMotions.add(motion);
				if (motion.enabled.value) lstActiveMotions.add(motion);
			}
		}	
		return component;
	}
	
	@Override
	public boolean startWork(int delay) {
		return false;
	}

	@Override
	public boolean transferData(Entity object, DataInterface data) {
		
		boolean transfered = false;
		
		/*for (Reflection reflection : object.lstTouches) {
			if (reflection instanceof InputMotion) {
				InputMotion motion = (InputMotion) reflection;
				
				if (data instanceof Position3) {
					transfered = true;
					Position3f where = ((Position3) data).get3f();
					motion.moveTo(where); //
					//motion.localCommit(this, motion.position);
					motion.position.localCommit(this, motion, data);
				} else if (data instanceof Bool) {
					transfered = true;
					Bool isEnabled = (Bool) data;
					motion.enabled.value = isEnabled.value;
					//motion.localCommit(this, motion.enabled);
					motion.enabled.localCommit(this, motion, data);
				}
				
			} else if (reflection instanceof WorldPointer){
				//ignore
			}
		} */
		return transfered;
	}

	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		Log.d(LOG_TAG, name + ": ltrnsfr: " + reflection.getName() + "; " + data.getContext().name);
		boolean handled = false;
		/*if (reflection instanceof InputMotion) {
			InputMotion motion = (InputMotion) reflection;
			
			if (data instanceof Position3f) {
				handled = true;
				Position3f where = (Position3f) data;
				motion.moveTo(where);//
			} else if (data instanceof Bool) {
				handled = true;
				Bool isEnabled = (Bool) data;
				motion.enabled.value = isEnabled.value;
			}
			
		} else if (reflection instanceof WorldPointer) {
			WorldPointer pointer = (WorldPointer) reflection;
			if (data instanceof Bool) {
				handled = true;
				if (pointer.isActive.value) {
					Log.d(LOG_TAG, name+": "+" and then here; " + lstActiveMotions.size());
					for (InputMotion activeMotion : lstActiveMotions) {
						Log.d(LOG_TAG, name+": "+activeMotion.master.name+ " checking;");
						if (!activeMotion.touched(pointer.positionf)) {
							Log.d(LOG_TAG, name+": "+activeMotion.master.name+ " touched;");
							activeMotion.globalTouchPointTap.set(pointer.positionf.getX(), pointer.positionf.getY());
							activeMotion.directTap.set(pointer.positionf.getX()-activeMotion.position.getX(), 
														pointer.positionf.getY()-activeMotion.position.getY());
							activeMotion.touchMoment = System.currentTimeMillis();
						}
					} 
				} else {
					Log.d(LOG_TAG, name+": "+" and then here; " + lstActiveMotions.size());
					for (InputMotion activeMotion : lstActiveMotions) {
						Log.d(LOG_TAG, name+": "+activeMotion.master.name+ " checking;");
						if (!activeMotion.touched(pointer.positionf)) {
							int time = (int) (System.currentTimeMillis() - activeMotion.touchMoment);
							if (time < activeMotion.latency) {
								Log.d(LOG_TAG, name+": "+activeMotion.master.name+ " up; tap");
								activeMotion.globalTouchPointTap.set(pointer.positionf.getX(), pointer.positionf.getY());
								activeMotion.isLongTap.value = false;
								//activeMotion.isLongTap.getContext().commit(this, activeMotion.master, activeMotion.isLongTap);
								activeMotion.isLongTap.commit(this, activeMotion.master);
								activeMotion.directTap.set(	pointer.positionf.getX() - activeMotion.position.getX(), 
															pointer.positionf.getY() - activeMotion.position.getY());
								//activeMotion.directTap.getContext().commit(this, activeMotion.master, activeMotion.directTap);
								activeMotion.directTap.commit(this, activeMotion.master);
							} else {
								Log.d(LOG_TAG, name+": "+activeMotion.master.name+ " up; long tap");
								activeMotion.globalTouchPointTap.set(pointer.positionf.getX(), pointer.positionf.getY());
								activeMotion.isLongTap.value = true;
								//activeMotion.isLongTap.getContext().commit(this, activeMotion.master, activeMotion.isLongTap);
								activeMotion.isLongTap.commit(this, activeMotion.master);
								activeMotion.directTap.set(	pointer.positionf.getX() - activeMotion.position.getX(), 
															pointer.positionf.getY() - activeMotion.position.getY());
								//activeMotion.directTap.getContext().commit(this, activeMotion.master, activeMotion.directTap);
								activeMotion.directTap.commit(this, activeMotion.master);
							}
						}
					} 
				}
			} else if (data instanceof Point2f) {
				//Log.d(TAG, name+".pointerIsActive: "+pointer.pntrMaster.name+ " is moving;");
			}
		}*/
		return handled;
	}

}
