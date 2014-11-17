package org.aimas.ami.cmm.simulation.sensors;

import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.location.ZoneListener;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;
import fr.liglab.adele.icasa.simulator.listener.util.BaseZoneListener;

@Component(name = "iCasa.Microphone")
@Provides(properties = {@StaticServiceProperty(type="java.lang.String", name="service.description")})
public class MicrophoneImpl extends AbstractDevice implements Microphone, SimulatedDevice {
	
	@ServiceProperty(name=Microphone.DEVICE_SERIAL_NUMBER, mandatory=true)
	private String serialNumber;
	
	private volatile Zone zone;
	private ZoneListener zoneListener = new MicrophoneZoneListener();
	
	
	public MicrophoneImpl() {
        super();

        // Property initialization
        super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);

        // Property initialization
        setPropertyValue(Microphone.CURRENT_NOISE_LEVEL, 0); 
    }
	
	
	@Override
	public String getSerialNumber() {
		return serialNumber;
	}
	
	
	@Override
	public void enterInZones(List<Zone> zones) {
		if (!zones.isEmpty()) {
			for (Zone z : zones) {
				// Since a microphone is static, and is attached to only one zone, 
				// we can stop searching after the first hit
				if (z.getVariableValue("Temperature") != null) {
					zone = z;
					getNoiseLevelFromZone();
					
					// Zone listener registration
					zone.addListener(zoneListener);
					break;
				}
			}
		}
	}
	
	@Override
	public void leavingZones(List<Zone> zones) {
		setPropertyValue(Microphone.CURRENT_NOISE_LEVEL, null);
		
		// Zone listener unregistration
		if (zone != null) {
			zone.removeListener(zoneListener);
		}
	}
	
	
	@Override
    public int getNoiseLevel() {
	    Double noiseLevel = (Double) getPropertyValue(Microphone.CURRENT_NOISE_LEVEL);
	    return noiseLevel == null ? 0 : (int)Math.round(noiseLevel);
    }
	
	private void getNoiseLevelFromZone() {
		if (zone != null) {
			Object currentNoiseLevel = zone.getVariableValue("NoiseLevel");
			if (currentNoiseLevel != null) {
				setPropertyValue(Microphone.CURRENT_NOISE_LEVEL, currentNoiseLevel);
			}
		}
	}
	
	// ================================================================================================
	private class MicrophoneZoneListener extends BaseZoneListener {
		
		@Override
        public void zoneVariableModified(Zone modifiedZone, String variableName, Object oldValue, Object newValue) {

            if (zone == modifiedZone) {
                if (!(getFault().equalsIgnoreCase("yes"))) {
                    if (variableName.equals("NoiseLevel")) {
                        getNoiseLevelFromZone();
                    }
                }
            }
        }
	}
}
