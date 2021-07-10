package ru.framework2d.physics.collisions.particular;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.collisions.PhysicalShape;

public class PhysicalParticular extends PhysicalShape {
	
	public Point2 target = new Point2();
	public Vector2 hitNormal = new Vector2();
	public Bool hited = new Bool(false);
	
	//PhysicalLine tp;
	
	public PhysicalParticular() {
		super();
		occupiedArea.setArea(0, 0, 0, 0);
	}

}
