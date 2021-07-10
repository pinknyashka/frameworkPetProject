package ru.framework2d.core;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;

import android.util.Log;

import android.view.SurfaceView;

public class GameCore {
	
	public EntityStorage entityStorage;
    
	public EnginesManager enginesManager;
    
	public GameResources gameResources;
    
	public MapParser mapManager;
    
	private static final String LOG_TAG = "core";
    //----------------------------------------------------
	
	public GameCore(Resources resources) {
		
        //vv-------------------------------------Initialization------------------------------------vv
		
		Log.d(LOG_TAG, "Core initialization...");
		
        /* 
         * connect resources
         */
        gameResources = new GameResources(resources);
        
        entityStorage = new EntityStorage();
        
        enginesManager = new EnginesManager(entityStorage, gameResources);
        
        /*
         * creating XML files manager 
         */
        mapManager = new MapParser(entityStorage, enginesManager, gameResources);
        
        Log.d(LOG_TAG, "Core initialization is complete;");
        
        //^^-------------------------------------Initialization------------------------------------^^
    }
	
	//vv-------------------------------------GAME_SURFACE------------------------------------vv
    /* primary view to output graphic
     */
	private GameSurface gameSurface;
	
	public SurfaceView createSurface(Context context) {
		
		Log.d(LOG_TAG, "Generating surface...");
		
		gameSurface = new GameSurface(context, enginesManager);
		gameResources.gameSurface = gameSurface; 
		
		Log.d(LOG_TAG, "Surface created;");
		return gameSurface;
	}
	
	public SurfaceView getGameSurface() {
		return gameSurface;
	}    
	//^^-------------------------------------GAME_SURFACE------------------------------------^^

	public void createObjectsModel(String mapName, boolean mapIsFromSd) {
		
        Log.d(LOG_TAG, "Creating of the objects model...");
        
        XmlPullParser map = (mapIsFromSd) ? 
        						gameResources.getSdMap(mapName) : 
        						gameResources.getLocalMap(mapName);

        /* 
         * Parsing the map into object model
         */
        if (EnginesManager.PARALLEL_WORK) {
            mapManager.parallelParseMap(map);
        } 
        else {
            mapManager.parseMap(map);
        }
        
        Log.d(LOG_TAG, "Creating of the objects model is complete;");
	}
}

