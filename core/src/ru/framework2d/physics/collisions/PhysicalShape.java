package ru.framework2d.physics.collisions;

import ru.framework2d.data.Area;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point3;
import ru.framework2d.data.Trigger;
import ru.framework2d.data.Vector3;
import ru.framework2d.physics.PhysicalInterface;

public abstract class PhysicalShape extends PhysicalInterface {
	
	public Bool isStatic = new Bool(false);
	
	public Area occupiedArea = new Area();
	public Fractional weight = new Fractional(10);
	
	public Point3 contactPoint = new Point3();

	public Vector3 impulse = new Vector3();

	//relative speed (px per base interval) at the last collision; written by the
	//interaction that fires the collision signal, read by its listeners (e.g. sound volume)
	public Fractional collisionSpeed = new Fractional();

	//the collision signal, propagated to other subsystems that react to it
	public Trigger collision = new Trigger();
	
	
	
	public InteractionShapes getBroadphaseHandler() {
		return null;
	}
	
	
	
	
	public PhysicalShape() {
		occupiedArea.setCenter(this.position);
	}
	
	
	
	@Override
	public boolean step(int t, double interval) {
		boolean stepDone = super.step(t, interval);
		
		if (stepDone) {
			if (occupiedArea.isAabb && movement.alphaRotation.value == 0) {
				occupiedArea.validateAabb();
			}
		}
		
		return stepDone;
	}
	
}
