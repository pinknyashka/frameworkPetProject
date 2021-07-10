package ru.framework2d.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;

import ru.framework2d.data.DataInterface;

import android.util.Log;

public abstract class ComponentsHandler extends Engine implements Factory {

	private static final String LOG_TAG = "core";

	
	public ComponentsHandler(int processingNum, Class <Component> ... classes) {
		super();
		
		this.fullName = name;
		
		this.processingNum = processingNum;
		
		for (int i = 0; i < processingNum; i++) {
			addProcessingComponent(classes[i]);
		}
		
		for (Class <Component> handledClass : classes) {
			lstUsingComponents.add(handledClass);
		}
	}

	public ComponentsHandler() {
		super();
	}

	
	protected void addProcessingComponent(Class <Component> processingComponentClass) {
		
		if (!lstProcessingComponents.contains(processingComponentClass)) {
			
			lstProcessingComponents.add(processingComponentClass);
			
			lstBasicProcessingComponents.add(processingComponentClass);
			
			String classShortName = "";
			
			try {
				classShortName = (String) (processingComponentClass).getDeclaredField("SHORT_CLASS_NAME").get(null);
			} catch (IllegalArgumentException e) {
				Log.w(LOG_TAG, "Wrong argument in " + processingComponentClass.getName());
			} catch (IllegalAccessException e) {
				Log.w(LOG_TAG, "Cant access field in " + processingComponentClass.getName());
			} catch (NoSuchFieldException e) {
				Log.w(LOG_TAG, "There is no such field in " + processingComponentClass.getName());
		
			} finally {
				
				if (!classShortName.contentEquals("")) {
					boolean gotThisShortName = false;
					for (String shortName : lstComponentsShortNames) {
						if (shortName.contentEquals(classShortName)) {
							gotThisShortName = true;
						}
					}
					if (!gotThisShortName) lstComponentsShortNames.add(classShortName);
				}
				
			}			
		}
	}
	
	
	
	public ArrayList <ComponentsHandler> lstSubHandlers = new ArrayList <ComponentsHandler>();

	@Override
	public void addSubengine(Engine subengine) {
		super.addSubengine(subengine);
		
		if (ComponentsHandler.class.isAssignableFrom(subengine.getClass())) {
			
			ComponentsHandler subHandler = (ComponentsHandler) subengine;
			
			if (!lstSubHandlers.contains(subHandler)) {
					
				lstSubHandlers.add(subHandler);
				subHandler.setMasterHandler(this);
			}
		}
	}
	
	public ComponentsHandler masterHandler;
	
	public void setMasterHandler(ComponentsHandler handler) {
	
		this.masterHandler = handler;
		
		this.fullName = "" + handler.name + "." + name;
				
		for (Class <Component> processingComponentClass : lstProcessingComponents) {
			masterHandler.addProcessingComponent(processingComponentClass);
		}
	}
	

	
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		
		for (Entity entity : objectManager.lstEntities) {
			
			for (Component component : entity.lstComponents) {
				
				for (Class <Component> processingComponentClass : lstProcessingComponents) {
					
					if (processingComponentClass.isAssignableFrom(component.getClass())) {
						connectComponent(component); 
					}
				}
			}
		}
		return false;
	}

	
	
	public boolean toHandleComponent(Component component) {
		return false;
	}
	
	
	public int processingNum = 0;

	public ArrayList <ComponentsHandler> lstHighterLinkedHandlers = new ArrayList <ComponentsHandler>();
	
	public ArrayList <Class <Component>> lstProcessingComponents = new ArrayList <Class <Component>>();

	public ArrayList <Class <Component>> lstUsingComponents = new ArrayList <Class <Component>>();
	
	public ArrayList <String> lstComponentsShortNames = new ArrayList <String>();
	
	public ArrayList <Class <Component>> lstBasicProcessingComponents = new ArrayList <Class <Component>>();
	
	

	public boolean transferData(Entity entity, DataInterface data) {
		return false;
	}
	
	public boolean transferLocalData(Component component, DataInterface data) {
		return false;
	}
	
	
	
	public boolean canCreateThisComponent(String componentName) {
		
		for (Class <Component> processingReflection : lstProcessingComponents) {
			
			String className = processingReflection.getSimpleName();
			
			if (className.contentEquals(componentName)) return true;
		}
		
		for (String shortName : lstComponentsShortNames) {
			
			if (shortName.contentEquals(componentName)) return true;
		}
		
		return false;
		
	}
	
	
	
	public Component connectComponent(Component component) {
		
		for (Class<Component> handledReflection : lstProcessingComponents) {
			
			if (handledReflection.isAssignableFrom(component.getClass())) {
				
				if (!component.lstLinkedHandlers.contains(this)) {
					
					component.lstLinkedHandlers.add(this);
					
					for (DataInterface data : component.lstData) data.lstLinkedHandlers.add(this);
				}
			} 
			
		}
		
		for (ComponentsHandler subHandler : lstSubHandlers) {
			
			for (Class <Component> componentClass : subHandler.lstUsingComponents) {
				
				if (componentClass.isAssignableFrom(component.getClass())) {
					
					subHandler.connectComponent(component);
					
					if (!component.lstLinkedSubHandlers.contains(subHandler)) {
						
						component.lstLinkedSubHandlers.add(subHandler);
					}
				}
			}
		}

		return component;
	}
	
	
	
	private ArrayList <DataCommunicationContext> lstContexts = new ArrayList <DataCommunicationContext>();
	
	//vv--------------------------------getContexts--------------------------------------vv
	public ArrayList <DataCommunicationContext> getContexts() {
		
		if (lstContexts.isEmpty()) {
			
			for (Class <Component> processingComponent : lstProcessingComponents) {
				
				Class <Component> component = processingComponent;
				
				//in all parents
				while (Component.class.isAssignableFrom(component)) {
					
					//to find fields
					for (Field field : component.getDeclaredFields()) {
						
						//which extended from data
						if (DataInterface.class.isAssignableFrom(field.getType())) {
							
							DataCommunicationContext currentContext = findExistingContext(field.getName()); 
							
							if (currentContext != null) {
								currentContext.context.lstLinkedComponents.add(processingComponent);
							} 
							else {
								currentContext = createContext(field.getName(), processingComponent);
							}
							
							setBasicProcessing(currentContext, processingComponent);
						}
					}
					
					component = (Class <Component>) component.getSuperclass();
				}
			}
		} 
		return lstContexts;
	}
	
	private DataCommunicationContext findExistingContext(String contextName) {
		
		for (DataCommunicationContext existingContext : lstContexts) {
			
			if (existingContext.dataNameInComponent.contentEquals(contextName)) {
				
				return existingContext;
			}
		}
		return null;
	}
	
	private DataCommunicationContext createContext(String contextName, Class<Component> processingReflection) {
		
		DataContext newDataContext = new DataContext(contextName, processingReflection);
		
		DataCommunicationContext createdContext = new DataCommunicationContext(
														contextName, 
														newDataContext
														); 
		lstContexts.add(createdContext);
		
		return createdContext;
	}
	
	private void setBasicProcessing(DataCommunicationContext createdContext, 
									Class<Component> processingComponent) {
		
		for (Class <Component>  basicProcessingReflection : lstBasicProcessingComponents) {
			
			//if added reflection to context was basic processed
			if (basicProcessingReflection.equals(processingComponent)) {
				
				if (!createdContext.context.lstBasicLinkedHandlers.contains(this)) {
					
					createdContext.context.lstBasicLinkedHandlers.add(this);
				}
			}
		}
	}
	//^^--------------------------------getContexts--------------------------------------^^
	
	public void setContextToComponent(Component component) {
		
		Class <Component> superComponent = (Class <Component>) component.getClass();
		
		while (Component.class.isAssignableFrom(superComponent)) {
			
			for (Field field : superComponent.getDeclaredFields()) {
				
				for (DataCommunicationContext context : lstContexts) {
					
					if (field.getName().contentEquals(context.dataNameInComponent)) {
						
						if (!Modifier.isPrivate(field.getModifiers())) {
							
							try {
								
								DataInterface data = (DataInterface) field.get(component);
								
								if (data != null) {
									
									data.setContext(context.context);
									data.setName(field.getName());
									
									if (!component.lstData.contains(data)) {
										component.lstData.add(data);
									}
									
									data.lstLocalLinkedHandlers.add(this);
									
									for (ComponentsHandler linkedHandler : context.context.lstBasicLinkedHandlers) {
										
										if (!data.lstLinkedHandlers.contains(linkedHandler)) {
											
											data.lstLinkedHandlers.add(linkedHandler);
										}
									}
								}
							
							} catch (IllegalArgumentException e) {
								Log.e(LOG_TAG, name + ": IllegalArgumentException");
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								Log.e(LOG_TAG, name + ": IllegalAccessException: " + field.getName());
								e.printStackTrace();
							}
						}
					}	
				}
			}
			superComponent = (Class <Component>) superComponent.getSuperclass();
		}
	}
	
	
	
	public Component createComponent(Entity master, TagOptions options) {
		
		Component newComponent = null;
		
		for (Class <Component> componentClass : lstProcessingComponents) {
			
			if (newComponent == null) {
				
				String shortName = "";
				
				try {
					shortName = (String) componentClass.getDeclaredField("SHORT_CLASS_NAME").get(null);
					
					if (shortName.contentEquals(options.name)
							|| componentClass.getSimpleName().contentEquals(options.name)) {
						
						newComponent = componentClass.newInstance();
					}
					
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
			} 
		}
		
		if (newComponent != null) {
			
			newComponent.setFactory(this);
			setContextToComponent(newComponent);
			newComponent.setOptions(options);
			
			if (master != null) {
				newComponent.entity = master;
				master.addComponent(newComponent);
			}
			
			newComponent = connectComponent(newComponent);
		
			return newComponent;
		}
		return null;
	}
}
