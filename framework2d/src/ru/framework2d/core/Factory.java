package ru.framework2d.core;

import java.util.ArrayList;

public interface Factory {
	
	abstract boolean canCreateThisComponent(String componentName);
	
	abstract Component createComponent(Entity master, TagOptions options);
	
	abstract Component connectComponent(Component component);
	
	
	
	abstract void setContextToComponent(Component component);
	
	abstract ArrayList <DataCommunicationContext> getContexts();
	
}
