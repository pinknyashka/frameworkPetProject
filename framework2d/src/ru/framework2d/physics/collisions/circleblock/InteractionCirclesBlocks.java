package ru.framework2d.physics.collisions.circleblock;

import ru.framework2d.core.Component;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.InteractionShapes;
import ru.framework2d.physics.collisions.blocks.PhysicalBlock;
import ru.framework2d.physics.collisions.circles.PhysicalCircle;
import android.util.Log;

public class InteractionCirclesBlocks extends InteractionShapes {
	
	public InteractionCirclesBlocks() {
		super(2, (Class <Component>) PhysicalCircle.class.asSubclass(Component.class), 
				(Class <Component>) PhysicalBlock.class.asSubclass(Component.class));
		
		if (PhysicalCircle.BROADPHASE_HANDLER == null) PhysicalCircle.BROADPHASE_HANDLER = this;
		if (PhysicalBlock.BROADPHASE_HANDLER == null) PhysicalBlock.BROADPHASE_HANDLER = this;
		PhysicalCircle.setInteractionHandler((Class <Component>) PhysicalBlock.class.asSubclass(Component.class), this);
	}
	
	private PhysicalCircle circle;
	private PhysicalBlock block;
	
	@Override
	public boolean isInteractionHappened(Component firstReflection, Component secondReflection) {
		circle = (PhysicalCircle) firstReflection;
		block = (PhysicalBlock) secondReflection;
		int timeToCollision = timeToCollision();
		if (timeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL){
			return true;
		} 
		else if (timeToCollision < IntervalPhysics.PROCESSED_INTERVAL){
			IntervalPhysics.PROCESSED_INTERVAL = timeToCollision;
		}
		return false;
	}

	boolean choiseLongestSide; 
	
	private Point2[] potencialContactVertex = new Point2[] {new Point2(), new Point2()};
	private Vector2[] potencialContactVertexMove = new Vector2[] {new Vector2(), new Vector2()};
	
	private Vector2 fromCircleToBlock = new Vector2();
	private Vector2 fromPPointToCircle = new Vector2();
	private Vector2 fromCenterToBlockVertex = new Vector2();
	
	private Vector2 fromBlockToContactVertex = new Vector2();
	private Vector2 blockVertexAngularBase = new Vector2();
	private Vector2 blockVertexAngularMove = new Vector2();
	
	private Vector2 relativeCircleMovement = new Vector2();
	private Vector2 circleOrbitalMovement = new Vector2();
	
	private Vector2 circleMovement = new Vector2();
	private Vector2 blockMovement = new Vector2();
	
	private Vector2 fromNearestToSecondVertex = new Vector2();
	private Vector2 fromNearestToThirdVertex = new Vector2();

	private Vector2 fromNearestVertexToCircle = new Vector2();
	private Vector2 fromCircleToNearestVertexNormal = new Vector2();
	
	private Vector2 fromNearestToSecondVertexBase = new Vector2();
	
	private Vector2 proection = new Vector2();
	private Point2 nearPointOnLine = new Point2();
	private Point2 nearestPointOnCircle = new Point2();
	private Vector2 fromCircleToContactPoint = new Vector2();
	
	private Vector2 fromCircleToNearPoint = new Vector2();
	
	private Point2 crossPoint;
	private Vector2 fromNearestVertexToCrossPoint = new Vector2();
	private Vector2 fromNearCirclePointToCrossPoint = new Vector2();
	
	Vector2 circleMovementNormal = new Vector2();

	private int timeToCollision() {
		
		int timeToCollision = IntervalPhysics.PROCESSED_INTERVAL;
		double dblTimeToCollision = timeToCollision;
		
		double ratio = dblTimeToCollision / (double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;
		
		/*
		 * ---search for collisions with vertex of block---
		 * */
		
		// find nearest to circle vertex of block   
		fromCircleToBlock.set(block.position, circle.position);
    	fromCircleToBlock.restore();
    	 
		fromCenterToBlockVertex.set(block.width.value / 2, block.height.value / 2); //vector from block's center to block's vertex
		double blockAreaRadius = fromCenterToBlockVertex.length2();
		
		blockMovement.set(block.movement);
		blockMovement.selfMulti(block.movement.currentSpeed.value);
		circleMovement.set(circle.movement);
		circleMovement.selfMulti(circle.movement.currentSpeed.value);
		
		// twice for 2 opposite vertex (in block ABCD for vertex A and C firstly then for B and D)
		for (int i = 0; i < 2; i++) {
			fromCenterToBlockVertex.rotate(block.position.getAlpha()); //to global coordinate
			if (fromCenterToBlockVertex.myProectionOnVector(fromCircleToBlock) < 0) {
				//if this vertex on the same side with circle (A point is nearest (or B when i = 1))
				potencialContactVertex[i].set(block.position.x.value + fromCenterToBlockVertex.x.value, 
											block.position.y.value + fromCenterToBlockVertex.y.value);
			} 
			else { 
				//otherwise (C point is nearest (or D when i = 1))
				potencialContactVertex[i].set(block.position.x.value - fromCenterToBlockVertex.x.value, 
											block.position.y.value - fromCenterToBlockVertex.y.value);
			}
			//for next iteration i++
			fromCenterToBlockVertex.set(block.width.value / 2, -block.height.value / 2);
			
			fromBlockToContactVertex.set(potencialContactVertex[i], block.position);
			
			//rotate 90d
			blockVertexAngularBase.set(-fromBlockToContactVertex.y.value, fromBlockToContactVertex.x.value);
			blockVertexAngularBase.restore();
			
			//v = r * w, where w is angular speed in radian
			blockVertexAngularMove.set(blockVertexAngularBase);
			blockVertexAngularMove.selfMulti(blockAreaRadius * block.movement.alphaRotation.value);
			
			//relative vertex speed (removing circle movement from vertex movement) 
			potencialContactVertexMove[i].set(blockMovement);
			potencialContactVertexMove[i].addict(blockVertexAngularMove);
			potencialContactVertexMove[i].decrease(circleMovement);
			potencialContactVertexMove[i].selfMulti(ratio);
			
			//y = K * x + C; path of vertex
			double K = potencialContactVertexMove[i].y.value / potencialContactVertexMove[i].x.value; 
			double C = potencialContactVertex[i].y.value - K * potencialContactVertex[i].x.value;
			
			double t = C - circle.position.y.value, w = circle.position.x.value;
			double a = (1 + K * K), b = 2 * K * t - 2 * w; 
			double c = t * t - circle.radius.value * circle.radius.value + w * w;
			double D = b * b - 4 * a * c;
			if (D > 0) {
				double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
				x1 = (-b - Math.sqrt(D)) / (2 * a);
				
				//x2 = (-b + Math.sqrt(D)) / (2 * a); //same thing as this one:
				x2 = -x1 - b / a;
				
				y1 = x1 * K + C;
				y2 = x2 * K + C;
				
				boolean sameDirect = false; 
				double len1, len2;
				
				fromPPointToCircle.set(x1 - potencialContactVertex[i].x.value, y1 - potencialContactVertex[i].y.value);
				len1 = fromPPointToCircle.length2();
				if (fromPPointToCircle.myProectionOnVector(potencialContactVertexMove[i]) > 0) sameDirect = true;
				
				fromPPointToCircle.set(x2 - potencialContactVertex[i].x.value, y2 - potencialContactVertex[i].y.value);
				len2 = fromPPointToCircle.length2();
				if (fromPPointToCircle.myProectionOnVector(potencialContactVertexMove[i]) > 0) sameDirect = true;
				
				if (sameDirect) {
					double length = potencialContactVertexMove[i].length2();
					if (length > len1 || length > len2) {
						
						double lessLen = Math.min(len1, len2);
						dblTimeToCollision = (double) timeToCollision * (lessLen / length);
						timeToCollision = (int) dblTimeToCollision;
						
						ratio = dblTimeToCollision 
								/ (double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;
						
						if (timeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL) {
							
							block.contactPoint.set(potencialContactVertex[i]);
							
							fromCircleToContactPoint.set(potencialContactVertex[i], circle.position);
							fromCircleToContactPoint.restore();
							fromCircleToContactPoint.selfMulti(circle.radius.value);
							
							circle.contactPoint.set(circle.position);
							circle.contactPoint.move(fromCircleToContactPoint);
							
						} 
						else if (timeToCollision < IntervalPhysics.PROCESSED_INTERVAL) {
							IntervalPhysics.PROCESSED_INTERVAL = timeToCollision;
						}
					}
				}
			}
		} 
		/*
		 * ---search for collisions with borders of block---
		 * */
		
		//
		setCircleRelativeMovement(circle.position, ratio);
		double approximateLength = relativeCircleMovement.length2(); 

		// switch potencialContactVertex[0] to nearest vertex to circle (if it's not)
		switchFirstVertexToNearest();
		fromNearestVertexToCircle.set(circle.position, potencialContactVertex[0]);
		
		//	sets potencialContactVertex[1] and potencialContactVertex[0] to points of closest border 
		chooseNearestBorder();
		
		fromNearestToSecondVertex.set(potencialContactVertex[1], potencialContactVertex[0]);
		
		fromNearestToSecondVertexBase.set(fromNearestToSecondVertex);
		fromNearestToSecondVertexBase.restore();
		proection.set(fromNearestVertexToCircle.toVectorProectionOnVector(fromNearestToSecondVertexBase));
		
		nearPointOnLine.set(potencialContactVertex[0]);
		nearPointOnLine.move(proection);
		
		if (nearPointOnLine.dist(circle.position) >= circle.radius.value) {
			fromCircleToNearPoint.set(nearPointOnLine, circle.position);
			fromCircleToNearPoint.restore();
			fromCircleToNearPoint.selfMulti(circle.radius.value);
			
			nearestPointOnCircle.set(circle.position);
			nearestPointOnCircle.move(fromCircleToNearPoint);
			
			setCircleRelativeMovement(nearestPointOnCircle, ratio);
			
			// sets crossPoint to cross point of border and moving path of nearest point on circle
			crossPoint = findCrossPoint();
			
			if (crossPoint != null) {
				fromNearestVertexToCrossPoint.set(crossPoint, potencialContactVertex[0]);
				if (fromNearestVertexToCrossPoint.myProectionOnVector(fromNearestToSecondVertex) > 0 && 
							fromNearestVertexToCrossPoint.length2() < fromNearestToSecondVertex.length2()) {
					fromNearCirclePointToCrossPoint.set(crossPoint, nearestPointOnCircle);
					double distanceToCollision = fromNearCirclePointToCrossPoint.length2(), movementLength = relativeCircleMovement.length2();
					if (approximateLength > movementLength) movementLength = approximateLength;
					if (fromNearCirclePointToCrossPoint.myProectionOnVector(relativeCircleMovement) > 0 
																			&& distanceToCollision < movementLength) {
						
						double dblCurrentTimeToCollision = dblTimeToCollision * (distanceToCollision / movementLength);
						int currentTimeToCollision = (int) dblCurrentTimeToCollision;
						
						if (currentTimeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL
								&& dblCurrentTimeToCollision < dblTimeToCollision) {
							
							circle.contactPoint.set(nearestPointOnCircle);
							block.contactPoint.set(nearPointOnLine);
							
						}
						else if (currentTimeToCollision < IntervalPhysics.PROCESSED_INTERVAL) {
							IntervalPhysics.PROCESSED_INTERVAL = currentTimeToCollision;
						}
						if (currentTimeToCollision < timeToCollision) { 
							return currentTimeToCollision;
						} 
						else return timeToCollision;
					}
				}
			}
		} 
		else {
			if (choiseLongestSide) Log.d("Physical", "walk throw long side " + block.entity.name + ":" + circle.entity.name 
					+ "; " + nearPointOnLine.dist(circle.position) + " : " + circle.radius);
			else Log.d("Physical", "walk throw short side " + block.entity.name + ":" + circle.entity.name 
					+ "; " + nearPointOnLine.dist(circle.position) + " : " + circle.radius);
		}
		if (timeToCollision <= IntervalPhysics.PROCESSED_INTERVAL)
			return timeToCollision;
		else 
			return IntervalPhysics.PROCESSED_INTERVAL + 2;
	}
	
	private Vector2 fromBlockToNearPoint = new Vector2 ();
	
	private void setCircleRelativeMovement(Point2 mNearestPointOnCircle, double ratio) {
		relativeCircleMovement.set(circleMovement);
		relativeCircleMovement.decrease(blockMovement);
		relativeCircleMovement.selfMulti(ratio);
		
		circleOrbitalMovement.set(mNearestPointOnCircle, block.position);
		fromBlockToNearPoint.set(circleOrbitalMovement);
		circleOrbitalMovement.rotate(-block.movement.alphaRotation.value * ratio);
		circleOrbitalMovement.decrease(fromBlockToNearPoint);
		
		relativeCircleMovement.addict(circleOrbitalMovement);
	}
	
	private Point2 calulatedCrossPoint = new Point2(); 
	private Point2 findCrossPoint() {
		double K1, C1, K2, C2, x, y;
		if (fromNearestToSecondVertex.x.value == 0) {
			if (relativeCircleMovement.x.value == 0) {
				x = 0;
				y = 0;
				return null;
			} 
			else {
				K2 = relativeCircleMovement.y.value / relativeCircleMovement.x.value; 
				C2 = nearestPointOnCircle.y.value - K2 * nearestPointOnCircle.x.value;
				y = K2 * potencialContactVertex[0].x.value + C2;
				x = (y - C2) / K2;
			}
		} 
		else if (fromNearestToSecondVertex.y.value == 0) {
			//if (relativeCircleMovement.x.value == 0) { //useless
			if (relativeCircleMovement.x.value <= 0.000000000001 && relativeCircleMovement.x.value >= -0.000000000001) {
				x = nearestPointOnCircle.x.value;
				y = potencialContactVertex[0].y.value;
			} 
			else {
				K2 = relativeCircleMovement.y.value / relativeCircleMovement.x.value; 
				C2 = nearestPointOnCircle.y.value - K2 * nearestPointOnCircle.x.value;
				x = (potencialContactVertex[0].y.value - C2) / K2;
				y = K2 * x + C2;
			}
		} 
		else {
			if (relativeCircleMovement.x.value <= 0.000000000001 && relativeCircleMovement.x.value >= -0.000000000001) {
				K2 = fromNearestToSecondVertex.y.value / fromNearestToSecondVertex.x.value; 
				C2 = potencialContactVertex[0].y.value - K2 * potencialContactVertex[0].x.value;
				x = nearestPointOnCircle.x.value;
				y = K2 * x + C2;
			} 
			else {
				K1 = fromNearestToSecondVertex.y.value / fromNearestToSecondVertex.x.value; 
				C1 = potencialContactVertex[0].y.value - K1 * potencialContactVertex[0].x.value;
				K2 = relativeCircleMovement.y.value / relativeCircleMovement.x.value; 
				C2 = nearestPointOnCircle.y.value - K2 * nearestPointOnCircle.x.value;
				if (K1 != K2) {
					x = (C2 - C1) / (K1 - K2);
					y = K1 * x + C1;
				} 
				else {
					x = 0; y = 0;
					return null;
				}
			} 
		}
		calulatedCrossPoint.set((float) x, (float) y);
		return calulatedCrossPoint;
	}

	private Point2 tmp = new Point2();
	
	private void switchFirstVertexToNearest() {
		if (potencialContactVertex[0].dist(circle.position) 
				> potencialContactVertex[1].dist(circle.position)) {
		
			tmp.set(potencialContactVertex[0]);
			potencialContactVertex[0].set(potencialContactVertex[1]);
			potencialContactVertex[1].set(tmp);
		}
	}
	
	private Point2 secondPotencialContactVertex = new Point2();
	private Point2 thirdPotencialContactVertex = new Point2();
	
	private void chooseNearestBorder() {
		
		secondPotencialContactVertex.set(potencialContactVertex[1]);

		fromCenterToBlockVertex.set(potencialContactVertex[1], block.position);
		fromCenterToBlockVertex.set(-fromCenterToBlockVertex.x.value, -fromCenterToBlockVertex.y.value);
		
		thirdPotencialContactVertex.set(block.position);
		thirdPotencialContactVertex.move(fromCenterToBlockVertex);
		
		fromNearestToSecondVertex.set(secondPotencialContactVertex, potencialContactVertex[0]);
		fromNearestToThirdVertex.set(thirdPotencialContactVertex, potencialContactVertex[0]);
		
		choiseLongestSide = fromNearestToSecondVertex.length2() > fromNearestToThirdVertex.length2();
		
		if (fromNearestToSecondVertex.myProectionOnVector(fromNearestVertexToCircle) < 0) {
			//окружность не лежит напротив того ребра которое мы приняли за ближайшее
			
			if (fromNearestToThirdVertex.myProectionOnVector(fromNearestVertexToCircle) < 0) {
				//а так же не лежит напротив второго ребра
				
				fromCircleToNearestVertexNormal.set(-fromNearestVertexToCircle.y.value, fromNearestVertexToCircle.x.value);
				fromCircleToNearestVertexNormal.restore();
				
				if ((relativeCircleMovement.myProectionOnVector(fromCircleToNearestVertexNormal) 
						* fromNearestToSecondVertex.myProectionOnVector(fromCircleToNearestVertexNormal)) < 0) {
					
					choiseLongestSide = !choiseLongestSide;
					potencialContactVertex[1].set(thirdPotencialContactVertex);
				}
				
			} 
			else {
				choiseLongestSide = !choiseLongestSide;
				potencialContactVertex[1].set(thirdPotencialContactVertex);
			}
		} 
	}
}

