package ru.framework2d.logic.players;

import java.util.ArrayList;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.data.Trigger;
import ru.framework2d.logic.LogicalInterface;


public class LogicalPlayer extends LogicalInterface {
	
	public final static String SHORT_CLASS_NAME = "player";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Text name = new Text("");
	
	public Bool gotControllingOwnership = new Bool(true);
	
	public Bool gotAccess = new Bool(true);

	public Bool isInProgress = new Bool(true);

	public Bool isGetTurnIfFlawless = new Bool(true);
	public Numeral numOwnershipAtRoundStart = new Numeral(0);
	
	public Bool isWaitForTurnOver = new Bool(true);
	public Bool isGetTurnIfWon = new Bool(true);

	public Bool isLoseRoundIfHaveNoOwnership = new Bool(true);
	
	public Trigger finishRoundTrigger = new Trigger();
	
	public Numeral wonRounds = new Numeral(0);
	public Numeral points = new Numeral(0);
	
	public Numeral pointsGaveForRoundVictory = new Numeral(1);
	public Numeral pointsLostForRoundLose = new Numeral(1);

	public Numeral pointsGaveForFlawlessVictory = new Numeral(2);
	
	public Numeral startPoints = new Numeral(0);
	public Numeral pointsNeedToVictory = new Numeral(10);
	
	public Bool canGetNegativePoints = new Bool(false);
	public Bool winGavePointsOnlyIfEnemyHaveNoPoints = new Bool(false);
	
	public Trigger finishedRoundTrigger = new Trigger();
	
	public ArrayList <LogicalOwnership> lstOwnership = new ArrayList <LogicalOwnership>(); 
	public ArrayList <LogicalOwnership> lstActiveOwnership = new ArrayList <LogicalOwnership>(); 
	
	public LogicalPlayer() { }

}
