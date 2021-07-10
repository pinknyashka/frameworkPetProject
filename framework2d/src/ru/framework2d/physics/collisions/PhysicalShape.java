package ru.framework2d.physics.collisions;

import ru.framework2d.data.Area;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Point3;
import ru.framework2d.data.Vector3;
import ru.framework2d.physics.PhysicalInterface;

public abstract class PhysicalShape extends PhysicalInterface {
	
	public Bool isStatic = new Bool(false);
	
	public Area occupiedArea = new Area();
	public Fractional weight = new Fractional(10);
	
	public Point3 contactPoint = new Point3();

	public Vector3 impulse = new Vector3();
	
	
	
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
