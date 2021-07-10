package ru.framework2d.touches.areas.direct;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector2;
import ru.framework2d.graphics.cameras.OutputGraphicalCamera;
import ru.framework2d.graphics.cameras.WorldPointerCamera;
import ru.framework2d.touches.WorldPointer;
import android.util.Log;

public class SubDirectedTouch extends ComponentsHandler {
	
	private static final String LOG_TAG = "Input";
	
	private int displayWidth = 480;
	private int displayHeight = 800;
	
	public SubDirectedTouch() {
		super(1, (Class <Component>) TouchableAreaDirect.class.asSubclass(Component.class));
		
		displayHeight = resources.appResources.getDisplayMetrics().heightPixels;
		displayWidth = resources.appResources.getDisplayMetrics().widthPixels;
		
	}
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		boolean handled = false;
		TouchableAreaDirect directArea = (TouchableAreaDirect) reflection;
		
		if (data instanceof Bool) {
			if (!((Bool) data).value) {
				if (directArea.lstPointers.size() == 1) {
					
					WorldPointer pointer = directArea.lstPointers.get(0);
					if (!directArea.touched(pointer.position) && directArea.isClikable.value) {
						
						directArea.shotDirect.set(pointer.position, directArea.position);
						if (directArea.isInScreenProportion.value) {
							
							areaScreenPos.set(directArea.position);
							pointerScreenPos.set(pointer.position);
							if (pointer instanceof WorldPointerCamera) {
								OutputGraphicalCamera camera = ((WorldPointerCamera) pointer).linkedCamera;
								areaScreenPos.set(camera.getScreenCoordinate(areaScreenPos));
								pointerScreenPos.set(camera.getScreenCoordinate(pointerScreenPos));
							}
							screenVector.set(pointerScreenPos, areaScreenPos);
							
							double k = getRatio(directArea);
							if (k > 1) k = 1;
							directArea.shotDirect.restore();
							directArea.shotDirect.selfMulti(k);
						}
						
						Log.d(LOG_TAG, masterEngine.name + ": " + pointer.entity.name 
								+ " directed shot: " 
								+ directArea.shotDirect.getX() + " : " 
								+ directArea.shotDirect.getY() + ";");
						
						directArea.shotDirect.commit(masterEngine, directArea.entity);
						directArea.lenght.value = (float) directArea.rotateDirect.length2();
						if (directArea.lenght.value > 1.0f) directArea.lenght.value = 1.0f; 
						directArea.lenght.commit(masterEngine, directArea.entity);
						
						handled = true;
					}
					
				}
			} 
			else {
				directArea.lenght.value = 0;
				directArea.lenght.commit(masterEngine, directArea.entity);
			}
		} 
		else if (data instanceof Point2) {
			if (directArea.lstPointers.size() == 1) {
				
				WorldPointer pointer = directArea.lstPointers.get(0);
				
				if (!directArea.touched(pointer.position)) {
					
					directArea.rotateDirect.set(pointer.position, directArea.position);
					Log.d(LOG_TAG, masterEngine.name + ": " 
											+ pointer.entity.name + "1 rotate direct: " 
											+ directArea.rotateDirect.getX() + " : " 
											+ directArea.rotateDirect.getY() + ";");
					if (directArea.isInScreenProportion.value) {
						
						areaScreenPos.set(directArea.position);
						pointerScreenPos.set(pointer.position);
						if (pointer instanceof WorldPointerCamera) {
							OutputGraphicalCamera camera = ((WorldPointerCamera) pointer).linkedCamera;
							areaScreenPos.set(camera.getScreenCoordinate(areaScreenPos));
							pointerScreenPos.set(camera.getScreenCoordinate(pointerScreenPos));
						}
						screenVector.set(pointerScreenPos, areaScreenPos);
						
						double k = getRatio(directArea);
						directArea.rotateDirect.restore();
						directArea.rotateDirect.selfMulti(k);
					}
					directArea.rotateDirect.commit(masterEngine, directArea.entity);
					directArea.lenght.value = (float) directArea.rotateDirect.length2();
					if (directArea.lenght.value > 1.0f) directArea.lenght.value = 1.0f; 
					directArea.lenght.commit(masterEngine, directArea.entity);
					handled = true;
				} 
				else {
					directArea.lenght.value = 0;
					directArea.lenght.commit(masterEngine, directArea.entity);
				}
				
			}
		}
		return handled;
	}

	
	
	private Point2 areaScreenPos = new Point2();
	private Point2 pointerScreenPos = new Point2();
	private Vector2 screenVector = new Vector2();
	
	private double getRatio(TouchableAreaDirect directArea) {
		double kX = screenVector.getX();
		if (kX > 0) {
			double lengthToDisplayBorder = displayWidth - areaScreenPos.getX();
			if (lengthToDisplayBorder < directArea.proportion.value) {
				if (lengthToDisplayBorder != 0) {
					kX =  kX / lengthToDisplayBorder;
				}
			} 
			else {
				if (directArea.proportion.value != 0) {
					kX =  kX / directArea.proportion.value;
				}
			}
		} 
		else {
			if (areaScreenPos.getX() < directArea.proportion.value) {
				if (areaScreenPos.getX() != 0) {
					kX = - kX / areaScreenPos.getX();
				}
			}
			else {
				if (directArea.proportion.value != 0) {
					kX = - kX / directArea.proportion.value;
				}
			}
		}
		double kY = screenVector.getY();
		if (kY > 0) {
			double lengthToDisplayBorder = displayHeight - areaScreenPos.getY();
			if (lengthToDisplayBorder < directArea.proportion.value) {
				if (lengthToDisplayBorder != 0) {
					kY =  kY / lengthToDisplayBorder;
				}
			}
			else {
				if (directArea.proportion.value != 0) {
					kY =  kY / directArea.proportion.value;
				}
			}
		} 
		else {
			if (areaScreenPos.getY() < directArea.proportion.value) {
				if (areaScreenPos.getY() != 0) {
					kY = - kY / areaScreenPos.getY();
				}
			} 
			else {
				if (directArea.proportion.value != 0) {
					kY = - kY / directArea.proportion.value;
				}
			}
		}
		if (kX > kY) {
			return kX;
		} 
		else {
			return kY;
		}
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
