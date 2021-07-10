package ru.framework2d.physics.medium;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.InteractionHandler;
import ru.framework2d.physics.IntervalPhysics;

public class InteractionMediumBody extends ComponentsHandler implements InteractionHandler {
	
	public InteractionMediumBody() {
		super(2, (Class <Component>) PhysicalMedium.class.asSubclass(Component.class), 
				(Class <Component>) PhysicalBody.class.asSubclass(Component.class));
		
		PhysicalMedium.setInteractionHandler((Class <Component>) PhysicalBody.class.asSubclass(Component.class), this);
	}
	
	public boolean toHandleInteraction(Component firstReflection, Component secondReflection) {
		
		PhysicalMedium medium = (PhysicalMedium) firstReflection;
		PhysicalBody body = (PhysicalBody) secondReflection;
		
		double interval = (double) IntervalPhysics.PROCESSED_INTERVAL / 
						(double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;
		double k;
		double s = Math.sqrt(body.movement.currentSpeed.value * body.movement.currentSpeed.value + 
							body.movement.alphaRotation.value * body.movement.alphaRotation.value);
		
		if (s >= IntervalPhysics.IMPULSE_LIMIT) {
			s = IntervalPhysics.IMPULSE_LIMIT;
		}
		
		if (s < 0.01d) {
			k = 0;
		}
		else {
			
			double flatness = 1.0d - (double) body.roughness.value;
			double mediumFlatness = 1.0d - (double) medium.slowingEffect.value;
			
			double speedReduce = s / (double) IntervalPhysics.IMPULSE_LIMIT;
			
			k = mediumFlatness + (double) medium.slowingEffect.value * speedReduce * flatness;
			
			k = Math.sin(k * Math.PI / 2); 

			while (interval > 1.0d) {
				k *= k;
				interval -= 1.0d;
			}
			
			k *= 1.0d - (1.0d - k) * interval;
		}
		
		body.movement.currentSpeed.value *= k;
		body.movement.alphaRotation.value *= k;
		
		body.movement.commit(masterEngine, body.entity);
		
		return true;
	}

	public boolean isInteractionPossible(Component firstComponent, Component secondComponent) {
		return false;
	}

	public boolean isInteractionHappened(Component firstComponent, Component secondComponent) {
		return ((PhysicalMedium) firstComponent).occupiedArea.contains(secondComponent.position);
	}

	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	@Override
	public boolean loadEntities(EntityStorage objectManager) {
		return false;
	}
}
