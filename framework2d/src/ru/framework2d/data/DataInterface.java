package ru.framework2d.data;

import java.util.ArrayList;

import ru.framework2d.core.Entity;
import ru.framework2d.core.DataContext;
import ru.framework2d.core.Engine;
import ru.framework2d.core.Expression;
import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;

public abstract class DataInterface {
	
	/**
	 * Sets the data parameters to this, such as context and name 
	 * Also try to set value of the taken data, if setValue() is overridden by inheritor   
	 * @param data - data to copy parameters from
	 */
	public void setData(DataInterface data) {
		setValue(data);
		setContext(data.getContext());
		setName(data.getName());
	}
	
	/**
	 * Sets the data values to this. Must be overridden by inheritor   
	 * @param data - data to copy values from
	 */
	abstract void setValue(DataInterface data);
	
	
	
	/**
	 * Sets taken value of the property specified by name
	 * Must be overridden by inheritor, where name must be parsed       
	 * @param name - name of the taken property
	 * @param value - value of taken property represented in string
	 */
	public abstract boolean setProperty(String name, String value);
	
	
	
	/**
	 * Sets the value of taken data if specified name equals name of this data
	 * @param name - name of the taken property
	 * @param data - data to copy values from
	 */
	public boolean setPropertyData(String name, DataInterface data) {
		
		if (getName().contentEquals(name)) {
			setValue(data);
			return true;
		}
		return false;
	}
	
	
	
	private String name = "";
	
	public String getName() {
		
		if (name.length() > 0) {
			return "" + name;	
		}
		else {
			return this.getClass().getSimpleName(); 
		}
	}
	
	public void setName(String name) {
		this.name = "" + name;
	}
	
	
	
	public DataInterface getInnerData(String propertyName) {
		return null;
	}
	
	

	private DataContext context = null;
	
	public DataContext getContext() {
		return context;
	}
	
	public void setContext(DataContext context) {
		this.context = context;
	}
	
	

	boolean gotBinding = false;
	private Expression binding;
	public void setBinding(String binding) {
		if (!binding.contentEquals("")) {
			
		}
	}
	
	
	
	public ArrayList <ComponentsHandler> lstLinkedHandlers = new ArrayList <ComponentsHandler>();
	
	private int linkedHandlersNum;
	
	private ComponentsHandler linkedHandler;
	
	private boolean transfered;
	
	public void commit(Engine sender, Entity object) {
		
		/*if (gotBinding) {
			value = binding.calculate();
		}*/
		linkedHandlersNum = lstLinkedHandlers.size(); 
		
		for (int i = 0; i < linkedHandlersNum; i++) {
			
			linkedHandler = lstLinkedHandlers.get(i); 
			
			if (linkedHandler != sender && linkedHandler != null) {
				
				transfered = linkedHandler.transferData(object, this);
				
				if (!transfered) {
					lstLinkedHandlers.remove(i--);
					linkedHandlersNum--;
				}
			}
		}
	}
	
	public ArrayList <ComponentsHandler> lstLocalLinkedHandlers = new ArrayList <ComponentsHandler>();
	
	private int localLinkedHandlersNum;
	
	private ComponentsHandler localLinkedHandler;
	
	private boolean localTransfered;
	
	public void localCommit(Engine sender, Component reflection, DataInterface data) {
		
		localLinkedHandlersNum = lstLocalLinkedHandlers.size(); 
		
		for (int i = 0; i < localLinkedHandlersNum; i++) {
			
			localLinkedHandler = lstLocalLinkedHandlers.get(i);
			
			if (localLinkedHandler != sender && localLinkedHandler != null) {
				
				localTransfered = localLinkedHandler.transferLocalData(reflection, data);
				
				if (!localTransfered) {
					lstLocalLinkedHandlers.remove(i--);
					localLinkedHandlersNum--;
				}
			}
		}
	}
	
}

