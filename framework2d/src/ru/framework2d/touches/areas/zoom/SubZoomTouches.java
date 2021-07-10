package ru.framework2d.touches.areas.zoom;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.EntityStorage;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Vector2;
import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.screen.TouchFinger;

public class SubZoomTouches extends ComponentsHandler {

	public SubZoomTouches() {
		super(1, (Class <Component>) TouchableAreaZoom.class.asSubclass(Component.class));
		
	}
	private long currentTick = 0;
	
	private Vector2 vToTouch = new Vector2();
	private Point2 oldMiddle = new Point2();
	private Point2 middle = new Point2();
	
	@Override
	public boolean transferLocalData(Component reflection, DataInterface data) {
		
		TouchableAreaZoom zoomedArea = (TouchableAreaZoom) reflection;
		
		if (data instanceof Point2) {
			if (zoomedArea.lstPointers.size() > 1) {
				
				long ticksNumber = enginesScheduler.currentTick; 
				if (ticksNumber != currentTick) {
					currentTick = ticksNumber;
					TouchFinger firstTouch = null;
					TouchFinger secondTouch = null;
					for(InputTouchInterface touch : zoomedArea.lstPointers.get(0).entity.lstTouches) {
						if (touch instanceof TouchFinger) {
							firstTouch = (TouchFinger) touch;
						}
					}
					for(InputTouchInterface touch : zoomedArea.lstPointers.get(1).entity.lstTouches) {
						if (touch instanceof TouchFinger) {
							secondTouch = (TouchFinger) touch;
						}
					}
					
					zoomedArea.betweenFingers.set(firstTouch.position, secondTouch.position);
					zoomedArea.oldBetweenFingers.set(firstTouch.oldPosition, secondTouch.oldPosition);
					
					oldMiddle.set(secondTouch.oldPosition);
					oldMiddle.move(zoomedArea.oldBetweenFingers, 0.5d);
					
					vToTouch.set(zoomedArea.realizePosition, oldMiddle);
					
					float zoomRatio = (float) (zoomedArea.betweenFingers.length2() / zoomedArea.oldBetweenFingers.length2());
					if (zoomedArea.zoom.value * zoomRatio > zoomedArea.zoomMax.value) {
						zoomRatio = zoomedArea.zoomMax.value / zoomedArea.zoom.value;
						zoomedArea.zoom.value = zoomedArea.zoomMax.value;
					} else if (zoomedArea.zoom.value * zoomRatio < zoomedArea.zoomMin.value) {
						zoomRatio = zoomedArea.zoomMin.value / zoomedArea.zoom.value;
						zoomedArea.zoom.value = zoomedArea.zoomMin.value;
					} else {
						zoomedArea.zoom.value *= zoomRatio;
					}
					
					vToTouch.selfMulti(zoomRatio);
					
					double deltaAngle = zoomedArea.betweenFingers.computeAngle() - zoomedArea.oldBetweenFingers.computeAngle();
					
					vToTouch.rotate(deltaAngle);
					
					zoomedArea.zoom.commit(masterEngine, zoomedArea.entity);
					
					middle.set(secondTouch.position);
					middle.move(zoomedArea.betweenFingers, 0.5d);
					
					zoomedArea.realizePosition.set(	middle.x.value + vToTouch.x.value, 
													middle.y.value + vToTouch.y.value, 
													(zoomedArea.realizePosition.getAlpha() + deltaAngle) % (2 * Math.PI));
	
					zoomedArea.realizePosition.commit(masterEngine, zoomedArea.entity);
				}
			}
		}
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
