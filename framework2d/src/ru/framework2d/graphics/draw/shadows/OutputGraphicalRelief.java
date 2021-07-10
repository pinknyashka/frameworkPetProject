package ru.framework2d.graphics.draw.shadows;

import ru.framework2d.core.Sprite;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.graphics.OutputGraphicalInterface;


public class OutputGraphicalRelief extends OutputGraphicalInterface {
	
	public final static String SHORT_CLASS_NAME = "relief"; 
	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
	
	public Text reliefName = new Text("");
	
	public Sprite reliefSprite;
	
	public Numeral spriteCollumnsNum = new Numeral(1);
	public Numeral spriteRowsNum = new Numeral(1);
	
	public Numeral currentSpriteNo = new Numeral(0);

	public OutputGraphicalRelief() { super(); };
	
}
