package ru.framework2d.physics.collisions.particularblock;

import ru.framework2d.core.Component;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.InteractionShapes;
import ru.framework2d.physics.collisions.blocks.PhysicalBlock;
import ru.framework2d.physics.collisions.particular.PhysicalParticular;

public class InteractionParticularBlock extends InteractionShapes {
	
	public InteractionParticularBlock() {
		super(2, (Class <Component>) PhysicalParticular.class.asSubclass(Component.class), 
				(Class <Component>) PhysicalBlock.class.asSubclass(Component.class));
		
		name = this.getClass().getSimpleName();
	}
	
	@Override
	public boolean toHandleInteraction(Component firstReflection,
			Component secondReflection) {
		PhysicalParticular particular = (PhysicalParticular) firstReflection;
		PhysicalBlock block = (PhysicalBlock) secondReflection;
		int timeToCollision = timeToCollision(particular, block);
		if (timeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL) {
			return true;
		} 
		else if (timeToCollision < IntervalPhysics.PROCESSED_INTERVAL) {
			IntervalPhysics.PROCESSED_INTERVAL=timeToCollision;
		}
		return false;
	}
	
	private int timeToCollision(PhysicalParticular particular, PhysicalBlock block) {
		int timeToCollision = IntervalPhysics.PROCESSED_INTERVAL;
		
		double ratio = (double)timeToCollision / 
						(double)IntervalPhysics.BASE_PHYSICAL_INTERVAL;

		/*Vector2 directToBlock = new Vector3(block.position, particular.position);
    	directToBlock.restore();
		ArrayList <Point2f> potencialContactPoint = new ArrayList<Point2f>();
		double angle=block.position.getAlpha() / 57.3f;
		Vector2 vToPoint=new Vector2(block.width / 2, block.height / 2);
		double blockAreaRadius=vToPoint.length2();
		for (int i = 0; i < 2; i++) {
			vToPoint.rotate(angle);
			if (vToPoint.myProectionOnVector(directToBlock)<0){
				potencialContactPoint.add(new Point2f(block.position.getXf()+(float)vToPoint.getX(), block.position.getYf()+(float)vToPoint.getY()));
			}else {
				potencialContactPoint.add(new Point2f(block.position.getXf()-(float)vToPoint.getX(), block.position.getYf()-(float)vToPoint.getY()));
			}
			vToPoint.set(block.width/2, -block.height/2);
		} 
		if (potencialContactPoint.get(0).dist(particular.position)>potencialContactPoint.get(1).dist(particular.position)){
			Point2f tmp= new Point2f(potencialContactPoint.get(0));
			potencialContactPoint.get(0).set(potencialContactPoint.get(1));
			potencialContactPoint.get(1).set(tmp);
		}			
		Vector2 fromFirstToSecondPoint= new Vector2(potencialContactPoint.get(1), potencialContactPoint.get(0));
		Vector2 fromFirstPointToCircle= new Vector2(particular.position.toPos3f(), potencialContactPoint.get(0));
		if (fromFirstToSecondPoint.myProectionOnVector(fromFirstPointToCircle)<0){
			vToPoint=new Vector2(potencialContactPoint.get(1), block.position.getNew3f());
			vToPoint.set(-vToPoint.getX(), -vToPoint.getY());
			potencialContactPoint.get(1).set(block.position.getXf()+(float)vToPoint.getX(), block.position.getYf()+(float)vToPoint.getY());
		}
		fromFirstToSecondPoint.set(potencialContactPoint.get(1), potencialContactPoint.get(0));
		
		Vector2 particularMove = particular.movement.vMove().minus(block.movement.vMove());
		Vector2 circleAngularBase = new Vector2(-directToBlock.getY(), directToBlock.getX());
		double distance=particular.position.dist(block.position);
		particularMove.set(particularMove.plus(circleAngularBase.multi(distance*block.movement.alphaRotation)));
		particularMove.set(particularMove.multi(ratio));
		boolean gotCrossPoint=true;
		double K1, C1, K2, C2, x, y;
		if (fromFirstToSecondPoint.getX()==0){
			K2=particularMove.getY()/particularMove.getX(); //
			C2=particular.position.getY()-K2*particular.position.getX();
			y=K2*potencialContactPoint.get(0).getX()+C2;
			x=(y-C2)/K2;
		}else if (fromFirstToSecondPoint.getY()==0){
			K2=particularMove.getY()/particularMove.getX(); 
			C2=particular.position.getY()-K2*particular.position.getX();
			x=(potencialContactPoint.get(0).getY()-C2)/K2;
			y=K2*x+C2;
		} else {
			K1=fromFirstToSecondPoint.getY()/fromFirstToSecondPoint.getX(); 
			C1=potencialContactPoint.get(0).getY()-K1*potencialContactPoint.get(0).getX(); 
			K2=particularMove.getY()/particularMove.getX(); 
			C2=particular.position.getY()-K2*particular.position.getX();
			if (K1!=K2){
				x=(C2-C1)/(K1-K2);
				y=K1*x+C1;
			}else {
				gotCrossPoint=false;
				x=0; y=0;
			}
		}
		if (gotCrossPoint){
			Point2f crossPoint = new Point2f((float)x, (float)y);Vector2 fromFirstPointToCrossPoint= new Vector2(crossPoint, potencialContactPoint.get(0));
			if (fromFirstPointToCrossPoint.myProectionOnVector(fromFirstToSecondPoint)>0){
				if (fromFirstPointToCrossPoint.length2()<fromFirstToSecondPoint.length2()){
					double distanceFromCrossToParticular=crossPoint.dist(particular.position);
					if (distanceFromCrossToParticular<particularMove.length2()){
						timeToCollision=(int)((double)timeToCollision*(distanceFromCrossToParticular/particularMove.length2()));
						if (timeToCollision<=DirectPhysicalInterface.MINIMAL_CALCULATION_INTERVAL){
							Vector2 normal = new Vector2(fromFirstToSecondPoint);
							normal.restore();
							normal.set(-normal.getY(), normal.getX());
							if (normal.myProectionOnVector(particularMove)>0) {
								normal.set(-normal.getX(), -normal.getY());	
							}
							particular.hitNormal.set(normal);
							particular.target.set(crossPoint);
							particular.hited.value=true;
							particular.tp.visibility.value=true;
							particular.tp.visibility.commit(null, particular.tp.pntrMaster);
							particular.movement.stop();
							particular.movement.commit(null, particular.pntrMaster);
							particular.tp.normal.set(particular.hitNormal);
							particular.tp.position.setPoint(particular.target);
							particular.tp.position.alpha=particular.tp.normal.angle();
							particular.tp.movement.stop();
							particular.tp.movement.commit(null, particular.tp.pntrMaster);
							particular.tp.position.commit(null, particular.tp.pntrMaster);
							return timeToCollision;
						}else if (timeToCollision<DirectPhysicalInterface.PROCESSED_INTERVAL) {
							DirectPhysicalInterface.PROCESSED_INTERVAL=timeToCollision;
						}
					}
				}
			}
		}		*/
		
		return IntervalPhysics.PROCESSED_INTERVAL + 2;
	}

	@Override
	public boolean isInteractionPossible(Component firstComponent, Component secondComponent) {
		return false;
	}

	@Override
	public boolean isInteractionHappened(Component firstComponent, Component secondComponent) {
		return false;
	}
}

