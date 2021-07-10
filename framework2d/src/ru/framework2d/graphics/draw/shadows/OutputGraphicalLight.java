package ru.framework2d.graphics.draw.shadows;

import ru.framework2d.core.Property;
import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.graphics.OutputGraphicalInterface;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Path;
import android.graphics.RectF;

public class OutputGraphicalLight extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "light"; 
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	
	public Fractional intensity = new Fractional(1.0f);
	
	public Fractional diffuse = new Fractional(0.0f);

	
	public Bool glow = new Bool(false);

	
	public Numeral color = new Numeral(Color.WHITE);
	
	public Numeral red = new Numeral(255);
	
	public Numeral green = new Numeral(255);
	
	public Numeral blue = new Numeral(255);
	
	
	
	public ColorFilter colorFilter;
	
	public ColorFilter diffuseFilter;
	
	
	
	public Path drawingArea = new Path();
	
	public RectF drawingRect = new RectF();

	
	
	@Override
	public void setPropertyData(Property property) {
		
		if (property.isNamed("color")) {
			
			if (property.data.toString().length() > 0 && property.data.toString().charAt(0) == '#') {
				color.value = Color.parseColor(property.data.toString());
			} 
			else {
				color.value = (int) (Double.parseDouble(property.data.toString()));
			}
			setColor(color.value);
			
		} 
		else {
			super.setPropertyData(property);
		}
	}
	
	
	
	public void setColor(int color) {
		
		this.color.value = color;
		
		red.value = Color.red(color);
		green.value = Color.green(color);
		blue.value = Color.blue(color);
		
		setFilters();
	}
	
	public void setRed(int red) {
		setColor(Color.rgb(red, green.value, blue.value));
	}
	
	public void setGreen(int green) {
		setColor(Color.rgb(red.value, green, blue.value));
	}
	
	public void setBlue(int blue) {
		setColor(Color.rgb(red.value, green.value, blue));
	}
	
	public void setIntensity(float intensity) {
		
		this.intensity.value = intensity;
		
		setFilters();
	}
	
	public void setDiffuse(float diffuse) {
		
		this.diffuse.value = diffuse;
		
		diffuseFilter = new LightingColorFilter(
        		Color.rgb(	(int) (red.value * this.diffuse.value * intensity.value), 
        					(int) (green.value * this.diffuse.value * intensity.value),
        					(int) (blue.value * this.diffuse.value * intensity.value)), 
				Color.BLACK);
	}
	
	private void setFilters() {
		
		colorFilter = new LightingColorFilter(
				Color.rgb(	(int) (red.value * intensity.value), 
							(int) (green.value * intensity.value),
							(int) (blue.value * intensity.value)), 
				Color.BLACK);
        diffuseFilter = new LightingColorFilter(
        		Color.rgb(	(int) (red.value * diffuse.value * intensity.value), 
        					(int) (green.value * diffuse.value * intensity.value),
        					(int) (blue.value * diffuse.value * intensity.value)), 
        		Color.BLACK);
	}
}
