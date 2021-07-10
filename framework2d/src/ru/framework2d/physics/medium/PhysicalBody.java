package ru.framework2d.physics.medium;

import ru.framework2d.core.Component;
import ru.framework2d.data.Fractional;
import ru.framework2d.physics.PhysicalInterface;

public class PhysicalBody extends PhysicalInterface {
	
	public Fractional roughness = new Fractional(1.0f);
	
	
	
	public final static String SHORT_CLASS_NAME = "body";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	

	public final static Class <Component> COMPONENT_CLASS = 
			(Class <Component>) (PhysicalBody.class.asSubclass(Component.class));
	
	@Override
	public Class <Component> getComponentClass() {
		return COMPONENT_CLASS;
	}
}
