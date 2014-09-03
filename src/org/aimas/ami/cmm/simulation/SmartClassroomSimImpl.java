package org.aimas.ami.cmm.simulation;

import java.util.HashMap;
import java.util.Map;

import fr.liglab.adele.icasa.device.DeviceListener;
import fr.liglab.adele.icasa.device.GenericDevice;
import fr.liglab.adele.icasa.device.light.Photometer;
import fr.liglab.adele.icasa.device.presence.PresenceSensor;
import fr.liglab.adele.icasa.device.temperature.Thermometer;

public class SmartClassroomSimImpl implements DeviceListener {
	
	/** Field for luminositySensors dependency */
	private Photometer[] luminositySensors;
	
	/** Field for presenceSensor dependency */
	private PresenceSensor presenceSensor;
	
	/** Field for temperatureSensors dependency */
	private Thermometer[] temperatureSensors;
	
	private Map<String, Thermometer> temperatureSensorMap;
	private Map<String, Photometer> luminositySensorMap;
	
	
	/* ==================================== DEVICE-SPECIFIC BINDING ======================================== */
	/** Bind Method for luminositySensors dependency */
	public void bindLuminositySensor(Photometer photometer, Map properties) {
		luminositySensorMap.put(photometer.getSerialNumber(), photometer);
		photometer.addListener(this);
	}
	
	/** Unbind Method for luminositySensors dependency */
	public void unbindLuminositySensor(Photometer photometer, Map properties) {
		photometer.removeListener(this);
		luminositySensorMap.remove(photometer.getSerialNumber());
	}
	
	/** Bind Method for temperatureSensors dependency */
	public void bindTemperatureSensor(Thermometer thermometer, Map properties) {
		temperatureSensorMap.put(thermometer.getSerialNumber(), thermometer);
		thermometer.addListener(this);
	}
	
	/** Unbind Method for temperatureSensors dependency */
	public void unbindTemperatureSensor(Thermometer thermometer, Map properties) {
		thermometer.removeListener(this);
		temperatureSensorMap.remove(thermometer.getSerialNumber());
	}
	
	/* ====================================== LIFE CYCLE IMPLEMENTATION ========================================== */
	/** Component Lifecycle Method */
	public void start() {
		System.out.println("[INFO] Smart Classroom Simulator starting");
		temperatureSensorMap = new HashMap<String, Thermometer>();
		luminositySensorMap = new HashMap<String, Photometer>();
	}
	
	/** Component Lifecycle Method */
	public void stop() {
		System.out.println("[INFO] Smart Classroom Simulator stopping");
		temperatureSensorMap = null;
		luminositySensorMap = null;
	}
	
	
	/* ==================================== DEVICE LISTENER IMPLEMENTATION ======================================== */
	@Override
    public void deviceAdded(GenericDevice device) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void deviceEvent(GenericDevice device, Object event) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void devicePropertyAdded(GenericDevice device, String property) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void devicePropertyModified(GenericDevice device, String property, Object oldValue, Object newValue) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void devicePropertyRemoved(GenericDevice device, String property) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void deviceRemoved(GenericDevice device) {
	    // TODO Auto-generated method stub
	    
    }
	
}
