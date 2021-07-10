package ru.framework2d.core;

import ru.framework2d.data.DataInterface;

public class Property {
	
	public String name;
	
	public DataInterface data;
	
	
	
	public Property(String name, DataInterface data) {
		this.name = name; this.data = data;
	}
	
	
	
	public boolean isNamed(String name) {
		return this.name.equalsIgnoreCase(name);
	}
}
