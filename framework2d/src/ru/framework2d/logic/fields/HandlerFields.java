package ru.framework2d.logic.fields;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Position3;
import ru.framework2d.logic.LogicalInterface;
import android.util.Log;

public class HandlerFields extends ComponentsHandler {
	
	ArrayList <LogicalUnit> lstUnits = new ArrayList <LogicalUnit>();
	
	ArrayList <LogicalField> lstFields = new ArrayList <LogicalField>();
	
	ArrayList <Entity> lstMovedObjects = new ArrayList <Entity>();
	
	
	
	private static final String LOG_TAG = "Logical";
	
	
	
	public HandlerFields() {
		super(2, (Class <Component>) LogicalField.class.asSubclass(Component.class), 
				(Class <Component>) LogicalUnit.class.asSubclass(Component.class));
		
		getContexts();
		loadEntities(entityStorage);
	}
	
	
	
	protected void iterate() {
		
    	long timerMark2 = System.currentTimeMillis();
    	
    	handleMoving();
    	
    	Log.d(LOG_TAG, name + ": step done in "+ (System.currentTimeMillis() - timerMark2) + " ms;");
    	masterEngine.subEngineWorkDone(this);
	}

	

	private ArrayList <LogicalField> lstFieldsWithMovingUnits = new ArrayList <LogicalField>();

	private void handleMoving() {
		
		lstFieldsWithMovingUnits.clear();
		
		while (!lstMovedObjects.isEmpty()) {
			
	    	//Log.d(LOG_TAG, name + ": got moved object: " + lstMovedObjects.get(0).name + ";");
			
			for (LogicalInterface logical : lstMovedObjects.get(0).lstLogicals) {
				
				if (logical instanceof LogicalUnit) {
					LogicalUnit unit = (LogicalUnit) logical;
					
					for (LogicalField field : unit.lstFieldsToCheck) {
						
						if (field.isEnabled.value) {
							
							if (!lstFieldsWithMovingUnits.contains(field)) {
								lstFieldsWithMovingUnits.add(field);
							}
							
							boolean inField = !field.checkEscape(unit.position);
							if (unit.inField.value != inField) { //if changed
								unit.inField.value = inField;
								
								Log.d(LOG_TAG, name + ": commit " + unit.entity.name + " to " + inField);
								
								unit.inField.commit(this, unit.entity);
								if (!unit.inField.value) {
									if (field.lstUnitsInField.remove(unit) && field.lstUnitsInField.isEmpty()) {
										field.gotUnitInField.value = false;
										field.gotUnitInField.commit(this, field.entity);
									}
								} 
								else {
									if (!field.lstUnitsInField.contains(unit)) {
										if (field.lstUnitsInField.isEmpty()) {
											field.gotUnitInField.value = true;
											field.gotUnitInField.commit(this, field.entity);
										}
										field.lstUnitsInField.add(unit);
									}
								}
							}
						}
					}
					
					//if (!changed) gotMovingUnits = true;

				} 
				else if (logical instanceof LogicalField) {
					LogicalField field = (LogicalField) logical;
					for (LogicalUnit unit : field.lstUnits) {
						boolean inField = !field.checkEscape(unit.position);
						if (unit.inField.value != inField) { //if changed
							unit.inField.value = inField;
							Log.d(LOG_TAG, name + ": commit " + unit.entity.name + " to " + inField);
							unit.inField.commit(this, unit.entity);
							if (!unit.inField.value) {
								if (field.lstUnitsInField.remove(unit) && field.lstUnitsInField.isEmpty()) {
									field.gotUnitInField.value = false;
									field.gotUnitInField.commit(this, field.entity);
								}
							} 
							else {
								if (!field.lstUnitsInField.contains(unit)) {
									if (field.lstUnitsInField.isEmpty()) {
										field.gotUnitInField.value = true;
										field.gotUnitInField.commit(this, field.entity);
									}
									field.lstUnitsInField.add(unit);
								}
							}
						}
					}
				}
			}
			lstMovedObjects.remove(0);
    	}
		
		for (LogicalField field : lstFields) {
			
			if (lstFieldsWithMovingUnits.contains(field)) {
				
				if (!field.gotMovingUnits.value) {
					
					field.gotMovingUnits.value = true;
					field.gotMovingUnits.commit(this, field.entity);
					Log.d(LOG_TAG, name + ": set " + field.name + ".gotMovingUnits to: " + field.gotMovingUnits.value + ";");
				}
				
			} 
			else {
				
				if (field.gotMovingUnits.value) {

					setWorkState(false);
					
					field.gotMovingUnits.value = false;
					field.gotMovingUnits.commit(this, field.entity);
					Log.d(LOG_TAG, name + ": set " + field.name + ".gotMovingUnits to: " + field.gotMovingUnits.value + ";");
					
				}
			}
		}

	}
	
	

	@Override
	public boolean transferData(Entity object, DataInterface data) {
		Log.d(LOG_TAG, name + ": transfer data " + data.getName() + " in " + object.name);
		long timerMark1 = System.currentTimeMillis();
		
		boolean transfered = false;
		
		for (Component component : object.lstLogicals) {
			if (component instanceof LogicalUnit || component instanceof LogicalField) {
				if (data instanceof Position3) {
					transfered = true;
					
					setWorkState(true);
					
					Position3 newPosition = (Position3) data;
					
					//Log.d(LOG_TAG, name + ": moving unit to " + newPosition.getX() + " : " + newPosition.getY());
					
					if (!lstMovedObjects.contains(object)) lstMovedObjects.add(object);
					
					for (int log = 0; log < object.lstLogicals.size(); log++) {
						object.lstLogicals.get(log).position.set(newPosition);
					}
				} 
			}
		}
		
		Log.d(LOG_TAG, name + ": transfer done in "+ (System.currentTimeMillis() - timerMark1) + " ms;");
		return transfered;
	}

	public Component connectComponent(Component component) {
		super.connectComponent(component);
		
		if (component instanceof LogicalField) {
			
			if (!lstFields.contains(component)) {
				
				LogicalField field = (LogicalField) component;
				lstFields.add(field);
				
				for (LogicalUnit unit : lstUnits) {
					
					for (String fieldName : unit.lstFieldsNameToCheck) {
						
						if (fieldName.contentEquals(field.name.value)) {
							
							if (!field.lstUnits.contains(unit)) field.lstUnits.add(unit);
							
							if (!unit.lstFieldsToCheck.contains(field)) unit.lstFieldsToCheck.add(field);
						}
					}
					if (unit.fieldName.value.contentEquals(field.name.value)) {
						
						if (!field.lstUnits.contains(unit)) field.lstUnits.add(unit);
						
						if (!unit.lstFieldsToCheck.contains(field)) unit.lstFieldsToCheck.add(field);
					}
				}
			}
		} 
		else if (component instanceof LogicalUnit) {
			
			if (!lstUnits.contains(component)) {
				
				if (component.entity != null) {
					
					LogicalUnit firstUnit = null; 
					LogicalUnit secondUnit = null;
					
					for (LogicalInterface logical : component.entity.lstLogicals) {
						
						if (logical instanceof LogicalUnit) {
							
							if (firstUnit == null)  firstUnit = (LogicalUnit) logical;
							else secondUnit = (LogicalUnit) logical;
							
							LogicalUnit unit = (LogicalUnit) logical;
							
							for (LogicalField field : lstFields) {
								
								for (String fieldName : unit.lstFieldsNameToCheck) {
									
									if (field.name.value.contentEquals(fieldName)) {
										
										if (!firstUnit.lstFieldsToCheck.contains(field)) firstUnit.lstFieldsToCheck.add(field);
										
										if (!field.lstUnits.contains(firstUnit)) {
											field.lstUnits.add(firstUnit);
//											if (!lstMovedObjects.contains(firstUnit.entity)) lstMovedObjects.add(firstUnit.entity);
//											setWorkState(true);
										}
									}
								}
								
								if (field.name.value.contentEquals(unit.fieldName.value)) {
									
									if (!field.lstUnits.contains(firstUnit)) field.lstUnits.add(firstUnit);
									
									if (!field.lstUnits.contains(firstUnit)) field.lstUnits.add(firstUnit);
								}
							}		
						}
					}
					if (secondUnit != null) {
						component.entity.lstLogicals.remove(secondUnit);
						component.entity.lstComponents.remove(secondUnit);
					}
					lstUnits.add(firstUnit);
					return firstUnit;
				}
						
			}
		}
		return component;
	}
	
	
	
}
