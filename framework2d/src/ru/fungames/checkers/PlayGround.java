package ru.fungames.checkers;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.WindowManager;

import ru.framework2d.core.EnginesManager;
import ru.framework2d.core.GameCore;

public class PlayGround extends Activity {

	public GameCore gameCore;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String mapName = getIntent().getStringExtra(MainActivity.MAP_NAME);
        boolean mapLoadedFromSd = getIntent().getBooleanExtra(MainActivity.IS_MAP_FROM_SD, false);
        EnginesManager.PARALLEL_WORK = getIntent().getBooleanExtra(MainActivity.IS_PARALLEL, false);
        
		gameCore = new GameCore(this.getResources());
		gameCore.createObjectsModel(mapName, mapLoadedFromSd);
		
        boolean enableAccelerationFlag = true; //static sent from main activity was deprecated
        
        if (enableAccelerationFlag) {
        	// Build target in AndroidManifest.xml and/or Eclipse must be 11 or 
        	// higher to compile this code.
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(gameCore.createSurface(this));
        
        if (	android.os.Build.VERSION.SDK_INT 
        		> android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
        	gameCore.getGameSurface().setWillNotDraw(false);
        }
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}
