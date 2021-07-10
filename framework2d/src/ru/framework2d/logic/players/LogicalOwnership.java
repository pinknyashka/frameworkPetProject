package ru.framework2d.logic.players;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;


public class LogicalOwnership extends LogicalInterface {
	
	public final static String SHORT_CLASS_NAME = "ownership";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Text ownerName = new Text("");
	public LogicalPlayer owner;
	
	public Bool isExist = new Bool(true);
	public Bool isControlled = new Bool(true);
	public Bool isCurrentlyControlled = new Bool(false);
	
	public Bool isOwnerDrawRound = new Bool(false);
	public Bool isOwnerWonGame = new Bool(false);
	
	public LogicalOwnership() { }
	
}
