package ru.framework2d.logic.respawns;

import java.util.ArrayList;

import ru.framework2d.core.Entity;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;


public class LogicalRespawner extends LogicalInterface {

	public final static String SHORT_CLASS_NAME = "respawner";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Text name = new Text("");
	public Bool respawningTrigger = new Bool(false);
	public Numeral number = new Numeral(1);

	public Entity prototype;
	public ArrayList <LogicalRespawn> lstRespawns = new ArrayList <LogicalRespawn>();

}
