package ru.framework2d.logic.grids;

import java.util.ArrayList;

import ru.framework2d.core.Component;
import ru.framework2d.core.ComponentsHandler;
import ru.framework2d.core.Entity;
import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Vector2;
import android.util.Log;

public class HandlerGrids extends ComponentsHandler {

	private static final String LOG_TAG = "Logical";
	
	ArrayList <LogicalGrid> lstGrids = new ArrayList <LogicalGrid>();
	
	ArrayList <LogicalCellGroup> lstGroups = new ArrayList <LogicalCellGroup>();
	
	ArrayList <LogicalCellItem> lstItems = new ArrayList <LogicalCellItem>();
	
	
	
	public HandlerGrids() {
		super(3, (Class <Component>) LogicalGrid.class.asSubclass(Component.class), 
			(Class <Component>) LogicalCellItem.class.asSubclass(Component.class),
			(Class <Component>) LogicalCellGroup.class.asSubclass(Component.class));

		getContexts();
		loadEntities(entityStorage);
	}

	
	
	@Override
	public boolean startWork(int delay) {
		return false;
	}
	
	private boolean itemHaveNoGrid = false;
	
	public Component connectComponent(Component component) {
		super.connectComponent(component);
		if (component instanceof LogicalGrid) {
			if (!lstGrids.contains(component)) {
				LogicalGrid grid = (LogicalGrid) component;
				lstGrids.add(grid);
				grid.setCellsParams();
				/*if (itemHaveNoGrid) {
					boolean stillGotItemWithoutGrid = false;
					for (LogicalCellItem ownership : lstItems) {
						if (ownership.owner == null) {
							if (player.name.contentEquals(ownership.ownerName)) {
								ownership.owner = player;
								if (ownership.master != null && ownership.master.isSingle) {
									player.lstOwnership.add(ownership);
									player.gotControllingOwnership.value = true;
								}
							} 
							else stillGotOwnershipWithoutOwner = true;
						}
					}
					if (!stillGotItemWithoutGrid) itemHaveNoGrid = false;
				}*/

			}
		} 
		else if (component instanceof LogicalCellGroup) {
			if (!lstGroups.contains(component)) {
				LogicalCellGroup group = (LogicalCellGroup) component;
				lstGroups.add(group);
				if (!group.gridName.value.contentEquals("")) {
					for (LogicalGrid grid : lstGrids) {
						if (grid.name.value.contentEquals(group.gridName.value)) {
							group.grid = grid;
							if (!group.relativeCells.value.contentEquals("")) {
								int x = 0, y = 0;
								int separatorNo;
								String subString = "" + group.relativeCells;
								while (subString.length() > 2) {
									separatorNo = subString.indexOf(',');
									if (separatorNo < 0) {
										separatorNo = subString.length(); 
									}
									x = group.leaderColumn.value + Integer.parseInt(subString.substring(0, separatorNo));
									subString = subString.substring(separatorNo + 1);
									separatorNo = subString.indexOf(',');
									if (separatorNo < 0) {
										separatorNo = subString.length(); 
									}
									y = group.leaderRow.value + Integer.parseInt(subString.substring(0, separatorNo));
									if (subString.length() > separatorNo) subString = subString.substring(separatorNo + 1);
									if (grid.cells[x][y] != null) {
										group.lstCells.add(grid.cells[x][y]);
									}
								}
							}
						}
					}
				}
			}
		} 
		else if (component instanceof LogicalCellItem) {
			if (!lstItems.contains(component)) {
				LogicalCellItem item = (LogicalCellItem) component;
				for (LogicalCellGroup group : lstGroups) { 
					if (group.itemName.value.contentEquals(item.name.value) && !group.lstItems.contains(item)) {
						group.lstItems.add(item);
						if (item.entity != null && !item.entity.isPrototype) {
							int i = 0;
							boolean itemIsSet = false;
							while (i < group.lstCells.size() && !itemIsSet) {
								CellGrid cell = group.lstCells.get(i);
								if (cell.lstItems.isEmpty()) {
									cell.lstItems.add(item);
									item.cell = cell;
									itemIsSet = true;
								}
								i++;
							}
							if (item.cell != null) {
								item.cellPosition.set(item.cell.position);
								item.cellPosition.commit(this, item.entity);
								item.position.set(item.cellPosition);
								item.position.commit(this, item.entity);
							}
						}
					}
				}
				/*for (LogicalGrid grid : lstGrids) {
					if (grid.name.contentEquals(item.gridName)) {
						item.grid = grid;
						if (item.master != null && !item.master.isPrototype) {
							
							for (int c = 0; c < grid.columnsNumber.value; c++) {
								for (int r = 0; r < grid.rowsNumber.value; r++) {
									if (grid.cells[c][r].lstItems.isEmpty()) {
										grid.cells[c][r].lstItems.add(item);
										item.cell = grid.cells[c][r]; 
										Log.d(LOG_TAG, name + ": c=" + c + "; r=" + r + " move to: " 
												+ grid.cells[c][r].position.getX() + " : " 
												+ grid.cells[c][r].position.getY() + ";");
										c = grid.columnsNumber.value;
										r = grid.rowsNumber.value;
									}
								}
							}
							if (item.cell != null) {
								item.cellPosition.set(item.cell.position);
								item.cellPosition.commit(this, item.master);
								item.position.set(item.cellPosition);
								item.position.commit(this, item.master);
							}
							
						}
					}
				} */
				if (item.grid == null) {
					itemHaveNoGrid = true;
					Log.d(LOG_TAG, name + ": created reflection got no owner;");
				}
				
				lstItems.add(item);
				
			}
		}
		return component;
	}
	private Vector2 dispositionDirect = new Vector2();
	
	@Override
	public boolean transferData(Entity object, DataInterface data) {
		Log.d(LOG_TAG, name + ": transfer data " + data.getName() 
				+ "<" + data.getContext().name + "> in " + object.name);
		
		boolean transfered = false;
		
		long timerMark1 = System.currentTimeMillis();
		for (Component reflection : object.lstLogicals) {
			if (reflection instanceof LogicalCellItem) {
				if (data instanceof Position3) {
					transfered = true;
					Position3 where = (Position3) data;
					reflection.position.set(where);
				} 
				else {
					LogicalCellItem item = (LogicalCellItem) reflection;
					if (data instanceof Bool) {
						if (data.getContext() == item.isEnabled.getContext()) {
							transfered = true;
						} 
					}
				}
			} 
			else if (reflection instanceof LogicalGrid) {
				LogicalGrid grid = (LogicalGrid) reflection;
				if (data instanceof Position3) {
					transfered = true;
					Position3 where = (Position3) data;
					grid.position.set(where);
				} 
				else if (data instanceof Bool) {
					if (data.getContext() == grid.isEnabled.getContext()) {
						transfered = true;
						long timerMark2 = System.currentTimeMillis();
					}
				} 
				else if (data instanceof Numeral) {
					if (data.getContext() == grid.respawnRow.getContext()) {
						Log.d(LOG_TAG, name + ": transfer in " + grid.name + " resp No " + ((Numeral) data).value);
						transfered = true;
						float cellSize = (float) grid.size.y.value / (float) grid.rowsNumber.value;
						
						dispositionDirect.set(0, 1);
						dispositionDirect.rotate(grid.localPos.getAlpha());
						grid.localPos.move(dispositionDirect, (double) (cellSize * grid.respawnRow.value));
						
						grid.respawnRow.value = ((Numeral) data).value;
						dispositionDirect.selfMulti(-1);
						grid.localPos.move(dispositionDirect, cellSize * grid.respawnRow.value);
						grid.setCellsParams();
						
						for (int c = 0; c < grid.columnsNumber.value; c++) {
							for (int r = 0; r < grid.rowsNumber.value; r++) {
								for (LogicalCellItem item : grid.cells[c][r].lstItems) {
									item.cellPosition.set(grid.cells[c][r].position);
									item.cellPosition.commit(this, item.entity);
								}
							}
						}
						
					}
				}
			} 
			else if (reflection instanceof LogicalCellGroup) {
				LogicalCellGroup group = (LogicalCellGroup) reflection;
				if (data instanceof Numeral) {
					if (data.getContext() == group.leaderRow.getContext()) {
						Log.d(LOG_TAG, name + ": transfer in " + group.grid.name + " resp No " + ((Numeral) data).value);
						transfered = true;
						 
						int newRow = ((Numeral) data).value;
						
						if (group.leaderRow.value != newRow) {
							for (int cellNo = 0; cellNo < group.lstCells.size(); cellNo++) {
								CellGrid oldCell = group.lstCells.get(cellNo);
								int row = oldCell.row - group.leaderRow.value + newRow;
								CellGrid newCell = group.grid.cells[oldCell.column][row];
								group.lstCells.set(cellNo, newCell);
								for (int i = 0; i < oldCell.lstItems.size(); i++) {
									LogicalCellItem item = oldCell.lstItems.get(i);
									if (group.lstItems.contains(item)) {
										oldCell.lstItems.remove(i--);
										newCell.lstItems.add(item);
										item.cellPosition.set(newCell.position);
										item.position.set(newCell.position);
										item.cellPosition.commit(this, item.entity);
									}
								}
							}
							group.leaderRow.value = newRow;
						}
						
					}
				}
			}
		} 
		Log.d(LOG_TAG, name + ": transfer done in " + (System.currentTimeMillis() - timerMark1) + " ms;");
		return transfered;
	}
}
