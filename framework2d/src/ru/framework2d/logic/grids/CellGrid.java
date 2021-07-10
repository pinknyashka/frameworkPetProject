package ru.framework2d.logic.grids;

import java.util.ArrayList;

import ru.framework2d.data.Position3;


public class CellGrid {
	public int column = 0;
	public int row = 0;
	
	public float width = 0;
	public float height = 0;
	
	public Position3 position = new Position3();
	
	public ArrayList <LogicalCellItem> lstItems = new ArrayList <LogicalCellItem>(); 
	
	public CellGrid(){}
	public CellGrid(int column, int row, float width, float height) {
		this.column = column; this.row = row; this.width = width; this.height = height;
	}
	public void set(int column, int row, float width, float height) {
		this.column = column; this.row = row; this.width = width; this.height = height;
	}
}
