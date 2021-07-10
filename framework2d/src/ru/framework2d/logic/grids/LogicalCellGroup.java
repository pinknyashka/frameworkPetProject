package ru.framework2d.logic.grids;

import java.util.ArrayList;

import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;


public class LogicalCellGroup extends LogicalInterface {
	
	public final static String SHORT_CLASS_NAME = "cell_group";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Text name = new Text("");
	
	public Text gridName = new Text("");
	public LogicalGrid grid;
	
	public Text relativeCells = new Text("");
	public ArrayList <CellGrid> lstCells = new ArrayList <CellGrid>();
	
	public Text itemName = new Text("");
	public ArrayList <LogicalCellItem> lstItems = new ArrayList <LogicalCellItem>();
	
	public Numeral leaderRow = new Numeral(0);
	public Numeral leaderColumn = new Numeral(0);
}
