package ru.framework2d.logic.respawns;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.logic.LogicalInterface;

public class LogicalRespawn extends LogicalInterface {

	public final static String SHORT_CLASS_NAME = "l_respawn";
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}

	public Text respawnerName = new Text("");
	public LogicalRespawner respawner;
	
	public Position3 respawningPosition = new Position3();
	
	public Bool respawningTrigger = new Bool(false);
}
