package ru.framework2d.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;


import android.util.Log;

public abstract class Component {
	
	private static final String LOG_TAG = "LocalCommit";
	
	public String getShortClassName() {
		return this.getClass().getSimpleName();
	}
	
	
	
	public Component() {
		setName(this.getClass().getSimpleName());
	}
	
	
	
	private String _name = "";
	
	public void setName(String name) {
		this._name = "" + name;
	}
	
	public String getName() {
		return "" + _name;
	}
	
	
	
//	public Component parentComponent;
//	
//	public ArrayList <Component> lstChildren = new ArrayList <Component>();
	
	

	public Position3 position = new Position3();
	
	
	
	public Entity entity;

	
	
	public ArrayList <ComponentsHandler> lstLinkedHandlers = new ArrayList <ComponentsHandler>();
	
	public ArrayList <ComponentsHandler> lstLinkedSubHandlers = new ArrayList <ComponentsHandler> ();

	public ArrayList <DataInterface> lstData = new ArrayList <DataInterface>();
	
	
	
	protected Factory factory;
	
	public void setFactory(Factory factory) {
		this.factory = factory;
	}
	
	public Factory getFactory() {
		return factory;
	}

	
	
	public Component createClone(Entity master) {
		return null;
	}
	
	
	
	public Component createAutoClone(Entity master) {
		Component clone = null;
		
		try {
			clone = (Component) this.getClass().newInstance();
		} catch (InstantiationException e) {
			clone = null;
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			clone = null;
			e.printStackTrace();
		}
		
		if (clone != null) {
			clone.setPrototypePropertys(this);
			clone.entity = master;
			master.addComponent(clone);
		}
		return clone;
	}
	
	
	
	protected void setPrototypePropertys(Component prototype) {
		
		if (prototype.getClass().equals(this.getClass())) {
			
			Class <Component> superReflection = (Class<Component>) prototype.getClass();
			
			while (Component.class.isAssignableFrom(superReflection)) {
				
				setMatchingFields(prototype, superReflection);
				
				superReflection = (Class <Component>) superReflection.getSuperclass();
			}
		}
	}
	
	
	
	private void setMatchingFields(Component prototype, Class<Component> reflection) {
		for (Field field : reflection.getDeclaredFields()) {
			if (!Modifier.isPrivate(field.getModifiers())) {
				if (DataInterface.class.isAssignableFrom(field.getType())) {
					
					setPrototypeDataToField(prototype, field);
					
				} 
				else if ((!Modifier.isFinal(field.getModifiers())  )) {
								/*&& (field.getType().isPrimitive() 
								|| String.class.equals(field.getType())
								|| field.getType().isArray())) 
								|| Reflection.class.isAssignableFrom(field.getType())) {*/ 
					try {
						field.set(this, field.get(prototype));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						Log.e("Component", "wrong access to field " + field.getName());
						e.printStackTrace();
					}
				} 
			}
		}
		
	}

	
	
	private void setPrototypeDataToField(Component prototype, Field field) {
		try {
			DataInterface prototypeData = (DataInterface) field.get(prototype);
			DataInterface myData = (DataInterface) field.get(this);
			if (myData == null) {
				myData = prototypeData.getClass().newInstance();
				field.set(this, myData);
			}
			myData.setData(prototypeData);
			myData.setName(field.getName());
			if (!this.lstData.contains(myData)) this.lstData.add(myData);
			myData.lstLinkedHandlers.addAll(prototypeData.lstLinkedHandlers);
			myData.lstLocalLinkedHandlers.addAll(prototypeData.lstLocalLinkedHandlers);
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, _name + ": IllegalArgumentException");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.e(LOG_TAG, _name + ": IllegalAccessException");
			e.printStackTrace();
		} catch (InstantiationException e) {
			Log.e(LOG_TAG, _name + ": InstantiationException");
			e.printStackTrace();
		}
	}

	
	
	public void setOptions(TagOptions options) {
		
		for (int propertyNo = 0; propertyNo < options.lstPropertys.size(); propertyNo++) {
			
			setPropertyData(options.lstPropertys.get(propertyNo));
		} 
	}
	
	
	
	public void setPropertyData(Property property) {
		
		for (DataInterface innerData : lstData) {
			
			if (innerData.setPropertyData(property.name, property.data)) {
				return;
			}
		}
	}
}
