package ru.framework2d.logic.grids;

import ru.framework2d.core.Property;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Point2;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.data.Vector2;
import ru.framework2d.logic.LogicalInterface;
import android.util.Log;


public class LogicalGrid extends LogicalInterface {
	
	public final static String SHORT_CLASS_NAME = "grid";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	

	public Position3 localPos = new Position3(0, 0, 0);
	public Text name = new Text("");
	
	public Numeral columnsNumber = new Numeral(1);
	public Numeral rowsNumber = new Numeral(1);

	public Bool isEnabled = new Bool(true);
	
	public CellGrid[][] cells = new CellGrid[1][1];
	
	public Point2 size = new Point2(0, 0);
	
	public Numeral respawnRow = new Numeral(0);
	
	
	
	//ArrayList <LogicalCellItem> lstItems = new ArrayList <LogicalCellItem> ();
	
	public LogicalGrid() {}
	
	
	
	@Override
	public void setPropertyData(Property property) {
		
		if (property.isNamed("columnsNumber")) {
			int columns = (int) Double.parseDouble(property.data.toString());
			if (columnsNumber.value != columns) {
				columnsNumber.value = columns;
				cells = new CellGrid[columnsNumber.value][rowsNumber.value];
			}
		} 
		else if (property.isNamed("rowsNumber")) {
			int rows = (int) Double.parseDouble(property.data.toString());
			if (rowsNumber.value != rows) {
				rowsNumber.value = rows;
				cells = new CellGrid[columnsNumber.value][rowsNumber.value];
			}
		} 
		else {
			super.setPropertyData(property);
		}
	}

	
	
	private Vector2 toLocal = new Vector2();
	
	private Vector2 toCell = new Vector2();
	
	public void setCellsParams() {
		
		if (size.x.value != 0 && size.y.value != 0) {
			
			float cellSizeX = (float) size.x.value / (float) columnsNumber.value;
			float cellSizeY = (float) size.y.value / (float) rowsNumber.value;
			
			for (int c = 0; c < columnsNumber.value; c++) {
				for (int r = 0; r < rowsNumber.value; r++) {
					
					if (cells[c][r] == null) {
						cells[c][r] = new CellGrid(c, r, cellSizeX, cellSizeY);
					} 
					else {
						cells[c][r].set(c, r, cellSizeX, cellSizeY);
					}
					
					toLocal.set(localPos.x.value, localPos.y.value);
					toLocal.rotate(position.getAlpha());
					
					toCell.set(-size.x.value / 2.0f + cellSizeX * (float) c + cellSizeX / 2.0f, 
								-size.y.value / 2.0f + cellSizeY * (float) r + cellSizeY / 2.0f);
					toCell.rotate(position.getAlpha());
					toCell.rotate(localPos.getAlpha());
					
					cells[c][r].position.set(position.x.value + toLocal.x.value + toCell.x.value, 
											position.y.value + toLocal.y.value + toCell.y.value);
					cells[c][r].position.setAlpha(position.getAlpha() + localPos.getAlpha());
				}			
			}
			
		}
	}
}
