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

	//playback volume, 0..1
	public Fractional volume = new Fractional(1.0f);

	@Override
	public String getShortClassName() {
		return SHORT_CLASS_NAME;
	}
}
