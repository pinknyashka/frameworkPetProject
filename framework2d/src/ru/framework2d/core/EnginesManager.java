package ru.framework2d.core;

import java.util.ArrayList;

import ru.framework2d.data.DataInterface;

import android.util.Log;

public class EnginesManager {
	
	private static final String LOG_TAG = "core";
	
	public static boolean PARALLEL_WORK = false;
	
	public ArrayList <Engine> lstEngines = new ArrayList <Engine>();
	
	public ArrayList <ComponentsHandler> lstHandlers = new ArrayList <ComponentsHandler>();

	protected ArrayList <DataContext> lstCommonContexts = new ArrayList <DataContext>();
	
	
	
	private Component currentCreatedComponent;
	
	private ComponentsHandler currentCreatedHandler;
	
	private Engine currentCreatedEngine;
	
	
	public EnginesManager(EntityStorage entityStorage, GameResources resources) {
		
		Engine.resources = resources;
		Engine.entityStorage = entityStorage;
		Engine.enginesManager = this;
		
		Engine.enginesScheduler = new EnginesScheduler();
		
		currentCreatedEngine = Engine.enginesScheduler;
	}
	
	
	
	/**
	 * Connect engine specified by name. Also sets options 
	 * @param engineName - engine name 
	 * @param options - options to set to connected engine     
	 */
	@SuppressWarnings("unchecked")
	public void connectEngine(TagOptions options) {
		
		Engine newEngine = null;
		
		Class <Engine> engineClass = null;
		
		String path = "";
		
		try {
					
			Property enginePackage = options.findPropertyByName("package");
			
			if (enginePackage == null) {

				path += currentCreatedEngine.getClass().getPackage().getName() + ".";
				
				Property engineSubpackage = options.findPropertyByName("subpackage");
				
				if (engineSubpackage != null) {
					
					path += engineSubpackage.data.toString() + ".";
				}
			}
			else if (!enginePackage.data.toString().contentEquals("")) {
				
				path += enginePackage.data.toString() + ".";
			}
			
			Property engineName = options.findPropertyByName("engineName");
			
			if (engineName != null) {
				
				path += engineName.data.toString();
			}
			
			engineClass = (Class <Engine>) Class.forName(path);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			
			newEngine = Engine.createEngine(options, engineClass);
			
		}
		
		if (newEngine != null) {
			
			lstEngines.add(newEngine);
			
			if (options.name.contentEquals("handler")) {
				currentCreatedHandler = (ComponentsHandler) newEngine;
				lstHandlers.add(currentCreatedHandler);
			}

			if (currentCreatedEngine != null) {
				currentCreatedEngine.addSubengine(newEngine);
			}
			
			currentCreatedEngine = newEngine;
		}
	}
	
	
	
	/**
	 * Notifies engines manager that the closing tag for handler appears in map
	 * Sets contexts of created engine 
	 */
	public void engineCreationIsOver() {
		
		if (currentCreatedHandler == currentCreatedEngine) {
			ComponentsHandler handler = currentCreatedHandler;
			
			Log.d(LOG_TAG, handler.name + " created;");
			
			ArrayList <DataCommunicationContext> lstNewHandlerContexts = handler.getContexts();
			
			//по всем связям дата-контекст нового движка
			for (DataCommunicationContext newHandlerContext : lstNewHandlerContexts) {
				boolean gotCreatedContext = false;
				//по всем общим контекстам 
				for (DataContext commonContext : lstCommonContexts) {
					//если контексты совпадают по имени
					if (newHandlerContext.context.name.contentEquals(commonContext.name)) {
						//синхронизация отражений хранящихся в контексте
						
						//по всем отражениям свзяанным с контекстом обрабатываемым новым движком 
						for (Class <Component> newHandlerLinkedReflection : newHandlerContext.context.lstLinkedComponents) {
							boolean gotSameReflection = false;
							//по всем отражениям встречающимся в общем контексте 
							for (Class <Component> commonLinkedReflection : commonContext.lstLinkedComponents) {
								if (newHandlerLinkedReflection.equals(commonLinkedReflection)) {
									//отражение в контексте нового движка совпало с отражением из общего контеста 
									gotSameReflection = true;
									//убедиться есть ли у отражения обработчик
									boolean gotBasicHandler = false;
									if (commonContext.lstBasicLinkedHandlers.isEmpty()) {
										gotBasicHandler = true; //обработчик отражения был, делать нечего
									}
									if (!gotBasicHandler) {//обработчика небыло, добавляем себя в качестве базового обработчика
										for (Class <Component> basicProcessingReflection : handler.lstBasicProcessingComponents) {												
											if (basicProcessingReflection.equals(commonLinkedReflection)){
												commonContext.lstBasicLinkedHandlers.add(handler);
											}
										}
									}
								}
							}
							if (!gotSameReflection) {
								commonContext.lstLinkedComponents.add(newHandlerLinkedReflection);
								//убедиться есть ли у отражения обработчик
								boolean gotBasicHandler = false;
								for (ComponentsHandler reflectionsHandler : commonContext.lstBasicLinkedHandlers) {
									for (Class <Component> basicProcessingReflection : reflectionsHandler.lstBasicProcessingComponents) {
										//if (basicProcessingReflection.equals(newHandlerLinkedReflection)) {
										if (newHandlerLinkedReflection.isAssignableFrom(basicProcessingReflection)) {
											gotBasicHandler = true; //обработчик отражения был, делать нечего
										}
									}
								}
								if (!gotBasicHandler) { //обработчика небыло, добавляем себя в качестве базового обработчика
									for (Class <Component> basicProcessingReflection : handler.lstBasicProcessingComponents) {												
										if (basicProcessingReflection.equals(newHandlerLinkedReflection)) {
											commonContext.lstBasicLinkedHandlers.add(handler);
										}
									}
								}
							}
						}
						
						newHandlerContext.context = commonContext;
						gotCreatedContext = true;
					} 
				}
				if (!gotCreatedContext) lstCommonContexts.add(newHandlerContext.context);
			}
			//если обрабатываемое новым движком отражение имеет представления более высокого уровня то запомнить все связанные с ними обработчики   
			for (Class <Component> newHandlerComponent : handler.lstProcessingComponents) {
				
				for (ComponentsHandler processingHandler : lstHandlers) {
					
					for (Class <Component> component : processingHandler.lstProcessingComponents) {
						
						if (component.isAssignableFrom(newHandlerComponent)){
							
							handler.lstHighterLinkedHandlers.add(processingHandler);
						}
					}
				}
			}
			currentCreatedHandler = null;
		}
		
		if (currentCreatedEngine != null) {
			currentCreatedEngine = currentCreatedEngine.masterEngine;	
		}
		
	}

	
	
	/**
	 * Do the same as createComponentInEntity() for current created entity from entity storage
	 * @param options - options to apply on reflection
	 */
	public Component createComponentInCurrentEntity(TagOptions options) {
		
		return createComponentInEntity(
				Engine.entityStorage.currentCreatedEntity, 
				options
				);
	}
	
	/**
	 * Create new Component by options if it's possible and put it in master object. 
	 * Also returning created Component or null if there was no handlers which could create it.  
	 * @param entity - entity to put this component
	 * @param options - options to apply on component
	 */
	public Component createComponentInEntity(Entity entity, TagOptions options) {
		
		Log.d(LOG_TAG, "Trying to create component <" + options.name + 
				"> with " + lstHandlers.size() + " handlers;");
		
		Component createdComponent = null;
		
		boolean isSucces = false;
		int handlerNo = 0;
		while (handlerNo < lstHandlers.size() && !isSucces) {
			
			if (lstHandlers.get(handlerNo).canCreateThisComponent(options.name)) {
				
				Log.d(LOG_TAG, "Creating <" + options.name + "> in: " 
								+ lstHandlers.get(handlerNo).name + ";");
				
				createdComponent = lstHandlers.get(handlerNo).createComponent(entity, options);
				
				if (createdComponent != null) {
					
					isSucces = true;
					
					if (entity != null) {
						createdComponent.entity = entity;
						entity.addComponent(createdComponent);
					}
					
//					if (currentCreatedComponent != null) {
//						createdComponent.parentComponent = currentCreatedComponent;
//						currentCreatedComponent.lstChildren.add(createdComponent);
//					}
					
					currentCreatedComponent = createdComponent;
					
					for (ComponentsHandler handler : lstHandlers.get(handlerNo).lstHighterLinkedHandlers) {
						handler.connectComponent(createdComponent);
					}
					
				} 
				else Log.d(LOG_TAG, "Creation of <" + options.name + "> failed;");
			} 
			handlerNo++;
			
		}
		
		if (!isSucces) Log.d(LOG_TAG, "Unknown type or cant create <" + options.name + ">;");
		
		return createdComponent;
	}
	
	
	public void componentCreationIsOver() {
		
		if (currentCreatedComponent != null) {
			currentCreatedComponent = null;
			//currentCreatedComponent = currentCreatedComponent.parentComponent;	
		}
	}
	
	
	
	/**
	 * Sends components of this entity to linked handlers to let them connect it
	 * @param master - entity to connect     
	 */
	public void loadEntity(Entity master) {
		for (Component component: master.lstComponents) {
			for (ComponentsHandler handler: component.lstLinkedHandlers) {
				handler.connectComponent(component);
			}
		}
	}

	
	
	/**
	 * Sets context on currently added handler/component if it exist. 
	 * @param options - context options     
	 */
	@SuppressWarnings("unchecked")
	public void setContext(TagOptions options) {
		
		if (currentCreatedHandler != null) {
			
			ArrayList <DataCommunicationContext> lstNewHandlerContexts = currentCreatedHandler.getContexts();
			//если среди опций есть имя существующего контекста 
			//то создать новый контекст (или позаимствовать существующий) 
			//с именем указаным в параметре и заменить
			
			Property contextName = options.findPropertyByName("name");
			Property contextValue = options.findPropertyByName("value");
			
			Property transferValue = options.findPropertyByName("transferValue");
			
			if (contextName != null && contextValue != null && 
					!contextName.name.contentEquals("") && !contextName.data.toString().contentEquals("") && 
					!contextValue.name.contentEquals("") && !contextValue.data.toString().contentEquals("")) {
				//по всем опциям созданного движка
				for (DataCommunicationContext newHandlerContext : lstNewHandlerContexts) {
					//если среди имен переменных найдено соответствие с опцией
					if (contextName.data.toString().contentEquals(newHandlerContext.dataNameInComponent)) {
						newHandlerContext.context.name = contextValue.data.toString();
						if (transferValue != null) newHandlerContext.transferValue = transferValue.data.toString();
					}
				}
			}
		} 
		else if (currentCreatedComponent != null) {
			Property contextName = options.findPropertyByName("name");
			Property contextValue = options.findPropertyByName("value");
			String contextValues = contextValue.data.toString();
			
			if (contextName != null && contextValue != null) {
				for (DataInterface data : currentCreatedComponent.lstData) {
					if (data.getName().contentEquals(contextName.data.toString())) {
						if (!data.getContext().name.contentEquals(contextValues)) {
							
							boolean gotContext = false;
							
							ComponentsHandler makingFactory = 
									(ComponentsHandler) currentCreatedComponent.getFactory();
							
							for (DataContext commonContext : lstCommonContexts) {
								//если контексты совпадают по имени
								if (contextValues.contentEquals(commonContext.name)) {
									Log.d(LOG_TAG, "using common context " + commonContext.name + 
												" in data " + data.getName() +
												" in object " + currentCreatedComponent.entity.name);
									
									gotContext = true;
									boolean gotAdditionalLinkedHandler = false; 
									
									if (!commonContext.lstBasicLinkedHandlers.contains(makingFactory)) {
										gotAdditionalLinkedHandler = true;
										commonContext.lstBasicLinkedHandlers.add(makingFactory);
									}
									
									data.setContext(commonContext);
									data.lstLinkedHandlers.addAll(commonContext.lstBasicLinkedHandlers);
									
									if (!commonContext.lstLinkedComponents.contains((Class <Component>) currentCreatedComponent.getClass())) {
										commonContext.lstLinkedComponents.add((Class <Component>) currentCreatedComponent.getClass());
									}
									
									if (gotAdditionalLinkedHandler) {
									/* Set additional linked handler to exists linked component 
									 * */
										for (Component reflection : currentCreatedComponent.entity.lstComponents) {
											for (Class <Component> reflectionClass : commonContext.lstLinkedComponents) {
												if (reflection.getClass().isAssignableFrom(reflectionClass)) {
													for (DataInterface reflectionData : reflection.lstData) {
														if (reflectionData.getContext() == data.getContext()) {
															reflectionData.lstLinkedHandlers.clear();
															reflectionData.lstLinkedHandlers.addAll(commonContext.lstBasicLinkedHandlers);
															
															
//															for (ComponentsHandler handler : reflectionData.lstLinkedHandlers) {
//																Log.d(LOG_TAG, "linked handler " + handler.name);
//															}
															
														}
													}
												}
											}	
										}
									}
									
//									for (ComponentsHandler handler : data.lstLinkedHandlers) {
//										Log.d(LOG_TAG, "data linked handler " + handler.name);
//									}
									
								}
							}
							
							if (!gotContext) {
								Log.d(LOG_TAG, "creating context to data " + data.getName() + " in object " + currentCreatedComponent.entity.name);
								DataContext newContext = new DataContext(contextValues, (Class <Component>) currentCreatedComponent.getClass());
								lstCommonContexts.add(newContext);
								newContext.lstBasicLinkedHandlers.add(makingFactory);
								data.setContext(newContext);
								data.lstLinkedHandlers.add(makingFactory);
								
							}
						}
					}
				}
			}
		}
	}
	
}
