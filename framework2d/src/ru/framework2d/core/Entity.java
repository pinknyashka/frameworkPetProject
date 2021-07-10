package ru.framework2d.core;

import java.util.ArrayList;

import ru.framework2d.data.Bool;

import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.logic.LogicalInterface;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.touches.InputTouchInterface;


public class Entity {
	
	public String name = ""; 
	
	public Bool isSingle = new Bool(false);
	
	public boolean isPrototype = false;
	public int implementationsNum = 0;
	
	
	
	//list of all entity's representations 
	public ArrayList <Component> lstComponents = new ArrayList <Component>();
	
	
	//image of the entity
	public ArrayList <OutputGraphicalInterface> lstGraphicals = new ArrayList <OutputGraphicalInterface>();
	
	//the way you can feel the entity
	public ArrayList <InputTouchInterface> lstTouches = new ArrayList <InputTouchInterface>();
	
	//how this object can interact with other entities
	public ArrayList <PhysicalInterface> lstPhysicals = new ArrayList <PhysicalInterface>();
	
	//how this entity is thinking
	public ArrayList <LogicalInterface> lstLogicals = new ArrayList <LogicalInterface>();

	

	//Entity who is owner of this  
	public Entity parent;
	
	//list of entities whose parent is this object  
	//they have relevant properties, such as drawn area, touching area, some logic
	public ArrayList <Entity> lstChildren = new ArrayList <Entity>();
	
	
	
	public Entity() { }
	
    public Entity(String name) { this.name = name; }
    
    public Entity(TagOptions options) {
    	
		for (Property property : options.lstPropertys) {
			setProperty(property);
		}
		
		if (options.name.contentEquals("prototype")) isPrototype = true;
    }
    
    public Entity(Entity prototype) {
    	
    	isPrototype = false;
		prototype.implementationsNum++;
		name = prototype.name + prototype.implementationsNum;
		for (Component prototypeComponent : prototype.lstComponents) {
			
			Component clone = prototypeComponent.createClone(this);
			if (clone == null) {
				clone = prototypeComponent.createAutoClone(this);
			}
			
		}
    }
    
    
    
	public void addComponent(Component component) {
		
		if (!lstComponents.contains(component)) {
			
			lstComponents.add(component);
			
			if (component instanceof LogicalInterface) {
				lstLogicals.add((LogicalInterface) component);
			} 
			else if (component instanceof PhysicalInterface) {
				lstPhysicals.add((PhysicalInterface) component);
			} 
			else if (component instanceof OutputGraphicalInterface) {
				lstGraphicals.add((OutputGraphicalInterface) component);
			} 
			else if (component instanceof InputTouchInterface) {
				lstTouches.add((InputTouchInterface) component);
			}
		}
		
	}
	
	
	public void setProperty(Property property) {
		
		if (property.isNamed("name")) {
			name = property.data.toString();
		} 
		else if (property.isNamed("single")) {
			isSingle.setValue(property.data);
		}
		
	}
	
	public void setInnerOptions(TagOptions options) {
		
		for (Property property : options.lstPropertys) {
			
			setInnerProperty(property);
		}
	}
	
	public void setInnerProperty(Property property) {
		
		for (Component component : lstComponents) {
			
			component.setPropertyData(property);
		}
		
	}
    
}
