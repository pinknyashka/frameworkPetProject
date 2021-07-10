package ru.framework2d.logic.grids;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;

public class LogicalCellItem extends LogicalInterface {

	public final static String SHORT_CLASS_NAME = "item";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Position3 cellPosition = new Position3();
	public Bool isEnabled = new Bool(false);
	
	public Text gridName = new Text("");
	
	public Text name = new Text("");
	
	public LogicalGrid grid;
	public CellGrid cell;
	
	public LogicalCellItem() { }
}
