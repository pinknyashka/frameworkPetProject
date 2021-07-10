package ru.framework2d.physics.medium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.framework2d.core.InteractionHandler;
import ru.framework2d.core.Property;
import ru.framework2d.core.Component;

import ru.framework2d.data.Area;
import ru.framework2d.data.Fractional;

import ru.framework2d.physics.PhysicalInterface;


public class PhysicalMedium extends PhysicalInterface {
	
	public final static String SHORT_CLASS_NAME = "surface";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	
	
	public Area occupiedArea = new Area();
	
	public Fractional slowingEffect = new Fractional(1.0f);
	
	public ArrayList <PhysicalBody> lstContactingPhysicals = new ArrayList <PhysicalBody>();

	
	
	public final static Class <Component> COMPONENT_CLASS = 
			(Class <Component>) (PhysicalMedium.class.asSubclass(Component.class));
	
	@Override
	public Class <Component> getComponentClass() {
		return COMPONENT_CLASS;
	}
	
	
	public static void setInteractionHandler(	Class <Component> interactedClass, 
										InteractionHandler handler) {
		
		interactionHandlers.put(interactedClass, handler);
	}
	
	
	
	private static Map <Class <Component>, InteractionHandler> 
			interactionHandlers = new HashMap <Class <Component>, InteractionHandler>();
	
	@Override
	public InteractionHandler getInteractionHandler(Class <Component> interactedClass) {
		return (InteractionHandler) interactionHandlers.get(interactedClass);
	}
	
	
	public PhysicalMedium() { super(); occupiedArea.setCenter(position); }
	
//	@Override
//	public boolean step(int t, double interval) {
//		for (int contactingBodyNo = 0; contactingBodyNo < contactingPhysicals.size(); contactingBodyNo++) {
//			PhysicalBody contactingPhysical = contactingPhysicals.get(contactingBodyNo);
//			double k;
//			//double s = contactingPhysical.movement.currentSpeed; 
//					//+ Math.abs(contactingPhysical.movement.alphaRotation) * 5.0d;
//			double s = Math.sqrt(contactingPhysical.movement.currentSpeed.value * 
//									contactingPhysical.movement.currentSpeed.value + 
//									contactingPhysical.movement.alphaRotation.value * 
//									contactingPhysical.movement.alphaRotation.value);
//			
//			if (s >= DirectPhysicsInterface.IMPULSE_LIMIT) {
//				s = DirectPhysicsInterface.IMPULSE_LIMIT;
//			}
//			
//			k = (1.0f + (
//					(float) s * 
//					(1.0f - contactingPhysical.roughness.value) / 
//							DirectPhysicsInterface.IMPULSE_LIMIT - 1.0f) * 
//					slowingEffect.value 
//					) * Math.PI / 2;
//			
//			k = Math.sin(k); 
//			
//			while (interval > 1.0d) {
//				k = k * k;
//				interval -= 1.0d;
//			}
//			
//			k = 1.0d - interval + k * interval;
//			if (s < 0.01d) k = 0;
//			
//			contactingPhysical.movement.currentSpeed.value *= k;
//			contactingPhysical.movement.alphaRotation.value *= k;
//			//Log.d(LOG_TAG, "HandlerPhysicalEnvironments: k = " + k + "; interval = " + interval);
//			if (k == 0) {
//				contactingPhysicals.remove(contactingPhysical);
//				contactingBodyNo--;
//			}
//			contactingPhysical.movement.commit((ReflectionsHandler) getFactory(), contactingPhysical.master);
//		}
//		return false;
//	}
	@Override
	public boolean isActive() {
		return false;
	}
	
	@Override
	public void setPropertyData(Property property) {
		super.setPropertyData(property);
		
		if (property.isNamed("size:x")) {
			
			occupiedArea.x.setValue(property.data); 
			occupiedArea.isAabb = true;
			occupiedArea.validateAabb();
		} 
		else if (property.isNamed("size:y")) {
			
			occupiedArea.y.setValue(property.data); 
			occupiedArea.isAabb = true;
			occupiedArea.validateAabb();
		}

	}

}
