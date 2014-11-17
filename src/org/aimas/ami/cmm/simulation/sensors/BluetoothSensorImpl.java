package org.aimas.ami.cmm.simulation.sensors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.service.location.PersonLocationService;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;
import fr.liglab.adele.icasa.simulator.SimulationManager;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

@Component(name = "iCasa.BluetoothSensor")
@Provides(properties = {@StaticServiceProperty(type="java.lang.String", name="service.description")})
public class BluetoothSensorImpl extends AbstractDevice implements BluetoothSensor, SimulatedDevice, PersonListener {
	
	@Requires
	private PersonLocationService personLocationService;
	
	@Requires 
	private SimulationManager simulationManager;
	
	@ServiceProperty(name=Microphone.DEVICE_SERIAL_NUMBER, mandatory=true)
	private String serialNumber;
	
	private volatile Zone zone;
	
	/** Holds the mapping from person-id to an auto-generated skeleton name */
	private volatile Map<String, String> personToAddressMap;
	private volatile Set<String> sensedAddresses;
	
	public BluetoothSensorImpl() {
		super();
		
        // Property initialization
        super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);
        
        personToAddressMap = new HashMap<String, String>();
        sensedAddresses = new HashSet<String>();
        
        // Property initialization
        setPropertyValue(BluetoothSensor.SENSED_ADDRESSES, sensedAddresses);
        
        simulationManager.addListener(this);
	}
	
	@Override
	public String getSerialNumber() {
		return serialNumber;
	}
	
	
	@Override
	public void enterInZones(List<Zone> zones) {
		if (!zones.isEmpty()) {
			for (Zone z : zones) {
				if (zone == null) {
					zone = z;
				}
				else if (z.getXLength() * z.getYLength() > zone.getXLength() * zone.getYLength()) {
					zone = z;
				}
			}
			
			updateSensedAddresses();
		}
	}
	
	@Override
	public void leavingZones(List<Zone> zones) {
		setPropertyValue(BluetoothSensor.SENSED_ADDRESSES, null);
	}
	
	@Override
	public Set<String> getSensedAddresses() {
		return (Set<String>) getPropertyValue(BluetoothSensor.SENSED_ADDRESSES);
	}
	
	
	private void updateSensedAddresses() {
		if (zone != null) {
			sensedAddresses.clear();
			
			Set<String> allDetectedPersons = new HashSet<String>();
			Set<String> detectedPersons = personLocationService.getPersonInZone(zone.getId());
			if (detectedPersons != null) {
				allDetectedPersons.addAll(detectedPersons);
			}
			
			for (Zone z : zone.getChildren()) {
				detectedPersons = personLocationService.getPersonInZone(z.getId());
				
				if (detectedPersons != null) {
					allDetectedPersons.addAll(detectedPersons);
				}
			}
			
			for (String personId : allDetectedPersons) {
				if (staticBluetoothMap.containsKey(personId)) {
					personToAddressMap.put(personId, staticBluetoothMap.get(personId));
					sensedAddresses.add(staticBluetoothMap.get(personId));
				}
				else {
					String address = personToAddressMap.get(personId);
					if (address == null) {
						address = generateAddress();
						personToAddressMap.put(personId, address);
					}
					
					sensedAddresses.add(address);
				}
			}
			
			setPropertyValue(BluetoothSensor.SENSED_ADDRESSES, sensedAddresses);
		}
	}
	
	
	// PERSON LISTENING
	///////////////////////////////////////////////////////////////////////////////////////
	@Override
    public void personAdded(Person person) {
		updateSensedAddresses();    
    }
	
	@Override
    public void personMoved(Person person, Position position) {
		updateSensedAddresses();
    }

	@Override
    public void personRemoved(Person person) {
	    updateSensedAddresses();
    }
	
	@Override
    public void personDeviceAttached(Person person, LocatedDevice device) {}

	@Override
    public void personDeviceDetached(Person person, LocatedDevice device) {}
	
	// STATIC PERSON TO ADDRESS MAPPING
	/////////////////////////////////////////////////////////////////////////////////////////
	private static final String DEFAULT_ADDRESS_BASE = "01:23:45:67:86";
	private static Map<String, String> staticBluetoothMap;
	static {
		staticBluetoothMap = new HashMap<String, String>();
		staticBluetoothMap.put("Alice", "01:23:45:67:89:ab");
		staticBluetoothMap.put("Bob", "01:23:45:67:89:ac");
		staticBluetoothMap.put("Cecille", "01:23:45:67:89:ad");
	}
	
	static int addressCounter = 0x00;
	private static String generateAddress() {
		addressCounter = (addressCounter + 1) % 256;
		return DEFAULT_ADDRESS_BASE + ":" + String.format("%02X", addressCounter & 0xFF);
	}
}
