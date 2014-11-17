package org.aimas.ami.cmm.simulation.sensors;

import java.util.Set;

import fr.liglab.adele.icasa.device.GenericDevice;

public interface BluetoothSensor extends GenericDevice {
	public static String SENSED_ADDRESSES = "bluetooth.sensedAddresses";
	
	public Set<String> getSensedAddresses();
}
