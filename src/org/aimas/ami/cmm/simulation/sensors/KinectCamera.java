package org.aimas.ami.cmm.simulation.sensors;

import java.util.Map;

import fr.liglab.adele.icasa.device.GenericDevice;

public interface KinectCamera extends GenericDevice {
	public static String SENSED_SKELETONS = "kinect.sensedSkeletons";
	
	public static String POSITION_STANDING = "SkeletonStanding";
	public static String POSITION_SITTING = "SkeletonSitting";
	
	public Map<String, String> getSensedSkeletons();
}
