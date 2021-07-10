package ru.framework2d.physics.collisions.blocks;

import java.util.HashMap;
import java.util.Map;

import ru.framework2d.core.InteractionHandler;
import ru.framework2d.core.Property;
import ru.framework2d.core.Component;
import ru.framework2d.data.Fractional;
import ru.framework2d.physics.collisions.InteractionShapes;
import ru.framework2d.physics.collisions.PhysicalShape;

public class PhysicalBlock extends PhysicalShape {

	public final static String SHORT_CLASS_NAME = "block";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Fractional width = new Fractional(0);
	public Fractional height = new Fractional(0);
	public Fractional depht = new Fractional(0);
	
	public PhysicalBlock() {super(); }
	
	
	
	public final static Class <Component> REFLECTION_CLASS = (Class <Component>) (PhysicalBlock.class.asSubclass(Component.class));
	
	@Override
	public Class <Component> getComponentClass() {
		return REFLECTION_CLASS;
	}
	
	
	
	public static InteractionShapes BROADPHASE_HANDLER = null;
	
	@Override
	public InteractionShapes getBroadphaseHandler() {
		return BROADPHASE_HANDLER;
	}
	
	
	
	public static void setInteractionHandler(	Class <Component> interactedClass, 
										InteractionHandler handler) {
		interactionHandlers.put(interactedClass, handler);
	}
	
	
	
	private static Map <	Class <Component>, InteractionHandler> 
			interactionHandlers  = new HashMap <Class <Component>, InteractionHandler>();
	
	@Override
	public InteractionShapes getInteractionHandler(Class <Component> interactedClass) {
		return (InteractionShapes) interactionHandlers.get(interactedClass);
	}
	
	
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("width")) {
			width.setValue(property.data);
			occupiedArea.setArea(width.value, height.value, depht.value, 
					(float) Math.sqrt(width.value * width.value / 4 + height.value * height.value / 4 + depht.value * depht.value / 4));
			occupiedArea.setCenter(this.position);
			occupiedArea.validateAabb();
			occupiedArea.isAabb = true;
		} 
		else if (property.isNamed("height")) {
			height.setValue(property.data);
			occupiedArea.setArea(width.value, height.value, depht.value, 
					(float) Math.sqrt(width.value * width.value / 4 + height.value * height.value / 4 + depht.value * depht.value / 4));
			occupiedArea.setCenter(this.position);
			occupiedArea.validateAabb();
			occupiedArea.isAabb = true;
		} 
		else if (property.isNamed("depht")) { 
			depht.setValue(property.data);
			occupiedArea.setArea(width.value, height.value, depht.value, 
					(float) Math.sqrt(width.value * width.value / 4 + height.value * height.value / 4 + depht.value * depht.value / 4));
			occupiedArea.setCenter(this.position);
			occupiedArea.validateAabb();
			occupiedArea.isAabb = true;
		}
	}

}
