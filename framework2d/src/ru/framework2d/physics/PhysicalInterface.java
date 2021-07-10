package ru.framework2d.physics;

import ru.framework2d.core.InteractionHandler;
import ru.framework2d.core.Component;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Movement3;
import ru.framework2d.data.Vector2;


public abstract class PhysicalInterface extends Component {
	
	public Movement3 movement = new Movement3(1, 0, 0);
	
	public Bool isEnabled = new Bool(true);

	
	
	public InteractionHandler getInteractionHandler(Class <Component> interactedClass) {
		return null;
	}
	
	public Class <Component> getComponentClass() {
		return null;
	}
	
	
	
	public boolean isActive() {
		return (isEnabled.value && movement.isMoving());
	}
	
	
	
	public void setImpulse(Vector2 direct, double p) {
		movement.set(direct);
		movement.restore();
		movement.currentSpeed.value = p;
	}
	
	
	
	public boolean step(int t, double interval) {
		
		if (movement.currentSpeed.value > 0 || movement.alphaRotation.value > 0) {
			
			position.move(movement, movement.currentSpeed.value * interval);
			position.alpha.value += movement.alphaRotation.value * interval;
 
			if (position.alpha.value > 12.57) position.alpha.value -= Math.PI * 4;
			else if (position.alpha.value < -12.57) position.alpha.value += Math.PI * 4;
			
			position.localCommit(null, this, position);
			
			return true; 
		}
		return false;
	}
}
