package ru.framework2d.data;


public class Area extends Point3 {
	
	public Point3 left_top = new Point3();
	public Point3 right_bottom = new Point3();
	public float radius;
	
	public Position3 center_d;

	public boolean isAabb = false;
	public boolean isSphere = false;
	
	public double areaBySphere;
	public double areaByAabb;

	
	
	public void setArea(Area area) {
		radius = area.radius;
		x.value = area.x.value; y.value = area.y.value; z.value = area.z.value;  
		isAabb = area.isAabb; isSphere = area.isSphere;
		validateArea();
		left_top.set(area.left_top);
		right_bottom.set(area.right_bottom);
	}
	
	
	
	public void setArea(float x, float y, float z, float r) {
		radius = r;
		set(x, y, z);
		validateArea();
		if (center_d != null) validateAabb();
	}
	
	
	
	public void setCenter(Position3 position) {
		center_d = (Position3) position;
	}
	
	
	
	public void validateArea() {
		if (getZ() != 0) {
			areaBySphere = ((float) Math.PI) * radius * radius * radius;
			areaByAabb = x.value * y.value * z.value;
		} else {
			areaBySphere = ((float) Math.PI) * radius * radius;
			areaByAabb = getX() * getY();
		}
	}
	
	
	
	public boolean incorrupted(Vector2 myMove, Area hisArea, Vector2 hisMove) {
		double myMoveX = myMove.getX(), myMoveY = myMove.getY();
		double 	myRightBorder = right_bottom.x.value, myLeftBorder = left_top.x.value,
				myBottomBorder = right_bottom.y.value, myTopBorder = left_top.y.value;
		if (myMoveX > 0) myRightBorder += myMoveX;
		else myLeftBorder += myMoveX;
		if (myMoveY > 0) myBottomBorder += myMoveY;
		else myTopBorder += myMoveY;
		
		double 	hisRightBorder = hisArea.right_bottom.x.value, hisLeftBorder = hisArea.left_top.x.value,
				hisBottomBorder = hisArea.right_bottom.y.value, hisTopBorder = hisArea.left_top.y.value;
		double hisMoveX = hisMove.getX(), hisMoveY = hisMove.getY();
		
		if (hisMoveX > 0) hisRightBorder += hisMoveX;
		else hisLeftBorder += hisMoveX;
		if (hisMoveY > 0) hisBottomBorder += hisMoveY;
		else hisTopBorder += hisMoveY;
		
		if (center_d.x.value < hisArea.center_d.x.value) {
			if (myRightBorder > hisLeftBorder) {
				if (center_d.y.value < hisArea.center_d.y.value) {
					if (myBottomBorder > hisTopBorder) {
						return true;
					}					
				} else if (myTopBorder < hisBottomBorder) {
					return true;
				}
			}
		} 
		else {
			if (myLeftBorder < hisRightBorder) {
				if (center_d.y.value < hisArea.center_d.y.value) {
					if (myBottomBorder > hisTopBorder) {
						return true;
					}
				} 
				else if (myTopBorder < hisBottomBorder) {
					return true;
				} 
			} 
		}
		return false;
	}
	
	
	
	public void validateAreaByAabb() {
		if (getZ() != 0) 	areaByAabb = getX() * getY() * getZ();
		else  				areaByAabb = getX() * getY();
	}
	
	
	
	private Vector2 toPoint = new Vector2();
	
	public void validateAabb() { 
		if (isSphere) {
			left_top.set((float) center_d.x.value - radius, 
					(float) center_d.y.value - radius, 
					(float) center_d.z.value - radius);
			right_bottom.set(	(float) center_d.x.value + radius, 
					(float) center_d.y.value + radius, 
					(float) center_d.z.value + radius);
		} 
		else {
			if (center_d.getAlpha() != 0) {
				
				toPoint.set(x.value / 2, y.value / 2);
				//double angle = Math.toRadians(center_d.getAlpha());
				double angle = center_d.getAlpha();
				toPoint.rotate(angle);
				
				double left_topX = toPoint.getX();
				double left_topY = toPoint.getY();
				double right_bottomX = toPoint.getX();
				double right_bottomY = toPoint.getY();
				
				double currentX = -left_topX, currentY = -left_topY;;
				
				if (currentX < left_topX) left_topX = currentX; 
				if (currentX > right_bottomX) right_bottomX = currentX;
				if (currentY < left_topY) left_topY = currentY; 
				if (currentY > right_bottomY) right_bottomY = currentY;
				
				toPoint.set(x.value / 2, -y.value / 2);
				toPoint.rotate(angle);
				
				currentX = toPoint.getX();
				currentY = toPoint.getY();
				
				if (currentX < left_topX) left_topX = currentX; 
				if (currentX > right_bottomX) right_bottomX = currentX;
				if (currentY < left_topY) left_topY = currentY; 
				if (currentY > right_bottomY) right_bottomY = currentY;
				
				currentX = -currentX;
				currentY = -currentY;
				
				if (currentX < left_topX) left_topX = currentX; 
				if (currentX > right_bottomX) right_bottomX = currentX;
				if (currentY < left_topY) left_topY = currentY; 
				if (currentY > right_bottomY) right_bottomY = currentY;
				
				left_top.set(		(float) (center_d.x.value + left_topX), 
									(float) (center_d.y.value + left_topY), 0);
				right_bottom.set(	(float) (center_d.x.value + right_bottomX), 
									(float) (center_d.y.value + right_bottomY), 0);
			} 
			else {
				left_top.set(		(float) center_d.x.value - x.value / 2, 
									(float) center_d.y.value - y.value / 2, 
									(float) center_d.z.value - z.value / 2);
				right_bottom.set(	(float) center_d.x.value + x.value / 2, 
									(float) center_d.y.value + y.value / 2, 
									(float) center_d.z.value + z.value / 2);
			}
		}
	}
	
	
	
	public boolean contains(Point3 p) {
		if (isAabb) {
			if (p.getX() >= left_top.getX() && p.getX() <= right_bottom.getX())
				if (p.getY() >= left_top.getY() && p.getY() <= right_bottom.getY())
					if (p.getZ() >= left_top.getZ() && p.getZ() <= right_bottom.getZ())
						return true;
		} 
		else if (isSphere) {
			return (p.dist(center_d) < radius);  
		}
		return false;
	}
	
	
	
	@Override
	public void setValue(DataInterface data) {
		if (data instanceof Area) setArea((Area) data);
	}
	
}
