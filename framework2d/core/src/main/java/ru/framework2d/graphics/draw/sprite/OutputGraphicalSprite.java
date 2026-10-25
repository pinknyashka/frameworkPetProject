package ru.framework2d.graphics.draw.sprite;

import ru.framework2d.core.Sprite;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;

import ru.framework2d.graphics.OutputGraphicalInterface;


public class OutputGraphicalSprite extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "pict";
	
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	
	
	public Text spriteName = new Text("");
	
	public Sprite sprite;

	
	
	public Numeral spriteCollumnsNum = new Numeral(1);
	
	public Numeral spriteRowsNum = new Numeral(1);
	
	public Numeral currentSpriteNo = new Numeral(0);
	
	public Text normalMapName = new Text("");
	
	public Fractional heightScale = new Fractional(1.0f);
	
	public Fractional specular = new Fractional(0.0f);
	
	public Fractional shininess = new Fractional(16.0f);
	
	public Bool useParallax = new Bool(false);
	
	public Bool cullable = new Bool(true);

}
