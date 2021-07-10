package ru.framework2d.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import ru.fungames.checkers.R;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;

public class GameResources {
	
	private static final String LOG_TAG = "Resources";
	
	public static final String APP_DIRECTORY = "sPart";
	public static final String CONFIG_NAME = "config.xml";
	
	public static final String IMAGE_NOT_FOUND = "image_not_found.png";

	public static final String DEAFULT_MAP_DIRECTORY = "Maps";
	public static final String DEFAULT_IMAGES_DIRECTORY = "Images";
	public static final String DEFAULT_SOUNDS_DIRECTORY = "Sounds";

	protected Config mConfiguration;
	
	public Resources appResources;
	
	public SurfaceView gameSurface;
	
	public GameResources(Resources resources) {
		appResources = resources;
		mConfiguration = new Config();
	}
	
	//vv----------------------------------------Configs------------------------------------------vv	
	private class Config {
		
		String mapsDirectory = "" + DEAFULT_MAP_DIRECTORY;
		String imagesDirectory = "" + DEFAULT_IMAGES_DIRECTORY;
		String soundsDirectory = "" + DEFAULT_SOUNDS_DIRECTORY;
		
		Config() {
			
			try {
				
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				XmlPullParser xmlConfig = factory.newPullParser();
			
	        	boolean gotErrors = false;
	        	
				if (storageIsAvailable()) {
		        	File sdPath = Environment.getExternalStorageDirectory();
		        	sdPath = new File(sdPath.getAbsolutePath() + "/" + APP_DIRECTORY);
		        	
		        	if (sdPath.exists()) { 
			        	File configFile = new File(sdPath, CONFIG_NAME);
			        	BufferedReader reader = null;
			        	try {
			        		reader = new BufferedReader(new FileReader(configFile));
			        	} catch (FileNotFoundException e) {
			        		if (moveLocalConfigsToSd()) {
			        			try {
									reader = new BufferedReader(new FileReader(configFile));
								} catch (FileNotFoundException e1) {
					        		Log.w(LOG_TAG, "Config read warning. File reader error: File not found;");
									gotErrors = true;
									e1.printStackTrace();
								}
			        		}
			        		e.printStackTrace();
			        	} 
			        	
			        	if (reader != null) {
			        		try {
				        		xmlConfig.setInput(reader);
				        		readConfigsFromXml(xmlConfig);
				        		reader.close();
			        		} catch (IOException e1){
				        		Log.w(LOG_TAG, "Config read failure. Buffered reader error;");
				        		gotErrors = true;
				        		e1.printStackTrace();
				        	}
			        	}
		        		
		        	} 
		        	else {
		        		sdPath.mkdir();
		        		gotErrors = true;
		        	}
		        	
		        	if (gotErrors) {
		        		Log.w(LOG_TAG, "Reading configs from local resources;");
		        		xmlConfig = getLocalConfigsXml();
			        	readConfigsFromXml(xmlConfig);
		        	}
		        	
		        } 
				else {
		        	Log.w(LOG_TAG, "Reading configs from local resources;");
		        	xmlConfig = getLocalConfigsXml();
		        	readConfigsFromXml(xmlConfig);
		        }
				
			} catch (XmlPullParserException e2) {
				Log.w(LOG_TAG, "Config read failure. Xml pull parser error: Unknown error;");
				e2.printStackTrace();
			}
		}
		private boolean moveLocalConfigsToSd() {			
			if (storageIsAvailable()) {
				File sdPath = Environment.getExternalStorageDirectory();
		    	sdPath = new File(sdPath.getAbsolutePath() + "/" 
		    						+ APP_DIRECTORY + "/" 
		    						+ CONFIG_NAME);
		    	
		    	if (!sdPath.exists()) {
			    	try {
						FileOutputStream outputStream = new FileOutputStream(sdPath);
		        		AssetManager assetManager = appResources.getAssets();
		        		InputStream inputStream = assetManager.open(CONFIG_NAME);
		        		int blockSize = 1024;
		        		byte [] buffer = new byte [blockSize];
		        		int readBytes = 0;
		        		while ((readBytes = inputStream.read(buffer)) != -1) {
		        			outputStream.write(buffer, 0, readBytes);
		        		}
		        		inputStream.close();
		        		outputStream.flush();
		        		outputStream.close();
		        		return true;
		        	} catch (FileNotFoundException e) {
		        		Log.e(LOG_TAG, "Configs copy error. File reader error: File not found;");
		        		e.printStackTrace();
		        	} catch (IOException e) {
		        		Log.e(LOG_TAG, "Configs copy error. IO error;");
						e.printStackTrace();
					}
		    	}
	        }
			return false;
		}
		
		private XmlPullParser getLocalConfigsXml() {
			AssetManager assetManager = appResources.getAssets();
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				XmlPullParser xmlConfigs = factory.newPullParser();
				InputStream inputStream = assetManager.open(CONFIG_NAME);
				xmlConfigs.setInput(inputStream, null);
	    		return xmlConfigs;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
			return null;
			
		}
		private void readConfigsFromXml(XmlPullParser xmlConfig) {
			
			try {
				
				while (xmlConfig.getEventType() != XmlPullParser.END_DOCUMENT) {
					switch (xmlConfig.getEventType()) {
						case XmlPullParser.START_TAG:
							String tagName = xmlConfig.getName();
				    		//--------------------------------MAPS_DIRECTORY----------------------------------
							if (tagName.contentEquals("maps")) {
								mapsDirectory = xmlConfig.nextText();
							}
				    		//--------------------------------IMAGES_DIRECTORY----------------------------------
							else if (tagName.contentEquals("images")) {
								imagesDirectory = xmlConfig.nextText();
							}
				    		//--------------------------------SOUNDS_DIRECTORY----------------------------------
							else if (tagName.contentEquals("sounds")) {
								soundsDirectory = xmlConfig.nextText();
							}
						break;
		    		}
					xmlConfig.next();
		    	}        
				if (storageIsAvailable()) {
					String [] directories = new String [] {	mapsDirectory, 
															imagesDirectory, 
															soundsDirectory };
					File sdPath;
					for (int i = 0; i < directories.length; i++) {
			        	sdPath = Environment.getExternalStorageDirectory();
			        	sdPath = new File(sdPath.getAbsolutePath() + "/" 
			        								+ APP_DIRECTORY + "/" 
			        								+ directories[i]);
			        	if (!sdPath.exists()) { 
			        		sdPath.mkdir();
			        	}
					}
				}
				
				xmlConfig.setInput(null);
				
		    } catch (XmlPullParserException e) {
		    	Log.w(LOG_TAG, "Xml pull parser error while config reading: " 
		    			+ "XmlPullParserException; "
		    			+ "It might be a call to xmlConfig.setInput(null) to free buffers;");
		    	e.printStackTrace();
		    } catch (IOException e) {
		    	Log.w(LOG_TAG, "Xml pull parser error while config reading: IOException;");
		    	e.printStackTrace();
		    }				
		}
	}
	//^^----------------------------------------Configs------------------------------------------^^
	
	//vv-------------------------------------Sprites-Images--------------------------------------vv	
	private ArrayList <Sprite> lstSprites = new ArrayList <Sprite> ();

	public Sprite getSprite(String spriteName, int collumnsNum, int rawsNum) {
		
		for (Sprite sprite : lstSprites) {
			if (sprite.name.contentEquals(spriteName)) {
				return sprite;
			}
		}
		
		Sprite newSprite = new Sprite(getImage(spriteName), spriteName, collumnsNum, rawsNum);
		
        lstSprites.add(newSprite);
        
        return newSprite;
        
	}
	public Bitmap getImage(String imageName) {
		imageName = imageName.replaceFirst(".png", "") + ".png";
		if (storageIsAvailable()) {
			try {
		    	File sdPath = Environment.getExternalStorageDirectory();
		    	sdPath = new File(sdPath.getAbsolutePath() + "/" 
		    								+ APP_DIRECTORY + "/" 
		    								+ mConfiguration.imagesDirectory);
		    	
		    	File sdFile = new File(sdPath, imageName); 
		    	Bitmap bitmap = BitmapFactory.decodeFile(sdFile.getAbsolutePath());
		    	if (bitmap != null) {
		    		return bitmap;
		    	} else {
					Log.w(LOG_TAG, "Cant load image from SD: " + imageName 
									+ "; null pointer is returned;");
		    	}
		        
			} catch (Exception e) {
				Log.w(LOG_TAG, "Cant load image from SD: " + imageName + "; Exception;");
				e.printStackTrace();
			} 
		}
		Log.w(LOG_TAG, "Searching for image " + imageName + " in local resorces;");
		return getImageFromLocalRes(imageName);
	}
	
	public Bitmap getImageFromLocalRes(String imageName) {
		int imageId = appResources.getIdentifier(imageName.replaceFirst(".png", ""), 
						"drawable", appResources.getString(R.string.my_package_name));
    	if (imageId != 0) return BitmapFactory.decodeResource(appResources, imageId);
    	else {
    		Log.w(LOG_TAG, "Cant load image <" + imageName + "> from local resources;");
    		return getImageFromLocalRes(GameResources.IMAGE_NOT_FOUND);
    	}
	}
	//^^-------------------------------------Sprites-Images--------------------------------------^^
	
	//vv------------------------------------------MAPS-------------------------------------------vv
	public String[] getLocalMapNames() {
		AssetManager manager = appResources.getAssets();
		String [] mapNames = null;
		try {
			mapNames = manager.list("xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mapNames;
	}
	
	public String[] getSdMapNames() {
		String [] mapNames = null;
    	if (storageIsAvailable()) {    	
	    	File sdPath = Environment.getExternalStorageDirectory();
	    	sdPath = new File(sdPath.getAbsolutePath() + "/" 
	    								+ APP_DIRECTORY + "/" 
	    								+ mConfiguration.mapsDirectory);
	    	
	    	mapNames = sdPath.list();
    	}
		return mapNames;
	}
	
	public XmlPullParser getSdMap(String mapName) {
		
		if (storageIsAvailable()) {
			mapName = mapName.replaceFirst(".xml", "") + ".xml";
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				XmlPullParser xmlMap = factory.newPullParser();
				
		    	File sdPath = Environment.getExternalStorageDirectory();
		    	sdPath = new File(sdPath.getAbsolutePath() + "/" 
		    								+ APP_DIRECTORY + "/" 
		    								+ mConfiguration.mapsDirectory);
		    	
		    	File sdFile = new File(sdPath, mapName);
		    	
	        	try {
	        		
	        		BufferedReader reader = new BufferedReader(new FileReader(sdFile));
	        		xmlMap.setInput(reader);
	        		return xmlMap;
	        		
	        	} catch (FileNotFoundException e) {
	        		Log.w(LOG_TAG, "Buffered reader failure. File " + sdFile 
	        				+ " not found. Searching for map in local resorces;");
	        		e.printStackTrace();
	        		
	        		return getLocalMap(mapName);
	        	} 
			} catch (XmlPullParserException e2) {
				Log.w(LOG_TAG, "Load map failure; Xml pull parser error: Unknown error;");
				e2.printStackTrace();
			}		
		} 
		else {
			Log.w(LOG_TAG, "Searching for map in local resorces;");
        	return getLocalMap(mapName);
		}
		return null;
	}

	public XmlPullParser getLocalMap(String mapName) {
		AssetManager assetManager = appResources.getAssets();
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser xmlMap = factory.newPullParser();
			InputStream inputStream = assetManager.open("xml/" + mapName);
			xmlMap.setInput(inputStream, null);
    		return xmlMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean writeMapToSd(String localMapName) {
		if (storageIsAvailable()) {
			File sdPath = Environment.getExternalStorageDirectory();
	    	sdPath = new File(sdPath.getAbsolutePath() + "/" 
	    						+ APP_DIRECTORY + "/" 
	    						+ mConfiguration.mapsDirectory + "/" 
	    						+ localMapName);
	    	
	    	if (!sdPath.exists()) {
		    	try {
					FileOutputStream outputStream = new FileOutputStream(sdPath);
	        		AssetManager assetManager = appResources.getAssets();
	        		InputStream inputStream = assetManager.open("xml/" + localMapName);
	        		int blockSize = 1024;
	        		byte [] buffer = new byte [blockSize];
	        		int readBytes = 0;
	        		while ((readBytes = inputStream.read(buffer)) != -1) {
	        			outputStream.write(buffer, 0, readBytes);
	        		}
	        		inputStream.close();
	        		outputStream.flush();
	        		outputStream.close();
	        		return true;
	        	} catch (FileNotFoundException e) {
	        		Log.e(LOG_TAG, "Map copy error. File reader error: File not found;");
	        		e.printStackTrace();
	        	} catch (IOException e) {
	        		Log.e(LOG_TAG, "Map copy error. IO error;");
					e.printStackTrace();
				}
	    	}
        }
		return false;
	}
	
	public boolean removeMapFromSd(String mapName) {
		if (storageIsAvailable()) {
			File sdPath = Environment.getExternalStorageDirectory();
	    	sdPath = new File(sdPath.getAbsolutePath() + "/" 
	    								+ APP_DIRECTORY + "/" 
	    								+ mConfiguration.mapsDirectory + "/" 
	    								+ mapName);
	    	
	    	if (sdPath.exists()) {
				sdPath.delete();
				return true;
	    	}
        }
		return false;
	}
	//^^------------------------------------------MAPS-------------------------------------------^^
	private boolean storageIsAvailable() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        	Log.w(LOG_TAG, "SD-card is not available now: " 
        					+ Environment.getExternalStorageState() + ";");
        	return false;
        }
		return true;
	}
	
}
