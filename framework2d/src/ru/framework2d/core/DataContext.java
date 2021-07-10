package ru.framework2d.core;

import java.util.ArrayList;

public class DataContext {
	
	public String name = "";
	
	public ArrayList <Class <Component>> lstLinkedComponents = new ArrayList <Class <Component>>();
	public ArrayList <ComponentsHandler> lstLinkedHandlers = new ArrayList <ComponentsHandler>();
	
	public ArrayList <ComponentsHandler> lstBasicLinkedHandlers = new ArrayList <ComponentsHandler>();

	public DataContext(Class <Component> linkedComponentClass) {
		lstLinkedComponents.add(linkedComponentClass);
	}
	public DataContext(String name, Class <Component> linkedComponentClass) {
		lstLinkedComponents.add(linkedComponentClass);
		this.name = name;
	}
	
}
