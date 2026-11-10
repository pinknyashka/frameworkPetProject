package ru.framework2d.core;

import org.xmlpull.v1.XmlPullParser;

import ru.framework2d.compat.Log;

public class GameCore {

    public EntityStorage entityStorage;

    public EnginesManager enginesManager;

    public GameResources gameResources;

    public MapParser mapManager;

    private static final String LOG_TAG = "core";
    //----------------------------------------------------

    public GameCore() {

        //vv-------------------------------------Initialization------------------------------------vv

        Log.d(LOG_TAG, "Core initialization...");

        gameResources = new GameResources();

        entityStorage = new EntityStorage();

        enginesManager = new EnginesManager(entityStorage, gameResources);

        /*
         * creating XML files manager
         */
        mapManager = new MapParser(entityStorage, enginesManager, gameResources);

        Log.d(LOG_TAG, "Core initialization is complete;");

        //^^-------------------------------------Initialization------------------------------------^^
    }

    public void createObjectsModel(String mapName) {

        Log.d(LOG_TAG, "Creating of the objects model...");

        XmlPullParser map = gameResources.getLocalMap(mapName);

        /*
         * Parsing the map into object model
         */
        if (EnginesManager.PARALLEL_WORK) {
            mapManager.parallelParseMap(map);
        } else {
            mapManager.parseMap(map);
        }

        Log.d(LOG_TAG, "Creating of the objects model is complete;");
    }
}
