package ru.framework2d.graphics.animation;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Trigger;
import ru.framework2d.graphics.OutputGraphicalInterface;


public class OutputGraphicalAnimation extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "animation"; 
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Fractional interval = new Fractional(0.1f);
	
	public Fractional step = new Fractional(0.1f);
	
	public Fractional currentValue = new Fractional();
	
	public Fractional from = new Fractional();

	public Fractional to = new Fractional();
	
	public Trigger activation = new Trigger();
	
	public Numeral loops = new Numeral(1);

	public Bool isRunning = new Bool();

	public OutputGraphicalAnimation() { super(); };
	
}
