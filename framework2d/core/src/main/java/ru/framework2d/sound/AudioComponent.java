package ru.framework2d.sound;

import ru.framework2d.core.Component;

import ru.framework2d.data.Fractional;
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

	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
}
