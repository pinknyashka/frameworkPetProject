package ru.framework2d.physics.collisions.blocks;

import ru.framework2d.core.Component;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.InteractionShapes;
import ru.framework2d.physics.collisions.PhysicalShape;


public class InteractionBlocks extends InteractionShapes {
	
	public InteractionBlocks() {
		
		super(2, (Class <Component>) PhysicalBlock.class.asSubclass(Component.class), 
				(Class <Component>) PhysicalBlock.class.asSubclass(Component.class));
		
		PhysicalBlock.BROADPHASE_HANDLER = this;
		
		PhysicalBlock.setInteractionHandler((Class <Component>) PhysicalBlock.class.asSubclass(Component.class), this);
	}
	
	private int collisionsNum = 0;
	
	private Point2[] potencialContactVertex = new Point2[] {	new Point2(), new Point2(), 
																new Point2(), new Point2(), 
																new Point2(), new Point2(), 
																new Point2(), new Point2()};
	
	private Vector2[] potencialContactVertexMove = new Vector2[] {	new Vector2(), new Vector2(),
																	new Vector2(), new Vector2(),
																	new Vector2(), new Vector2(),
																	new Vector2(), new Vector2()};

	private Vector2[] potencialContactVertexMomentalMove = new Vector2[] {	new Vector2(), new Vector2(),
																			new Vector2(), new Vector2(),
																			new Vector2(), new Vector2(),
																			new Vector2(), new Vector2()};

	private Vector2[] potencialContactBorder = new Vector2[] {new Vector2(), new Vector2(), 
															new Vector2(), new Vector2()};

	private Vector2 fromCenterToFirstBlockVertex = new Vector2();
	
	
	private Vector2 firstBlockMovement = new Vector2();
	private Vector2 secondBlockMovement = new Vector2();

	private Point2 crossPoint;
	
	private Point2[] nearestVertex = new Point2[] {new Point2(), new Point2()};

	private double dblShortestTimeToCollision = 0;
	private double dblTimeToCollision = 0;
	
	public boolean isInteractionHappened(Component firstReflection, Component secondReflection) {
		
		int timeToCollision = timeToCollision((PhysicalBlock) firstReflection, (PhysicalBlock) secondReflection);
		
		if (timeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL){
			return true;
		} 
		else if (timeToCollision < IntervalPhysics.PROCESSED_INTERVAL){
			IntervalPhysics.PROCESSED_INTERVAL = timeToCollision;
		}
		
		return false;
	}
	
//	private Vector2 firstShapeMovement = new Vector2();
//	private Vector2 secondShapeMovement = new Vector2();
	
	public boolean isInteractionPossible(Component firstReflection, Component secondReflection) {
		if (super.isInteractionPossible(firstReflection, secondReflection)) {
			/*PhysicalShape firstShape = (PhysicalShape) firstReflection;
			PhysicalShape secondShape = (PhysicalShape) secondReflection;
			
			if (!firstShape.occupiedArea.isAabb || !secondShape.occupiedArea.isAabb) { 
				return true;	
			} 
			if (	(firstShape.movement.alphaRotation > 0 && !firstShape.occupiedArea.isSphere) 
					|| (secondShape.movement.alphaRotation > 0 && !secondShape.occupiedArea.isSphere)	) {
				return true;
			}
			
			double ratio = (double) DirectPhysicalInterface.PROCESSED_INTERVAL 
					/ (double) DirectPhysicalInterface.BASE_PHYSICAL_INTERVAL;
		
			firstShapeMovement.set(firstShape.movement);
			firstShapeMovement.selfMulti(firstShape.movement.currentSpeed * ratio);
			
			secondShapeMovement.set(secondShape.movement);
			secondShapeMovement.selfMulti(secondShape.movement.currentSpeed * ratio);
			return firstShape.occupiedArea.incorrupted(firstShapeMovement, secondShape.occupiedArea, secondShapeMovement);*/
			return true;
		}
		return false;
	}
	
	private int timeToCollision(PhysicalBlock firstBlock, PhysicalBlock secondBlock) {
		
		int timeToCollision = IntervalPhysics.PROCESSED_INTERVAL;
		dblTimeToCollision = timeToCollision;
		dblShortestTimeToCollision = dblTimeToCollision;
		
		double ratio = dblTimeToCollision / (double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;
		
		//massive mirror initialization
		for (int blockNo = 0; blockNo < 2; blockNo++) {
			
			firstBlockMovement.set(firstBlock.movement);
			firstBlockMovement.selfMulti(firstBlock.movement.currentSpeed.value);
			
			secondBlockMovement.set(secondBlock.movement);
			secondBlockMovement.selfMulti(secondBlock.movement.currentSpeed.value);
			
			
			double w = firstBlock.width.value / 2, h = firstBlock.height.value / 2;
			
			fromCenterToFirstBlockVertex.set(w, h); //vector from block's center to block's vertex
			
			double firstBlockAreaRadius = fromCenterToFirstBlockVertex.length2();
			
			for (int vertexNo = 0; vertexNo < 4; vertexNo++) {
				
				Point2 vertex = potencialContactVertex[blockNo * 4 + vertexNo];
				
				fromCenterToFirstBlockVertex.rotate(firstBlock.position.getAlpha());
				
				vertex.set(firstBlock.position);
				vertex.move(fromCenterToFirstBlockVertex);
				if (vertexNo % 2 == 0) h = - h;
				else w = - w;
				fromCenterToFirstBlockVertex.set(w, h);
				
				potencialContactVertexMove[blockNo * 4 + vertexNo].set(
						getVertexMovement(firstBlock, secondBlock, firstBlockMovement, secondBlockMovement, vertex, ratio));
				
				potencialContactVertexMomentalMove[blockNo * 4 + vertexNo].set(
						getVertexMomentalMovement(	firstBlock, firstBlockAreaRadius, 
													secondBlock, firstBlockMovement, secondBlockMovement, vertex, ratio));
				
				/*if (vertexNo == 0) {
					nearestVertex[blockNo].set(vertex);
				} else {
					if (nearestVertex[blockNo].dist(secondBlock.position) 
										> vertex.dist(secondBlock.position)) {
						
						nearestVertex[blockNo].set(vertex);
					}
				}*/
				
			}
			PhysicalBlock tmp = firstBlock;
			firstBlock = secondBlock;
			secondBlock = tmp;
		}
		//now potencialContactVertex[], potencialContactVertexMove[] and nearestVertex[] have been initialized
		
		/*mirror algorithm searches for collisions
		 *between (two nearest to second block borders / [23.11.14] all borders) of first block and vertexes of second block
		 *and switch blocks to do the same 
		*/
		boolean gotCollision = false;
		int iterNum = 0;
		collisionsNum = 0;
		for (int blockNo = 0; blockNo < 2; blockNo++) {
			
			/*int longestBorderNo = 0;
			int emptyBorderNo = 0;
			double longestLength = 0;
			int currentVertexNo = 0; 
			for (int vertexNo = 0; vertexNo < 4; vertexNo++) {
				
				potencialContactBorder[vertexNo].set(
						potencialContactVertex[blockNo * 4 + vertexNo], 
						nearestVertex[blockNo]);*/
				
				/*double length = potencialContactBorder[vertexNo].length2();
				if (length > 0) {
					if (longestLength == 0) {
						longestLength = length;
					} else {
						if (length > longestLength) {
							longestLength = length;
							longestBorderNo = vertexNo;
						}
					}
				} else emptyBorderNo = vertexNo;
			}*/
			
			for (int borderNo = 0; borderNo < 4; borderNo++) {
				
				potencialContactBorder[borderNo].set(
						potencialContactVertex[blockNo * 4 + (borderNo + 1) % 4], 
						potencialContactVertex[blockNo * 4 + borderNo]);
				
				//if (borderNo != longestBorderNo && borderNo != emptyBorderNo) {
				
					for (int vertexNo = 0; vertexNo < 4; vertexNo++) {
						
						/*int currentMomentalTimeToCollision = timeToCollisionBetween1stBlockBorder2ndBlockVertex(
								firstBlock, secondBlock,
								nearestVertex[blockNo], potencialContactBorder[borderNo], 
								potencialContactVertex[((blockNo + 1) % 2) * 4 + vertexNo], 
								potencialContactVertexMomentalMove[((blockNo + 1) % 2) * 4 + vertexNo], ratio);
						
						int currentTimeToCollision = timeToCollisionBetween1stBlockBorder2ndBlockVertex(
								firstBlock, secondBlock,
								nearestVertex[blockNo], potencialContactBorder[borderNo], 
								potencialContactVertex[((blockNo + 1) % 2) * 4 + vertexNo], 
								potencialContactVertexMove[((blockNo + 1) % 2) * 4 + vertexNo], ratio);*/
						int currentMomentalTimeToCollision = timeToCollisionBetween1stBlockBorder2ndBlockVertex(
								firstBlock, secondBlock,
								potencialContactVertex[blockNo * 4 + borderNo], potencialContactBorder[borderNo], 
								potencialContactVertex[((blockNo + 1) % 2) * 4 + vertexNo], 
								potencialContactVertexMomentalMove[((blockNo + 1) % 2) * 4 + vertexNo]);
						
						int currentTimeToCollision = timeToCollisionBetween1stBlockBorder2ndBlockVertex(
								firstBlock, secondBlock,
								potencialContactVertex[blockNo * 4 + borderNo], potencialContactBorder[borderNo], 
								potencialContactVertex[((blockNo + 1) % 2) * 4 + vertexNo], 
								potencialContactVertexMove[((blockNo + 1) % 2) * 4 + vertexNo]);
						if (currentMomentalTimeToCollision < currentTimeToCollision) {
							currentTimeToCollision = currentMomentalTimeToCollision;
						}
						
						if (currentTimeToCollision < timeToCollision) {
							gotCollision = true;
							iterNum++;
							
							timeToCollision = currentTimeToCollision;

							if (currentTimeToCollision < IntervalPhysics.PROCESSED_INTERVAL 
									&& currentTimeToCollision > IntervalPhysics.MINIMAL_CALCULATION_INTERVAL) {
								IntervalPhysics.PROCESSED_INTERVAL = currentTimeToCollision;
							}
						}
					}
				//}
			}
			PhysicalBlock tmp = firstBlock;
			firstBlock = secondBlock;
			secondBlock = tmp;
		}
		//if (gotCollision) Log.e("Physical", "got collision with " + iterNum + " iterations; collisions " + collisionsNum + ";");
		
		return timeToCollision;
	}
	
	private Vector2 vertexMovement = new Vector2();
	private Vector2 fromBlockToContactVertex = new Vector2();
	
	private Vector2 blockVertexAngularBase = new Vector2();
	private Vector2 blockVertexAngularMove = new Vector2();
	
	private Vector2 getVertexMomentalMovement(PhysicalShape firstBlock, double firstBlockAreaRadius,
						PhysicalShape secondBlock, Vector2 firstBlockMovement, Vector2 secondBlockMovement,
						Point2 vertex, double ratio) {
		
		fromBlockToContactVertex.set(vertex, firstBlock.position);
		
		blockVertexAngularBase.set(-fromBlockToContactVertex.y.value, fromBlockToContactVertex.x.value);
		blockVertexAngularBase.restore();
		
		//v = r * w, where w is angular speed in radian
		blockVertexAngularMove.set(blockVertexAngularBase);
		blockVertexAngularMove.selfMulti(firstBlockAreaRadius * firstBlock.movement.alphaRotation.value);
		
		vertexMovement.set(firstBlockMovement);
		vertexMovement.addict(blockVertexAngularMove);
		
		fromBlockToContactVertex.set(vertex, secondBlock.position);
		
		double secondBlockAreaRadius = fromBlockToContactVertex.length2();
		//rotate 90d
		blockVertexAngularBase.set(-fromBlockToContactVertex.y.value, fromBlockToContactVertex.x.value);
		blockVertexAngularBase.restore();
		
		//v = r * w, where w is angular speed in radian
		blockVertexAngularMove.set(blockVertexAngularBase);
		blockVertexAngularMove.selfMulti(secondBlockAreaRadius * secondBlock.movement.alphaRotation.value);
		
		vertexMovement.decrease(secondBlockMovement);
		vertexMovement.decrease(blockVertexAngularMove);
		
		vertexMovement.selfMulti(ratio);
		
		return vertexMovement;
	}

	private Vector2 fromBlockToVertexFinalPosition = new Vector2();

	private Vector2 getVertexMovement(	PhysicalShape firstBlock, PhysicalShape secondBlock, 
										Vector2 firstBlockMovement, Vector2 secondBlockMovement, 
										Point2 vertex, double ratio) {
		
		vertexMovement.set(firstBlockMovement);
		vertexMovement.decrease(secondBlockMovement);
		
		vertexMovement.selfMulti(ratio);
		
		
		fromBlockToContactVertex.set(vertex, firstBlock.position);
		
		fromBlockToVertexFinalPosition.set(fromBlockToContactVertex);
		fromBlockToVertexFinalPosition.rotate(firstBlock.movement.alphaRotation.value * ratio);
		
		blockVertexAngularMove.set(fromBlockToVertexFinalPosition);
		blockVertexAngularMove.decrease(fromBlockToContactVertex);
		
		vertexMovement.addict(blockVertexAngularMove);
		
		
		fromBlockToContactVertex.set(vertex, secondBlock.position);
		
		fromBlockToVertexFinalPosition.set(fromBlockToContactVertex);
		fromBlockToVertexFinalPosition.rotate(secondBlock.movement.alphaRotation.value * ratio);
		
		blockVertexAngularMove.set(fromBlockToVertexFinalPosition);
		blockVertexAngularMove.decrease(fromBlockToContactVertex);
		
		vertexMovement.decrease(blockVertexAngularMove);
		
		return vertexMovement; 
	}

	private Vector2 fromBorderPointToCrossPoint = new Vector2();
	private Vector2 fromVertexToCrossPoint = new Vector2();
	
	private int timeToCollisionBetween1stBlockBorder2ndBlockVertex(PhysicalShape shape1, PhysicalShape shape2, 
			Point2 borderPoint, Vector2 borderVector, Point2 vertex, Vector2 vertexMove) {
		
		crossPoint = findCrossPoint(borderPoint, borderVector, vertex, vertexMove);
		if (crossPoint != null) {
			fromBorderPointToCrossPoint.set(crossPoint, borderPoint);
			if (fromBorderPointToCrossPoint.myProectionOnVector(borderVector) > 0 && 
						fromBorderPointToCrossPoint.length2() < borderVector.length2()) {
				fromVertexToCrossPoint.set(crossPoint, vertex);
				double distanceToCollision = fromVertexToCrossPoint.length2(), movementLength = vertexMove.length2();
				if (fromVertexToCrossPoint.myProectionOnVector(vertexMove) > 0 
																		&& distanceToCollision < movementLength) {
					
					double dblCurrentTimeToCollision = dblTimeToCollision * (distanceToCollision / movementLength);
					int currentTimeToCollision = (int) dblCurrentTimeToCollision;
					
					if (dblCurrentTimeToCollision < dblShortestTimeToCollision) {
						
						dblShortestTimeToCollision = dblCurrentTimeToCollision;
						
						if (currentTimeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL) {
							
							collisionsNum++;
							
							shape1.contactPoint.set(vertex);
							shape2.contactPoint.set(crossPoint);
							shape1.contactPoint.commit(masterEngine, shape1.entity);
							shape2.contactPoint.commit(masterEngine, shape2.entity);
						} 
					}
					return currentTimeToCollision;
					
				}
			}
		}
		return IntervalPhysics.PROCESSED_INTERVAL + 2;
	}

	private Point2 calulatedCrossPoint = new Point2();
	
	private Point2 findCrossPoint(Point2 borderPoint, Vector2 borderVector,
			Point2 vertex, Vector2 vertexMove) {
		double K1, C1, K2, C2, x, y;
		if (borderVector.x.value == 0) {
			if (vertexMove.x.value == 0) {
				x = 0;
				y = 0;
				return null;
			} else {
				K2 = vertexMove.y.value / vertexMove.x.value; 
				C2 = vertex.y.value - K2 * vertex.x.value;
				y = K2 * borderPoint.x.value + C2;
				x = (y - C2) / K2;
			}
		} else if (borderVector.y.value == 0) {
			if (vertexMove.x.value <= 0.000000000001 && vertexMove.x.value >= -0.000000000001) {
				x = vertex.x.value;
				y = borderPoint.y.value;
			} else {
				K2 = vertexMove.y.value / vertexMove.x.value; 
				C2 = vertex.y.value - K2 * vertex.x.value;
				x = (borderPoint.y.value - C2) / K2;
				y = K2 * x + C2;
			}
		} else {
			if (vertexMove.x.value <= 0.000000000001 && vertexMove.x.value >= -0.000000000001) {
				K2 = borderVector.y.value / borderVector.x.value; 
				C2 = borderPoint.y.value - K2 * borderPoint.x.value;
				x = vertex.x.value;
				y = K2 * x + C2;
			} else {
				K1 = borderVector.y.value / borderVector.x.value; 
				C1 = borderPoint.y.value - K1 * borderPoint.x.value;
				K2 = vertexMove.y.value / vertexMove.x.value; 
				C2 = vertex.y.value - K2 * vertex.x.value;
				if (K1 != K2) {
					x = (C2 - C1) / (K1 - K2);
					y = K1 * x + C1;
				} else {
					x = 0; y = 0;
					return null;
				}
			}
		}
		calulatedCrossPoint.set(x, y);
		return calulatedCrossPoint;
	}
	
}
