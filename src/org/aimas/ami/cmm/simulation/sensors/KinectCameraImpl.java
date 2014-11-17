package org.aimas.ami.cmm.simulation.sensors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.location.ZoneListener;
import fr.liglab.adele.icasa.service.location.PersonLocationService;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;
import fr.liglab.adele.icasa.simulator.listener.util.BaseZoneListener;

@Component(name = "iCasa.KinectCamera")
@Provides(properties = {@StaticServiceProperty(type="java.lang.String", name="service.description")})
public class KinectCameraImpl extends AbstractDevice implements KinectCamera, SimulatedDevice {
	
	@Requires
	private PersonLocationService personLocationService;
	
	@ServiceProperty(name=Microphone.DEVICE_SERIAL_NUMBER, mandatory=true)
	private String serialNumber;
	
	private volatile Zone zone;
	private ZoneListener zoneListener = new KinectZoneListener();
	
	/** Holds the mapping from person-id to an auto-generated skeleton name */
	private volatile Map<String, String> personToSkeletonMap;
	private volatile Map<String, String> skeletonPositionMap;
	
	public KinectCameraImpl() {
        super();

        // Property initialization
        super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);
        
        
        personToSkeletonMap = new HashMap<String, String>();
        skeletonPositionMap = new HashMap<String, String>();
        
        // Property initialization
        setPropertyValue(KinectCamera.SENSED_SKELETONS, skeletonPositionMap); 
    }
	
	@Override
	public String getSerialNumber() {
		return serialNumber;
	}
	
	@Override
	public void enterInZones(List<Zone> zones) {
		if (!zones.isEmpty()) {
			// Since a Kinect camera is static, and is attached to only one zone, 
			// the required one is the most specific (i.e. small one)
			for (Zone z : zones) {
				if (zone == null) {
					zone = z;
				}
				else if (z.getXLength() * z.getYLength() < zone.getXLength() * zone.getYLength()) {
					zone = z;
				}
			}
			
			senseSkeletons();
			
			// Zone listener registration
			zone.addListener(zoneListener);
		}
	}
	
	@Override
	public void leavingZones(List<Zone> zones) {
		setPropertyValue(KinectCamera.SENSED_SKELETONS, null);
		
		// Zone listener unregistration
		if (zone != null) {
			zone.removeListener(zoneListener);
		}
	}
	
	@SuppressWarnings("unchecked")
    @Override
	public Map<String, String> getSensedSkeletons() {
		return (Map<String, String>) getPropertyValue(KinectCamera.SENSED_SKELETONS);
	}
	
	
	private void senseSkeletons() {
		if (zone != null && personLocationService != null) {
			skeletonPositionMap.clear();
			
			Set<String> detectedPersons = personLocationService.getPersonInZone(zone.getId());
			if (detectedPersons != null) {
				for (String personId : detectedPersons) {
					// STEP 1: determine the person that is in this zone
					String personSkeleton = personToSkeletonMap.get(personId);
					if (personSkeleton == null) {
						personSkeleton = generateSkeletonId();
						personToSkeletonMap.put(personId, personSkeleton);
					}
					
					// STEP 2: determine his/her position by looking for the custom variable in the zone
					String personPositionVar = "skel-position" + "-" + personId;
					String personPosition = (String)zone.getVariableValue(personPositionVar);
					if (personPosition == null) personPosition = POSITION_SITTING;
					
					skeletonPositionMap.put(personSkeleton, personPosition);
				}
			}
			
			setPropertyValue(KinectCamera.SENSED_SKELETONS, skeletonPositionMap); 
		}
	}
	
	// ================================================================================================
	private class KinectZoneListener extends BaseZoneListener {
		@Override
		public void zoneVariableAdded(Zone modifiedZone, String variableName) {
			if (zone == modifiedZone) {
                if (!(getFault().equalsIgnoreCase("yes"))) {
                    if (variableName.startsWith("skel-position")) {
                        senseSkeletons();
                    }
                }
            }
		}
		
		@Override
		public void zoneVariableRemoved(Zone modifiedZone, String variableName) {
			if (zone == modifiedZone) {
                if (!(getFault().equalsIgnoreCase("yes"))) {
                    if (variableName.startsWith("skel-position")) {
                        senseSkeletons();
                    }
                }
            }
		}
		
		@Override
        public void zoneVariableModified(Zone modifiedZone, String variableName, Object oldValue, Object newValue) {
            if (zone == modifiedZone) {
                if (!(getFault().equalsIgnoreCase("yes"))) {
                    if (variableName.startsWith("skel-position")) {
                        senseSkeletons();
                    }
                }
            }
        }
	}
	
	
	private static int counter = 0;
	private static final String SKELETON_NAME_BASE = "Skeleton";  
	static String generateSkeletonId() {
		return SKELETON_NAME_BASE + (counter++);
	}
}
