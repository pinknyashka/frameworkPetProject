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

			//���� ���� �� 1 ���� ��������
			//if (speed1 > 0 || speed2 > 0) { //no reason to check cz one of them is active
			
			 //������ �������������� ��������, ��������� �� ���������� �������������
			double relativeSpeed;// ������������� ��������
			//������ ������ ���� ����������� ������ �������� ���������������
			secondShapeMovement.set(secondCircle.movement);
			secondShapeMovement.selfMulti(speed2);
			relativeMovement.set(firstCircle.movement);
			relativeMovement.selfMulti(speed1);
			relativeMovement.decrease(secondShapeMovement);
			relativeSpeed = relativeMovement.restore(); //������ ������� �������� ������� ���� ������ ���� 
			//� �������� ������� ���� � ��� ����������� �������� ��� relativeSpeed � sum �������������� 

			//toSecond - ��������� �� ������� ���� �� �������, �� ���������
			toSecond.set(secondCircle.position, firstCircle.position);

			double proectionTo2ndOnRltvMvmnt = toSecond.myProectionOnVector(relativeMovement);
			//���� ������ ���� �������� � ����������� �������(������� ����������), ������� ���� ������� ������
			if (proectionTo2ndOnRltvMvmnt > 0) {
				//�� ������������ ��������
				//p2proectionOnSum - �������� ��������� ������� ���� �� ������ ���. ��������
				proectionRelativeMovement.set(relativeMovement);
				proectionRelativeMovement.selfMulti(proectionTo2ndOnRltvMvmnt);
				p2proectionOnRelativeMvmnt.set(firstCircle.position);
				p2proectionOnRelativeMvmnt.move(proectionRelativeMovement);
				
				//���� ������ ���� ��������� ���������� ������ � ����������, ����� ����
				if (secondCircle.position.dist(p2proectionOnRelativeMvmnt) < radiusSum) {
					//���� ��������� �����������, ������ ���� ����������
					
					//p1newpositionition - ����� ��������� �1 ��� ������������� ��������
					p1FinalPosition.set(x1, y1);
					p1FinalPosition.move(relativeMovement, relativeSpeed);
					//���� �������� ����������� ���������� �������� ��� ����������� � �������� �����, ����� ���� ���������� ������ 
					if (firstCircle.position.dist(p2proectionOnRelativeMvmnt) + p1FinalPosition.dist(p2proectionOnRelativeMvmnt) 
							<= firstCircle.position.dist(p1FinalPosition) + 0.00001 
							|| secondCircle.position.dist(p1FinalPosition) < radiusSum) {
						//�� ���������� �����������, ���� ������ ������������ ���������

						//����� ������� ������� ������������
						//{X1(t)=Kx1(t)+Xo1; Y1(t)=Ky1(t)+Yo1;} {X2(t)=Kx2(t)+Xo2; Y2(t)=Ky2(t)+Yo2;} - ��������� �������� ������� � ������� ���
						//������� ���������� ����� ������: d(t)=sqrt((X2(t)-X1(t))*(X2(t)-X1(t))+(Y2(t)-Y1(t))*(Y2(t)-Y1(t)))=r1+r2
						//�������� ��� ��������������: t1,2=-b+-sqrt(D)/2K ��� ������� ������������� t � ���� ������ ������� ������������

						//K1=Kx2-Kx1; K2=Ky2-Ky1; Xo=Xo2-Xo1; Yo=Yo2-Yo1; r1- ������ ������� ����; r2 - ������ ������� ����
						double 	K1 = secondCircle.movement.x.value * (speed2) / interval - firstCircle.movement.x.value * (speed1) / interval, 
								K2 = secondCircle.movement.y.value * (speed2) / interval - firstCircle.movement.y.value * (speed1) / interval,
								Xo = x2 - x1, Yo = y2 - y1;
						double b = 2 * (K1 * Xo + K2 * Yo), K = K1 * K1 + K2 * K2;
						double D = b * b - 4 * K * (Xo * Xo + Yo * Yo -	radiusSum * radiusSum);
						if (D > 0) { // ���� ������������ ������ ���� (������ ���� ������ �� ���� �������, ����� ��������� ����)
							double timeToCollision = ( -b - Math.sqrt(D)) / (2 * K);
							if (timeToCollision >= 0) { 
								// V---���������� ������������---V
								return (int) timeToCollision;
								// ^---���������� ������������---^
							}
							else {//mt=(-b+Math.sqrt(D))/(2 * K);
								timeToCollision = -timeToCollision - b / K;
							}
							
							if (timeToCollision >= 0) 
								//V---���������� ������������---V
								return (int) timeToCollision;
								//^---���������� ������������---^
						}
						return interval + 2; //��������� ���� ������� ������������ �� �����������	
					}
					//����� �� �����������
				}		
			}
			
		}
		return interval + 2;	
	}
}
