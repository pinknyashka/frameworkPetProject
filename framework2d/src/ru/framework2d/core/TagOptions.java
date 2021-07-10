package ru.framework2d.core;

import java.util.ArrayList;


public class TagOptions {
	
	public String name;
	
	public ArrayList <Property> lstPropertys = new ArrayList <Property>();
	
	public int depth = 0;
	
	public TagOptions() {}
	
	public TagOptions(String name) {
		this.name = name;
	}
	
	
	
	public Property findPropertyByName(String propertyName) {
		
		int propertyNo = 0;
		
		while (propertyNo < lstPropertys.size()) {
			
			if (lstPropertys.get(propertyNo).isNamed(propertyName)) {
				
				return lstPropertys.get(propertyNo);
			}
			else {
				propertyNo++;
			}
		} 
		
		return null;
	}
	
}
