package ru.framework2d.logic;

import ru.framework2d.core.Engine;

public class DirectLogicStandart extends Engine {

	@Override
	protected void onFinish() {
		masterEngine.subEngineWorkDone(this);
	}
	
}
