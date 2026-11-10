package ru.framework2d.core;

public class DataCommunicationContext {
	//пара имя данного - контекст. Необходимо для заполнения контестами данных новых отражений 
	
	String dataNameInComponent = "";
	
	DataContext context = null;
	
	String transferValue = "";
	
	public DataCommunicationContext(String dataName, DataContext context) {
		this.dataNameInComponent = dataName;
		this.context = context;
	}
}
