package ru.framework2d.core;

public class DataCommunicationContext {
	//���� ��� ������� - ��������. ���������� ��� ���������� ���������� ������ ����� ��������� 
	
	String dataNameInComponent = "";
	
	DataContext context = null;
	
	String transferValue = "";
	
	public DataCommunicationContext(String dataName, DataContext context) {
		this.dataNameInComponent = dataName;
		this.context = context;
	}
}
