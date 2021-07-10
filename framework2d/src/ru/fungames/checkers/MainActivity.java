package ru.fungames.checkers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ru.framework2d.core.GameResources;
import ru.fungames.checkers.R;

public class MainActivity extends Activity implements OnClickListener {

	public static final String MAP_NAME = "mapName";
	public static final String IS_MAP_FROM_SD = "fromSd";
	public static final String IS_PARALLEL = "parallelWork";

	private GameResources resources;
	
	private Button btnPlay;
	private Button btnSdOperations;
	private TextView tvChoosenMap;
	private ExpandableListView elvMapChoice;
	private ImageView imgvMapPreview;
	
	private SimpleExpandableListAdapter adapter;
	
	private String currentMap = "checkersclear.xml";
	private boolean mapLoadedFromSd = true;
	private boolean parallelWork = true;
	
	private boolean isInDeleteMode = false;
	
	private Bitmap mapPreview = null;

	private static final String LOG_TAG = "core";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnSdOperations = (Button) findViewById(R.id.btnSdOperations);
    	btnPlay.setOnClickListener(this);
    	btnSdOperations.setOnClickListener(this);
    	
        tvChoosenMap = (TextView) findViewById(R.id.tvChoosenMapName);
    	elvMapChoice = (ExpandableListView) findViewById(R.id.lstvMaps);
        imgvMapPreview = (ImageView) findViewById(R.id.imgvMapPreview);
        
        resources = new GameResources(getResources());
        
        adapter = createMapsAdapter();
        
        elvMapChoice.setAdapter(adapter);
        
        elvMapChoice.setOnChildClickListener(new OnChildClickListener() {

			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				currentMap = ((Map <String, String>) adapter.getChild(
														groupPosition, 
														childPosition)).get("mapName");
				tvChoosenMap.setText(currentMap);
				mapLoadedFromSd = (groupPosition == 1);
				Log.d(LOG_TAG, "Chosen map name: " + currentMap + ";");
				
	    		XmlPullParser map = null;
	    		if (mapLoadedFromSd) map = resources.getSdMap(currentMap);
	    		else map = resources.getLocalMap(currentMap);
	    		setConfigsFromMap(map);
	    		
		    	btnPlay.setEnabled(true);
				if (groupPosition == 0) {
					btnSdOperations.setText(getResources().getString(R.string.title_to_sd));
					isInDeleteMode = false;
				} 
				else {
					btnSdOperations.setText(getResources().getString(R.string.title_delete));
					isInDeleteMode = true;
				}
				btnSdOperations.setVisibility(View.VISIBLE);
				btnSdOperations.setEnabled(true);
				btnSdOperations.setClickable(true);
				return false;
			}
        	
        });
        
        elvMapChoice.expandGroup(0);
        
    }
    
	public void onClick(View view) {
		switch (view.getId()) {
			case (R.id.btnPlay): {
		    	Log.d(LOG_TAG, "Button 'play' is pressed; Chosen map is: " + currentMap + ";");
		    	Intent intent = new Intent(this, PlayGround.class);
		    	intent.putExtra(MAP_NAME, currentMap);
		    	intent.putExtra(IS_MAP_FROM_SD, mapLoadedFromSd);
		    	intent.putExtra(IS_PARALLEL, parallelWork);
		        startActivity(intent);
			}
			break;
			case (R.id.btnSdOperations): {
		    	Log.d(LOG_TAG, "Button 'move to sd/remove' is pressed; Chosen map is: " 
		    														+ currentMap + ";");
		    	if (isInDeleteMode) {
		    		resources.removeMapFromSd(currentMap);
			    	tvChoosenMap.setText(getResources().getString(R.string.map_name));
			    	btnPlay.setEnabled(false);
					btnSdOperations.setVisibility(View.INVISIBLE);
					btnSdOperations.setEnabled(false);
					btnSdOperations.setClickable(false);
				} 
		    	else {
		    		resources.writeMapToSd(currentMap);
		    	}
		    	
		    	//change adapter instead of creating new one
	            adapter = createMapsAdapter();
	            elvMapChoice.setAdapter(adapter);

	            elvMapChoice.expandGroup(0);
	            elvMapChoice.expandGroup(1);
	            
			}
			break;
		}
	}
	
	@SuppressLint("DefaultLocale")
	private Bitmap setConfigsFromMap(XmlPullParser map) {
		try {
			
			String previewName = "";
			boolean gotPreviewName = false;
			while ((map.getEventType() != XmlPullParser.END_DOCUMENT) && !gotPreviewName) {
				switch (map.getEventType()) {
					case XmlPullParser.START_TAG:
						String tagName = map.getName().toLowerCase();
						if (tagName.contentEquals("map")) {
							mapPreview = null;
	        				for (int i = 0; i < map.getAttributeCount(); i++) {
	        					if (map.getAttributeName(i).contentEquals("preview")) {
	        						previewName = map.getAttributeValue(i);
	        						if (previewName.length() > 0) {
	        							//mapPreview = resources.getImage(previewName);
	        							mapPreview = resources.getImage(previewName);//.lstSprites.get(0);
	        						} 
	        					} 
	        					else if (map.getAttributeName(i).contentEquals("name")) {
	        						tvChoosenMap.setText(map.getAttributeValue(i));
	        					} 
	        					else if (map.getAttributeName(i).contentEquals("parallel")) {
	        						parallelWork = Boolean.parseBoolean(map.getAttributeValue(i));
	        					}
	        				}
							if (mapPreview != null) {
								imgvMapPreview.setImageBitmap(mapPreview);
								Log.d(LOG_TAG, "Activity: map got preview: " + previewName + ";");
							}
							else {
								imgvMapPreview.setImageBitmap(resources.getImageFromLocalRes(
																GameResources.IMAGE_NOT_FOUND));
							}
						}
					break;
	    		}
				map.next();
	    	}        
			
	    } catch (XmlPullParserException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }				
		return null;
	}
	private SimpleExpandableListAdapter createMapsAdapter() {
		
        String [] groupNames = new String [] {"local", "sd"};
        String [] localMapNames = resources.getLocalMapNames();
        String [] sdMapNames = resources.getSdMapNames();
        
        Log.d(LOG_TAG, "Activity: mapNames: " + localMapNames[0] + ";");
        
        ArrayList <Map <String, String>> groupData = new ArrayList <Map <String, String>>();
        
        Map <String, String> attributes;
        
        for (String group : groupNames) {
        	attributes = new HashMap <String, String>();
        	attributes.put("groupName", group);
        	groupData.add(attributes);
        }
        
        String [] groupFrom = new String [] {"groupName"};
        int groupTo [] = new int [] {android.R.id.text1};

        ArrayList <ArrayList <Map <String, String>>> maps 
        								= new ArrayList <ArrayList <Map <String, String>>>();
        ArrayList <Map <String, String>> localMaps = new ArrayList <Map <String, String>>();
        ArrayList <Map <String, String>> sdMaps = new ArrayList <Map <String, String>>();

        for (String localMapName : localMapNames) {
        	attributes = new HashMap <String, String>();
        	attributes.put("mapName", localMapName);
        	localMaps.add(attributes);
        }
        maps.add(localMaps);
        for (String sdMapName : sdMapNames) {
        	attributes = new HashMap <String, String>();
        	attributes.put("mapName", sdMapName);
        	sdMaps.add(attributes);
        }
        maps.add(sdMaps);
        
        String [] childFrom = new String [] {"mapName"};
        int childTo [] = new int [] {android.R.id.text1};
        
        
        return new SimpleExpandableListAdapter(
        		this, 
        		groupData, 
        		android.R.layout.simple_expandable_list_item_1, 
        		groupFrom, groupTo, 
        		maps, 
        		android.R.layout.simple_list_item_1, 
        		childFrom, childTo);
	} 
	
}

