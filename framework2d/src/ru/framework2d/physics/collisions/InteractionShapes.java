package ru.framework2d.physics.collisions;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.core.InteractionHandler;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.blocks.PhysicalBlock;

public class InteractionShapes extends ComponentsHandler implements InteractionHandler {
	
	public InteractionShapes() {
		super(2, (Class <Component>) PhysicalShape.class.asSubclass(Component.class), 
				(Class <Component>) PhysicalShape.class.asSubclass(Component.class));
	}
	
	public InteractionShapes(int processingNum, Class <Component> firstReflectionClass, 
						Class <Component> secondReflectionClass) {
		
		super(processingNum, firstReflectionClass, secondReflectionClass);
	}
	
	public boolean isInteractionPossible(Component firstReflection, Component secondReflection) {
		PhysicalShape firstShape = (PhysicalShape) firstReflection;
		PhysicalShape secondShape = (PhysicalShape) secondReflection;
		
		double ratio = (double) IntervalPhysics.PROCESSED_INTERVAL 
					/ (double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;
		
		double dx = firstShape.position.x.value - secondShape.position.x.value,
				dy = firstShape.position.y.value - secondShape.position.y.value;
		
		double distance = dx * dx + dy * dy;
		
		//fromFirstToSecondShape.set(secondShape.position, firstShape.position);
		//fromFirstToSecondShape.restore();
		
		double range = firstShape.occupiedArea.radius + secondShape.occupiedArea.radius 
					//+ fromFirstToSecondShape.myIdentityProectionOnVector(firstShape.movement) *
					+ firstShape.movement.currentSpeed.value * ratio 
					//- fromFirstToSecondShape.myIdentityProectionOnVector(secondShape.movement) *
					+ secondShape.movement.currentSpeed.value * ratio;
		
		return (distance < range * range);
	}
	
	private Vector2 firstShapeMovement = new Vector2();
	private Vector2 secondShapeMovement = new Vector2();	
	
	private Vector2 contactNormal = new Vector2();
	
	private Vector2 toFirstShapeContactPoint = new Vector2();
	private Vector2 toSecondShapeContactPoint = new Vector2();
	
	private Vector2 angularFirstShapeContactPointBase = new Vector2();//вектор базы скорости вращения по часовой 
	private Vector2 angularSecondShapeContactPointBase = new Vector2();
	
	private Vector2 angularFirstShapeContactPointMove = new Vector2();		//мгновенное поступательное движение вращающейся точки
	private Vector2 angularSecondShapeContactPointMove = new Vector2(); //v=w*r
	
	private Vector2 oldFirstShapeContactPointMove = new Vector2();		//вектор суммарной скорости точки контакта
	private Vector2 oldSecondShapeContactPointMove = new Vector2();	
	
	private Vector2 firstShapeContactPointMove = new Vector2();		
	private Vector2 secondShapeContactPointMove = new Vector2();
	
	private Vector2 newImpulse1 = new Vector2();
	private Vector2 newImpulse2 = new Vector2();
	
	public boolean toHandleInteraction(Component firstReflection, Component secondReflection) {
		
		PhysicalShape firstShape = (PhysicalShape) firstReflection;
		PhysicalShape secondShape = (PhysicalShape) secondReflection;
		
		firstShapeMovement.set(firstShape.movement);
    	firstShapeMovement.selfMulti(firstShape.movement.currentSpeed.value);
    	
    	secondShapeMovement.set(secondShape.movement);
    	secondShapeMovement.selfMulti(secondShape.movement.currentSpeed.value);
		
    	toFirstShapeContactPoint.set(firstShape.contactPoint, firstShape.position);
    	double firstShapeContactRadius = toFirstShapeContactPoint.restore(); //returns length
    	
    	toSecondShapeContactPoint.set(secondShape.contactPoint, secondShape.position);
    	double secondShapeContactRadius = toSecondShapeContactPoint.restore();

    	angularFirstShapeContactPointBase.set( -toFirstShapeContactPoint.getY(), 
    											toFirstShapeContactPoint.getX()); 
    	angularSecondShapeContactPointBase.set(	-toSecondShapeContactPoint.getY(), 
    											toSecondShapeContactPoint.getX());
    	
    	double rotateProportion = 
    			Math.PI * Math.PI *
    			(IntervalPhysics.PHYSICAL_UNIT * IntervalPhysics.PHYSICAL_UNIT);
    	
    	/*double oldFirstShapeSpeed = firstShapeMovement.myProectionOnVector(angularFirstShapeContactPointBase);
    	
    	double oldSecondShapeSpeed = secondShapeMovement.myProectionOnVector(angularSecondShapeContactPointBase);
    	
    	double oldFirstShapeAngularSpeed = firstShape.movement.alphaRotation;
    	
    	double oldSecondShapeAngularSpeed = secondShape.movement.alphaRotation;*/
    	
    	/*double vx1 = firstShapeMovement.x.value, vy1 = firstShapeMovement.y.value;
    	double vx2 = secondShapeMovement.x.value, vy2 = secondShapeMovement.y.value;
    	
    	double w1 = firstShape.movement.alphaRotation;
    	double w2 = secondShape.movement.alphaRotation;
    	
    	double firstInverseM = 1 / firstShape.weight.value; 
    	double secondInverseM = 1 / secondShape.weight.value;
    	
    	double firstInverseI = firstShape.weight.value * firstShape.occupiedArea.radius * firstShape.occupiedArea.radius / (2 * rotateProportion); 
    	double secondInverseI = secondShape.weight.value * secondShape.occupiedArea.radius * secondShape.occupiedArea.radius / (2 * rotateProportion);

    	firstInverseI = 1 / firstInverseI;
    	secondInverseI = 1 / secondInverseI;
    	
    	contactNormal.set(firstShape.position, secondShape.position);
    	contactNormal.restore();
    	
    	double R1 = (contactNormal.y.value * toFirstShapeContactPoint.x.value - contactNormal.x.value * toFirstShapeContactPoint.y.value) * firstInverseI;
    	double R2 = (contactNormal.y.value * toSecondShapeContactPoint.x.value - contactNormal.x.value * toSecondShapeContactPoint.y.value) * secondInverseI;
    	
    	double J = contactNormal.x.value * (contactNormal.x.value * firstInverseM - toFirstShapeContactPoint.y.value * R1 
    										+ contactNormal.x.value * secondInverseM + toSecondShapeContactPoint.y.value * R2) 
    				+ contactNormal.y.value * (contactNormal.y.value * firstInverseM + toFirstShapeContactPoint.x.value * R1 * firstInverseI 
											+ contactNormal.y.value * secondInverseM - toSecondShapeContactPoint.x.value * R2 * secondInverseM);
    	
    	double relativeSpeed = contactNormal.x.value * (vx1 - w1 * toFirstShapeContactPoint.y.value - vx2  + w2 * toSecondShapeContactPoint.y.value) 
    							+ contactNormal.y.value * (vy1 + w1 * toFirstShapeContactPoint.x.value - vy2  - w2 * toSecondShapeContactPoint.x.value);
    	
    	double p = relativeSpeed / J;*/
    	
    	double firstM = firstShape.weight.value; 
    	double secondM = secondShape.weight.value;
    	
    	double firstI = firstM * firstShape.occupiedArea.radius * firstShape.occupiedArea.radius / (2 * rotateProportion); 
    	double secondI = secondM * secondShape.occupiedArea.radius * secondShape.occupiedArea.radius / (2 * rotateProportion);

    	if (firstShape instanceof PhysicalBlock) {
    		PhysicalBlock block = (PhysicalBlock) firstShape;
    		firstI = firstM * (block.height.value * block.height.value + block.width.value * block.width.value) / (12.0d * rotateProportion);  
    	} 
    	
    	if (secondShape instanceof PhysicalBlock) {
    		PhysicalBlock block = (PhysicalBlock) secondShape;
    		secondI = secondM * (block.height.value * block.height.value + block.width.value * block.width.value) / (12.0d * rotateProportion);  
    	} 
    	
    	double firstKK1 = firstM - firstI;	
    	double firstKK2 = firstM + firstI;
    	double secondKK1 = secondM - secondI;	
    	double secondKK2 = secondM + secondI;
    	
    	/*double newFirstShapeSpeed = oldFirstShapeSpeed * (firstKK1 / firstKK2) + oldFirstShapeAngularSpeed * 2 * firstI / firstKK2;
    	double newFirstShapeAngularSpeed = oldFirstShapeSpeed * 2 * firstShape.weight.value / firstKK2 
    										- oldFirstShapeAngularSpeed * (firstKK1 / firstKK2); 
    	
    	double newSecondShapeSpeed = oldSecondShapeSpeed * (secondKK1 / secondKK2) + oldSecondShapeAngularSpeed * 2 * secondI / secondKK2;
    	double newSecondShapeAngularSpeed = oldSecondShapeSpeed * 2 * secondShape.weight.value / secondKK2 
    										- oldSecondShapeAngularSpeed * (secondKK1 / secondKK2);*/
    	
    	angularFirstShapeContactPointMove.set(angularFirstShapeContactPointBase);
    	angularFirstShapeContactPointMove.selfMulti(firstShapeContactRadius 
    												* firstShape.movement.alphaRotation.value);
    	
    	angularSecondShapeContactPointMove.set(angularSecondShapeContactPointBase);
    	angularSecondShapeContactPointMove.selfMulti(secondShapeContactRadius 
    												* secondShape.movement.alphaRotation.value);
    	
    	oldFirstShapeContactPointMove.set(firstShapeMovement);		//вектор суммарной скорости точки контакта
    	oldFirstShapeContactPointMove.addict(angularFirstShapeContactPointMove);
    	
    	oldSecondShapeContactPointMove.set(secondShapeMovement);		
    	oldSecondShapeContactPointMove.addict(angularSecondShapeContactPointMove);

    	double K1 = firstShape.weight.value - secondShape.weight.value;	//v1'=(m1-m2)*v1/(m1+m2)+2*m2*v2/(m1+m2)
    	double K2 = firstShape.weight.value + secondShape.weight.value;	//v2'=(m2-m1)*v2/(m1+m2)+2*m1*v1/(m1+m2)
    	
    	if (secondShape.isStatic.value) {
    		firstShapeContactPointMove.set(oldFirstShapeContactPointMove);
    		firstShapeContactPointMove.turnBack();
    	} else {
	    	newImpulse1.set(oldSecondShapeContactPointMove);
	    	newImpulse1.selfMulti(2 * secondShape.weight.value / K2);
	    	newImpulse2.set(oldFirstShapeContactPointMove);
	    	newImpulse2.selfMulti(K1 / K2);
	    	
	    	firstShapeContactPointMove.set(newImpulse1);
	    	firstShapeContactPointMove.addict(newImpulse2);
    	}
    	
    	if (firstShape.isStatic.value) {
    		secondShapeContactPointMove.set(oldFirstShapeContactPointMove);
    		secondShapeContactPointMove.turnBack();
    	} else {
	    	newImpulse1.set(oldFirstShapeContactPointMove);
	    	newImpulse1.selfMulti(2 * firstShape.weight.value / K2);
	    	newImpulse2.set(oldSecondShapeContactPointMove);
	    	newImpulse2.selfMulti(-K1 / K2);
	    	
	    	secondShapeContactPointMove.set(newImpulse1);
	    	secondShapeContactPointMove.addict(newImpulse2);
    	}
    	firstShapeContactPointMove.decrease(oldFirstShapeContactPointMove);
    	secondShapeContactPointMove.decrease(oldSecondShapeContactPointMove);
    	
    	if (!firstShape.isStatic.value) {
    		
    		firstShape.movement.set(firstShapeMovement);
    		
    		//Vector2 tmp = new Vector2(firstShapeContactPointMove);
    		/*firstShape.movement.addict(firstShapeContactPointMove.selfMulti(-firstKK1 / firstKK2));
    		firstShape.movement.alphaRotation += tmp.selfMulti(2 * firstShape.weight.value / firstKK2).myProectionOnVector(
					angularFirstShapeContactPointBase);*/
    		double v0 = firstShapeContactPointMove.myProectionOnVector(toFirstShapeContactPoint);
    		double w0 = firstShapeContactPointMove.myProectionOnVector(angularFirstShapeContactPointBase) / firstShapeContactRadius;
    		firstShapeContactPointMove.restore();
    		firstShape.movement.decrease(toFirstShapeContactPoint.selfMulti(
    				w0 * firstShapeContactPointMove.myProectionOnVector(toFirstShapeContactPoint) 
    				* 2 * firstI / firstKK2 
    				+ v0 * firstKK1 / firstKK2));
    		firstShape.movement.alphaRotation.value += v0 
    				* firstShapeContactPointMove.myProectionOnVector(angularFirstShapeContactPointBase) 
    				* 2 * firstShape.weight.value / firstKK2 
    				- w0 * firstKK1 / firstKK2;
    		/*firstShape.movement.decrease(toFirstShapeContactPoint.selfMulti(w0 * 2 * firstI / firstKK2 + v0 * firstKK1 / firstKK2));
    		firstShape.movement.alphaRotation += v0 * 2 * firstShape.weight.value / firstKK2 - w0 * firstKK1 / firstKK2;*/
    		
    		/*firstShapeContactPointMove.set(contactNormal);
    		firstShape.movement.addict(	firstShapeContactPointMove.selfMulti(-p * firstInverseM));
    		firstShape.movement.alphaRotation += (contactNormal.y.value * toFirstShapeContactPoint.x.value 
    											- contactNormal.x.value * toFirstShapeContactPoint.y.value) * -p * firstInverseI;*/
    		
    		/*firstShape.movement.addict(	toFirstShapeContactPoint.selfMulti(
    									firstShapeContactPointMove.myProectionOnVector(
    									toFirstShapeContactPoint)));
	    	
	    	firstShape.movement.alphaRotation += 	firstShapeContactPointMove.myProectionOnVector(
	    											angularFirstShapeContactPointBase) 
	    											/ firstShapeContactRadius;*/		//извлекаем вращательную скорость
	    	
	    	firstShape.movement.currentSpeed.value = firstShape.movement.restore();
	    	
	    	/*if (secondShape.isStatic.value) {
	    		firstShape.movement.alphaRotation = -newFirstShapeAngularSpeed;
	    		toFirstShapeContactPoint.set(firstShape.contactPoint, firstShape.position);
	    		toFirstShapeContactPoint.restore();
	    		firstShape.movement.set(toFirstShapeContactPoint.selfMulti(-firstShapeMovement.myProectionOnVector(toFirstShapeContactPoint)));
	    		firstShape.movement.addict(angularFirstShapeContactPointBase.selfMulti(-newFirstShapeSpeed));
	    		firstShape.movement.currentSpeed = firstShape.movement.restore();
	    	}*/
	    	
	    	firstShape.movement.commit(masterEngine, firstShape.entity);
    	}
    	
    	if (!secondShape.isStatic.value) {
    		secondShape.movement.set(secondShapeMovement);
    		
    		//Vector2 tmp = new Vector2(secondShapeContactPointMove);
    		/*secondShape.movement.addict(secondShapeContactPointMove.selfMulti(-secondKK1 / secondKK2));
    		secondShape.movement.alphaRotation += tmp.selfMulti(2 * secondShape.weight.value / secondKK2).myProectionOnVector(
					angularSecondShapeContactPointBase);*/
    		double v0 = secondShapeContactPointMove.myProectionOnVector(toSecondShapeContactPoint);
    		double w0 = secondShapeContactPointMove.myProectionOnVector(angularSecondShapeContactPointBase) / secondShapeContactRadius;
    		secondShapeContactPointMove.restore();
    		secondShape.movement.decrease(toSecondShapeContactPoint.selfMulti(w0 * secondShapeContactPointMove.myProectionOnVector(toSecondShapeContactPoint) * 2 * secondI / secondKK2 + v0 * secondKK1 / secondKK2));
    		secondShape.movement.alphaRotation.value += v0 * secondShapeContactPointMove.myProectionOnVector(angularSecondShapeContactPointBase) * 2 * secondShape.weight.value / secondKK2 - w0 * secondKK1 / secondKK2;
    		/*Log.e("Physical", "yo! " + (v0 * 2 * secondShape.weight.value / secondKK2) + "; instead of " + (w0 * secondKK1 / secondKK2));*/

    		/*secondShapeContactPointMove.set(contactNormal);
    		secondShape.movement.addict(secondShapeContactPointMove.selfMulti(p * secondInverseM));
    		secondShape.movement.alphaRotation += (contactNormal.y.value * toSecondShapeContactPoint.x.value 
    											- contactNormal.x.value * toSecondShapeContactPoint.y.value) * p * secondInverseI;*/
    		
    		/*secondShape.movement.addict(toSecondShapeContactPoint.selfMulti(
    									secondShapeContactPointMove.myProectionOnVector(
    									toSecondShapeContactPoint)));
	    	secondShape.movement.alphaRotation += 	secondShapeContactPointMove.myProectionOnVector(
	    											angularSecondShapeContactPointBase) 
	    											/ secondShapeContactRadius;*/
	    	secondShape.movement.currentSpeed.value = secondShape.movement.restore();
	    	
	    	/*if (firstShape.isStatic.value) {
	    		secondShape.movement.alphaRotation = -newSecondShapeAngularSpeed;
	    		toSecondShapeContactPoint.set(secondShape.contactPoint, secondShape.position);
	    		toSecondShapeContactPoint.restore();
	    		secondShape.movement.set(toSecondShapeContactPoint.selfMulti(-secondShapeMovement.myProectionOnVector(toSecondShapeContactPoint)));
	    		secondShape.movement.addict(angularSecondShapeContactPointBase.selfMulti(-newSecondShapeSpeed));
	    		secondShape.movement.currentSpeed = secondShape.movement.restore();
	    	}*/
	    	
	    	secondShape.movement.commit(masterEngine, secondShape.entity);
    	}
    	return true;
		
	}

	public boolean isInteractionHappened(Component firstComponent,
			Component secondComponent) {
		// TODO Auto-generated method stub
		return false;
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
