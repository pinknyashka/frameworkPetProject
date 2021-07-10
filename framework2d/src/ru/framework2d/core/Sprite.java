package ru.framework2d.core;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class Sprite {
	
	public String name = "";
	
	public Bitmap spriteImage;
	
	
	
	public ArrayList <Rect> lstSpritesRects = new ArrayList <Rect>();
	
	int currentSpriteNo = 0;
	
	
	
	public Sprite(Bitmap bitmap, String bitmapName, int collumnsNum, int rowsNum) { 
		
		name = bitmapName;
		
		spriteImage = bitmap;
		
		for (int top = 0; top < bitmap.getHeight(); top += bitmap.getHeight() / rowsNum) {
			
			for (int left = 0; left < bitmap.getWidth();) {
				
				lstSpritesRects.add(
						new Rect(left, 
								 top, 
								 left += bitmap.getWidth() / collumnsNum, 
								 top + bitmap.getHeight() / rowsNum)
						);
			}
		}
	}
}
