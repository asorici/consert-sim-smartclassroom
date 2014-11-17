package org.aimas.ami.cmm.simulation.sensors;

import fr.liglab.adele.icasa.device.GenericDevice;

public interface Microphone extends GenericDevice {
	public static String CURRENT_NOISE_LEVEL = "microphone.noiseLevel";
	
	public int getNoiseLevel();
}
