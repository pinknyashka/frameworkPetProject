package ru.framework2d.graphics.cameras;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Engine;
import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.Entity;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.graphics.GraphicalPeriphery;
import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.screen.TouchFinger;
import android.graphics.Canvas;
import android.util.Log;

public class HandlerCameras extends ComponentsHandler {

	GraphicalPeriphery graphicalPeriphery;

	private static final String LOG_TAG = "Output";
	
	ArrayList <OutputGraphicalCamera> lstCameras = new ArrayList <OutputGraphicalCamera>();
	
	ArrayList <Entity> lstPointers = new ArrayList <Entity>();

	public HandlerCameras() {
		super(2, 
				(Class <Component>) OutputGraphicalCamera.class.asSubclass(Component.class),
				(Class <Component>) WorldPointerCamera.class.asSubclass(Component.class));

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
			
			for (Component component : entity.lstComponents) {
				
				if (component instanceof OutputGraphicalCamera &&
						!lstCameras.contains(component)) {
					
					OutputGraphicalCamera camera = (OutputGraphicalCamera)component;
					
					lstCameras.add(camera);
					
					if (!camera.lstProcessingEngines.contains(this)) {
						camera.lstProcessingEngines.add(this);
					}
				}
			}
		}
		
		createOrFindPointers(objectManager);
		
		return true;
	}
	
	private void createOrFindPointers(EntityStorage entityStorage) {
		
		int createdObjects = 0;
		
		for (Entity object : entityStorage.lstEntities) {
			
			if (object.name.contains("pointer")) {
				
				for (InputTouchInterface touch : object.lstTouches) {
					
					if (touch instanceof TouchFinger) {
						
						if (!touch.lstLinkedHandlers.contains(this)) {
							
							touch.lstLinkedHandlers.add(this);
							
							for (DataInterface data : touch.lstData) data.lstLinkedHandlers.add(this);
						}
					}
				}
				
				if (!lstPointers.contains(object)) lstPointers.add(createdObjects, object);
				
				if (object.lstGraphicals.size() <= lstCameras.size()) {
					
					for (OutputGraphicalCamera camera : lstCameras) {
						
						boolean gotCameraInPointer = false;
						
						for (OutputGraphicalInterface graphical : object.lstGraphicals) {
							
							if (graphical instanceof WorldPointerCamera) {
								
								if (((WorldPointerCamera) graphical).linkedCamera == camera) {
									
									gotCameraInPointer = true;
								}
							}
						}
						
						if (!gotCameraInPointer) {
							
				    		TagOptions options = new TagOptions("WorldPointerCamera");
				    		options.lstPropertys.add(new Property("cameraman", new Text(camera.name.value)));
				    		enginesManager.createComponentInEntity(object, options);
						}
					}
				}
				createdObjects++;
			}
		}
		
		while (createdObjects < 10) { // possible error here
			
			Entity newPointer = new Entity("pointer" + createdObjects);
			entityStorage.lstEntities.add(newPointer);
			lstPointers.add(createdObjects, newPointer);
			
			for (OutputGraphicalCamera camera : lstCameras) {
				
	    		TagOptions options = new TagOptions("WorldPointerCamera");
	    		options.lstPropertys.add(new Property("cameraman", new Text(camera.name.value)));
	    		
	    		enginesManager.createComponentInEntity(newPointer, options);
	    	}
			
	    	createdObjects++;
		}
	}
	
	

	@Override
	public boolean toHandleComponent(Component object) {
		drawObject(graphicalPeriphery.getCanvas(), (OutputGraphicalInterface) object);
		return false;
	}
	

	
	int saveCount = 0;
	
	private void drawObject(Canvas canvas, OutputGraphicalInterface g) {
		
		OutputGraphicalCamera camera = (OutputGraphicalCamera) g;
		
		canvas.scale(camera.zoom.value, camera.zoom.value);
		canvas.translate((float) -camera.eyePos.x.value, (float) -camera.eyePos.y.value);
	}
	
	
	
	public boolean transferData(Entity object, DataInterface data) {
		boolean transfered = false;
		if (data instanceof Point2) {
			for (Component reflection : object.lstGraphicals) {
				if (reflection instanceof WorldPointerCamera) {
					transfered = true;
					WorldPointerCamera pointer = (WorldPointerCamera) reflection;
					OutputGraphicalCamera camera = pointer.linkedCamera;
					if (camera.isEnabled.value && camera.isTouchable.value) {
						Point2 where = camera.getLocaleCoordinate((Point2) data);
						Log.d(LOG_TAG, name + ": " + pointer.entity.name 
											+ " in camera " + camera.name + " is moved;");
						pointer.position.set(where.x.value, where.y.value);
						if (pointer.isTouches.value) {
							pointer.position.localCommit(this, pointer, pointer.position);
						}
					}
				} 
				else if (reflection instanceof OutputGraphicalCamera) {
					OutputGraphicalCamera camera = (OutputGraphicalCamera) reflection;
					if (data.getContext() == camera.position.getContext()) {
						transfered = true;
						if (data instanceof Position3) {
							camera.position.set((Position3) data);
						} 
						else if (data instanceof Position2) {
							camera.position.set((Position2) data);
						}
						Log.d(LOG_TAG, name + ": set camera " + camera.name + " pos to: x: " 
											+ camera.position.getX() + " y: " + camera.position.getY() 
											+ "; a: " + camera.position.getAlpha());
					}
				}
			}
		} 
		else if (data instanceof Bool) {
			for (Component reflection : object.lstGraphicals) {
				if (reflection instanceof WorldPointerCamera) {
					WorldPointerCamera pointer = (WorldPointerCamera) reflection;
					if (data.getContext() == pointer.isTouches.getContext()) {
						transfered = true;
						OutputGraphicalCamera camera = pointer.linkedCamera; 
						if (camera.isEnabled.value && camera.isTouchable.value) {
							Log.d(LOG_TAG, name + ": " + reflection.entity.name + " active state changed to: " + ((Bool) data).value);
							pointer.isTouches.value = ((Bool) data).value;
							pointer.isTouches.localCommit(this, pointer, data);
						}
					} 
					else if (data.getContext() == pointer.isCurrentlyMoving.getContext()) {
						transfered = true;
						pointer.isCurrentlyMoving.value = ((Bool) data).value;
					}
				}
			}
		} 
		else if (data instanceof Fractional) {
			for (Component reflection : object.lstGraphicals) {
				if (reflection instanceof OutputGraphicalCamera) {
					transfered = true;
					OutputGraphicalCamera camera = (OutputGraphicalCamera) reflection;
					Fractional zoom = (Fractional) data;
					if (camera.zoom.getContext() == zoom.getContext())
					Log.d(LOG_TAG, name + ": set zoom to: " + zoom.value);
					camera.zoom.value = zoom.value;
					if (camera.zoom.value > camera.zoomMax.value) {
						camera.zoom.value = camera.zoomMax.value;
					} 
					else if (camera.zoom.value < camera.zoomMin.value) {
						camera.zoom.value = camera.zoomMin.value;
					}
				}
			}
			
		}
		return transfered;
	}
	
	
	
	public Component connectComponent(Component component) {
		
		if (component instanceof OutputGraphicalCamera) {
			
			OutputGraphicalCamera camera = (OutputGraphicalCamera) component;
			
			if (!lstCameras.contains(camera)) {
				lstCameras.add(camera);
				
				if (!component.lstLinkedHandlers.contains(this)) {
					component.lstLinkedHandlers.add(this);
					for (DataInterface data : component.lstData) data.lstLinkedHandlers.add(this);
				}
				
				if (!camera.lstProcessingEngines.contains(this)) {
					camera.lstProcessingEngines.add(this);
				}
				
			}
		}
		return component;
	}
	
	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	public Component createComponent(Entity master, TagOptions options) {
		
		Component newComponent = super.createComponent(master, options);
		
		if (options.name.contentEquals("WorldPointerCamera")) {
			
			WorldPointerCamera newPointer = (WorldPointerCamera) newComponent;
			for (Property property : options.lstPropertys) {
				if (property.name.contentEquals("cameraman")) {
					for (OutputGraphicalCamera camera : lstCameras) {
						if (camera.name.value.contentEquals(property.data.toString())) {
							newPointer.linkedCamera = camera;
						}
					}
				}
			}
		} 
		else if (options.name.contentEquals(OutputGraphicalCamera.SHORT_CLASS_NAME)
				|| options.name.contentEquals(OutputGraphicalCamera.class.getSimpleName())) {
			
			OutputGraphicalCamera newCamera = (OutputGraphicalCamera) newComponent;
			lstCameras.add(newCamera);
			
			for (Entity pointer : lstPointers) {
				
	    		TagOptions pointerOptions = new TagOptions("WorldPointerCamera");
	    		pointerOptions.lstPropertys.add(new Property("cameraman", new Text(newCamera.name.value)));
	    		
				enginesManager.createComponentInEntity(pointer, pointerOptions);
						
			}
			
			if (!newCamera.lstProcessingEngines.contains(this)) {
				newCamera.lstProcessingEngines.add(this);
			}
			
			Log.d(LOG_TAG, "Camera <" + newCamera.name + "> created;");
		}
		
		return newComponent;
	}
}

