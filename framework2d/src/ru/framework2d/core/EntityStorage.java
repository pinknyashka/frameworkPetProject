package ru.framework2d.core;

import java.util.ArrayList;

public class EntityStorage {

	public ArrayList <Entity> lstEntities = new ArrayList <Entity>();
	
	public ArrayList <Entity> lstPrototypes = new ArrayList <Entity>();
	

	
	public Entity currentCreatedEntity;
	
	public Entity createEntity(TagOptions options) {
		
		Entity newEntity = new Entity(options);
		lstEntities.add(newEntity);
		
		if (currentCreatedEntity != null) {
			currentCreatedEntity.lstChildren.add(newEntity);
			newEntity.parent = currentCreatedEntity; 
		}
		
		currentCreatedEntity = newEntity;
		
		return newEntity;
	}
	
	public void entityCreationIsOver() {
		
		if (currentCreatedEntity != null) {
			currentCreatedEntity = currentCreatedEntity.parent; 
		}
	}
	
	
	public Entity createPrototype(TagOptions options) {
		
		Entity newPrototype = new Entity(options);
		newPrototype.isPrototype = true;
		lstPrototypes.add(newPrototype);
		
		if (currentCreatedEntity != null) {
			newPrototype.parent = currentCreatedEntity; 
		}
		
		currentCreatedEntity = newPrototype;
		
		return newPrototype;
	}
	
	public Entity createEntityByPrototype(Entity prototype) {
		
		Entity newEntity = new Entity(prototype);
		lstEntities.add(newEntity);

		if (prototype.parent != null) {
			newEntity.parent = prototype.parent;
			prototype.parent.lstChildren.add(newEntity);
		}

		return newEntity;
	}
	
}
