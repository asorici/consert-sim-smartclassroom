package org.aimas.ami.cmm.simulation;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;

public class SimulationUsers {
	public static final String ALICE_NAME = "Alice";
	public static final String BOB_NAME = "Bob";
	public static final String CECILLE_NAME = "Cecille";
	public static final String DAN_NAME = "Dan";
	public static final String AIRCONDITIONING_NAME = "AirConditioning";
	public static final String PROJECTOR_NAME = "Projector";
	
	public static Map<String, Factory> userComponentFactoryMap;
	public static Map<String, ComponentInstance> userComponentInstanceMap;
	static {
		userComponentFactoryMap = new HashMap<String, Factory>();
		userComponentInstanceMap = new HashMap<String, ComponentInstance>();
	}
}
