package ru.framework2d.physics.collisions.circles;

import ru.framework2d.core.Component;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector2;
import ru.framework2d.physics.IntervalPhysics;
import ru.framework2d.physics.collisions.InteractionShapes;

public class InteractionCircles extends InteractionShapes {
	
	public InteractionCircles() {
		super(2, (Class <Component>) PhysicalCircle.class.asSubclass(Component.class), 
				(Class <Component>) PhysicalCircle.class.asSubclass(Component.class));
		
		PhysicalCircle.BROADPHASE_HANDLER = this;
		PhysicalCircle.setInteractionHandler((Class <Component>) PhysicalCircle.class.asSubclass(Component.class), this);
	}
	
	private Vector2 fromFirstToSecond = new Vector2();

	@Override
	public boolean isInteractionHappened(Component firstReflection, Component secondReflection) {
		PhysicalCircle firstCircle = (PhysicalCircle) firstReflection;
		PhysicalCircle secondCircle = (PhysicalCircle) secondReflection;
		
		int timeToCollision = timeToCollision(firstCircle, secondCircle);
		
		if (timeToCollision <= IntervalPhysics.MINIMAL_CALCULATION_INTERVAL){
			
			fromFirstToSecond.set(secondCircle.position, firstCircle.position);
	    	fromFirstToSecond.restore();
	    	
			firstCircle.contactPoint.set(
					firstCircle.position.x.value + fromFirstToSecond.x.value * firstCircle.radius.value, 
					firstCircle.position.y.value + fromFirstToSecond.y.value * firstCircle.radius.value);
			
			secondCircle.contactPoint.set(
					secondCircle.position.x.value - fromFirstToSecond.x.value * secondCircle.radius.value, 
					secondCircle.position.y.value - fromFirstToSecond.y.value * secondCircle.radius.value);
			
			firstCircle.contactPoint.commit(masterEngine, firstCircle.entity);
			secondCircle.contactPoint.commit(masterEngine, secondCircle.entity);
			
			return true;
			
		}
		else if (timeToCollision < IntervalPhysics.PROCESSED_INTERVAL){
			IntervalPhysics.PROCESSED_INTERVAL = timeToCollision;
		}
		return false;
	}
	
	private Vector2 relativeMovement = new Vector2();
	private Vector2 toSecond = new Vector2();
	private Vector2 secondShapeMovement = new Vector2();
	private Vector2 proectionRelativeMovement = new Vector2();
	
	private Point2 p2proectionOnRelativeMvmnt = new Point2();
	private Point2 p1FinalPosition = new Point2();
	
	private int timeToCollision(PhysicalCircle firstCircle, PhysicalCircle secondCircle) {
		
		int interval = IntervalPhysics.PROCESSED_INTERVAL;
		
		double ratio = (double) interval / (double) IntervalPhysics.BASE_PHYSICAL_INTERVAL;
		double speed1 = firstCircle.movement.currentSpeed.value * ratio;
		double speed2 = secondCircle.movement.currentSpeed.value * ratio;
		
		double x1 = firstCircle.position.x.value, y1 = firstCircle.position.y.value;
		double x2 = secondCircle.position.x.value, y2 = secondCircle.position.y.value;
		
		double distance = (x1 - x2)	* (x1 - x2) + (y1 - y2) * (y1 - y2);
		
		double radiusSum = firstCircle.occupiedArea.radius + secondCircle.occupiedArea.radius;

		double range = radiusSum + speed1 + speed2;
		if (distance < range * range) {
			
			if (distance < radiusSum * radiusSum) {
				return interval;
			}

			//если хотя бы 1 тело подвижно
			//if (speed1 > 0 || speed2 > 0) { //no reason to check cz one of them is active
			
			 //вектор относительного движения, единичный на протяжении использования
			double relativeSpeed;// относительная скорость
			//делаем второе тело неподвижным засчет принципа относительности
			secondShapeMovement.set(secondCircle.movement);
			secondShapeMovement.selfMulti(speed2);
			relativeMovement.set(firstCircle.movement);
			relativeMovement.selfMulti(speed1);
			relativeMovement.decrease(secondShapeMovement);
			relativeSpeed = relativeMovement.restore(); //теперь считаем скорость второго тела равной нулю 
			//а скорость первого тела и его направление движение это relativeSpeed и sum соответственно 

			//toSecond - направлен от первого тела ко второму, не единичный
			toSecond.set(secondCircle.position, firstCircle.position);

			double proectionTo2ndOnRltvMvmnt = toSecond.myProectionOnVector(relativeMovement);
			//если первое тело движется в направлении второго(которое неподвижно), широкая фаза второго уровня
			if (proectionTo2ndOnRltvMvmnt > 0) {
				//то столкновение возможно
				//p2proectionOnSum - проекция положения второго тела на вектор отн. движения
				proectionRelativeMovement.set(relativeMovement);
				proectionRelativeMovement.selfMulti(proectionTo2ndOnRltvMvmnt);
				p2proectionOnRelativeMvmnt.set(firstCircle.position);
				p2proectionOnRelativeMvmnt.move(proectionRelativeMovement);
				
				//если второе тело находится достаточно близко к траектории, узкая фаза
				if (secondCircle.position.dist(p2proectionOnRelativeMvmnt) < radiusSum) {
					//тела наверняка столкнулись, однако есть исключения
					
					//p1newpositionition - новое положение р1 при относительном движении
					p1FinalPosition.set(x1, y1);
					p1FinalPosition.move(relativeMovement, relativeSpeed);
					//если проекция принадлежит траектории движения или столкнулись в конечной точке, узкая фаза последнего уровня 
					if (firstCircle.position.dist(p2proectionOnRelativeMvmnt) + p1FinalPosition.dist(p2proectionOnRelativeMvmnt) 
							<= firstCircle.position.dist(p1FinalPosition) + 0.00001 
							|| secondCircle.position.dist(p1FinalPosition) < radiusSum) {
						//то однозначно столкнулись, фаза поиска столкновений закончена

						//поиск момента времени столкновения
						//{X1(t)=Kx1(t)+Xo1; Y1(t)=Ky1(t)+Yo1;} {X2(t)=Kx2(t)+Xo2; Y2(t)=Ky2(t)+Yo2;} - уравнения движения первого и второго тел
						//функция расстояние между телами: d(t)=sqrt((X2(t)-X1(t))*(X2(t)-X1(t))+(Y2(t)-Y1(t))*(Y2(t)-Y1(t)))=r1+r2
						//выполнив все преобразования: t1,2=-b+-sqrt(D)/2K где меньшее положительное t и есть момент времени столкновения

						//K1=Kx2-Kx1; K2=Ky2-Ky1; Xo=Xo2-Xo1; Yo=Yo2-Yo1; r1- радиус первого тела; r2 - радиус второго тела
						double 	K1 = secondCircle.movement.x.value * (speed2) / interval - firstCircle.movement.x.value * (speed1) / interval, 
								K2 = secondCircle.movement.y.value * (speed2) / interval - firstCircle.movement.y.value * (speed1) / interval,
								Xo = x2 - x1, Yo = y2 - y1;
						double b = 2 * (K1 * Xo + K2 * Yo), K = K1 * K1 + K2 * K2;
						double D = b * b - 4 * K * (Xo * Xo + Yo * Yo -	radiusSum * radiusSum);
						if (D > 0) { // если дискриминант больше нуля (должен быть больше во всех случаях, кроме равенства нулю)
							double timeToCollision = ( -b - Math.sqrt(D)) / (2 * K);
							if (timeToCollision >= 0) { 
								// V---РАЗРЕШЕНИЕ СТОЛКНОВЕНИЙ---V
								return (int) timeToCollision;
								// ^---РАЗРЕШЕНИЕ СТОЛКНОВЕНИЙ---^
							}
							else {//mt=(-b+Math.sqrt(D))/(2 * K);
								timeToCollision = -timeToCollision - b / K;
							}
							
							if (timeToCollision >= 0) 
								//V---РАЗРЕШЕНИЕ СТОЛКНОВЕНИЙ---V
								return (int) timeToCollision;
								//^---РАЗРЕШЕНИЕ СТОЛКНОВЕНИЙ---^
						}
						return interval + 2; //равенство нулю означет столкновение по касательной	
					}
					//иначе не столкнулись
				}		
			}
			
		}
		return interval + 2;	
	}
}
