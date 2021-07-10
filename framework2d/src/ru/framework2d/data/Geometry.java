package ru.framework2d.data;


public abstract class Geometry extends DataInterface {

	public abstract boolean setGeometry(Geometry geometry);

	public void setValue(DataInterface data) {
		if (data instanceof Geometry) setGeometry((Geometry) data);
	}
}
