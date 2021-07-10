package ru.framework2d.physics.collisions.circles;

import java.util.HashMap;
import java.util.Map;

import ru.framework2d.core.InteractionHandler;
import ru.framework2d.core.Property;
import ru.framework2d.core.Component;
import ru.framework2d.data.Fractional;
import ru.framework2d.physics.collisions.InteractionShapes;
import ru.framework2d.physics.collisions.PhysicalShape;


public class PhysicalCircle extends PhysicalShape {

	public final static String SHORT_CLASS_NAME = "circle";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	
	
	public Fractional radius = new Fractional(0);
	
	
	
	public final static Class <Component> COMPONENT_CLASS = (Class <Component>) (PhysicalCircle.class.asSubclass(Component.class));
	@Override
	public Class <Component> getComponentClass() {
		return COMPONENT_CLASS;
	}
	
	
	
	public static InteractionShapes BROADPHASE_HANDLER = null;
	@Override
	public InteractionShapes getBroadphaseHandler() {
		return BROADPHASE_HANDLER;
	}
	
	
	
	private static Map <Class <Component>, 
						InteractionHandler> interactionHandlers  = new HashMap <Class <Component>,
										InteractionHandler>();
	public static void setInteractionHandler(	Class <Component> interactedClass, 
										InteractionHandler handler) {
		interactionHandlers.put(interactedClass, handler);
	}
	
	@Override
	public InteractionShapes getInteractionHandler(Class <Component> interactedClass) {
		return (InteractionShapes) interactionHandlers.get(interactedClass);
	}

	
	
	public PhysicalCircle() {
		super();
	}
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("radius")) {
			radius.setValue(property.data);
			occupiedArea.setArea(2 * radius.value, 2 * radius.value, 2 * radius.value, radius.value);
			occupiedArea.setCenter(this.position);
			occupiedArea.isSphere = true;
			occupiedArea.isAabb = true;
			occupiedArea.validateAabb();
		}	
	}

}
