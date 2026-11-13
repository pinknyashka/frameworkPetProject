package ru.framework2d.sound;

import ru.framework2d.core.Component;

import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.data.Trigger;

public class AudioComponent extends Component {

	public final static String SHORT_CLASS_NAME = "audio";

	//the collision signal from the physical engine; shares a context with PhysicalShape.collision
	public Trigger collision = new Trigger();

	//the sound played when the collision signal arrives
	public Text sound = new Text("bump");

	//comma-separated sound names used instead of sound when playback is "random"
	public Text sounds = new Text("");

	//playback rule: "single" plays sound, "random" picks one name from sounds
	public Text playback = new Text("single");

	//playback volume, 0..1
	public Fractional volume = new Fractional(1.0f);

	//relative collision speed (px per base interval) that plays the sound at full volume;
	//slower collisions ramp the volume down to minVolume
	public Fractional fullVolumeSpeed = new Fractional(10.0f);

	//volume factor (0..1) of the slowest collision
	public Fractional minVolume = new Fractional(0.15f);

	//maximum number of collision sounds started by this component inside parallelWindow
	public Numeral parallelSounds = new Numeral(1);

	//collision window in milliseconds used by parallelSounds; 0 disables the window
	public Numeral parallelWindow = new Numeral(50);

	//what drives this component: "collision" reacts to the physics collision signal,
	//"motion" follows the movement of the entity and loops the sound while the speed is non-zero
	public Text source = new Text("collision");

	//movement speed (px per base interval) at which the entity counts as moving for source "motion";
	//slower movement stops the sound
	public Fractional motionSpeed = new Fractional(0.05f);

	//maximum number of entities that may play motion sounds of this kind at the same time
	public Numeral motionSounds = new Numeral(3);

	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
}
