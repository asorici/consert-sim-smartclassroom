package org.aimas.ami.cmm.simulation.sensors;

import java.util.Dictionary;
import java.util.Hashtable;

import org.aimas.ami.cmm.sensing.ContextAssertionAdaptor;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;

/**
 * This is the component instantiator for the bundle holding the different ContextAssertionAdaptor implementation
 * classes for each of the ContextAssertions used in the SmartClassroom scenario.
 * Each implementation of an adaptor also specifies properties that identify the service uniquely (identified by
 * an unique URI which is present in the cmm-config.ttl file under the SensingPolicy specifications for a CtxSensor)
 * are handled by the adaptor. This is a basic mechanism which might be changed in the future.
 * 
 * @author Alex Sorici
 *
 */

@Component(publicFactory=false)
@Instantiate(name="sensor-adaptor-configurator")
public class SensorAdaptorConfiguration {
	
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.sensors.InformTeachingAdaptor)")
	private Factory teachingAdaptorFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.sensors.KinectSkeletonAdaptor)")
	private Factory kinectSkeletonFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.sensors.NoiseLevelAdaptor)")
	private Factory noiseLevelAdaptorFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.sensors.SenseBluetoothAdaptor)")
	private Factory presenceAdaptorFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.sensors.SenseLuminosityAdaptor)")
	private Factory luminosityAdaptorFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.sensors.SenseTemperatureAdaptor)")
	private Factory temperatureAdaptorFactory;
	
	
	public SensorAdaptorConfiguration() {
		try {
			System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + "HERE!!!");
			configureSensorAdaptors();
        }
        catch (Exception e) {
	        System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] Failed to configure and instantiate ContextAssertion adaptors: " + e.getMessage());
        }
	}
	
	
    private void configureSensorAdaptors() throws Exception {
    	
    	// STEP 1: Register the InformTeachingAdaptor
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS, InformTeachingAdaptor.class.getName());
		props.put(ContextAssertionAdaptor.ADAPTOR_ASSERTION, SmartClassroom.takesPlaceIn.getURI());
		props.put(ContextAssertionAdaptor.ADAPTOR_CMM_AGENT, InformTeachingAdaptor.SENSOR_AGENT_NAME);
		
		ComponentInstance teachingAdaptorInstance = teachingAdaptorFactory.createComponentInstance(props);
		teachingAdaptorInstance.start();
		//System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + teachingAdaptorInstance.getInstanceDescription().getDescription());
		
		
		// STEP 2: Register the KinectSkeletonAdaptor
		props = new Hashtable<String, Object>();
		props.put(ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS, KinectSkeletonAdaptor.class.getName());
		props.put(ContextAssertionAdaptor.ADAPTOR_ASSERTION, SmartClassroom.sensesSkeletonInPosition.getURI());
		props.put(ContextAssertionAdaptor.ADAPTOR_CMM_AGENT, KinectSkeletonAdaptor.SENSOR_AGENT_NAME);
		
		ComponentInstance kinectAdaptorInstance = kinectSkeletonFactory.createComponentInstance(props);
		kinectAdaptorInstance.start();
		//System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + kinectAdaptorInstance.getInstanceDescription().getDescription());
		//context.registerService(ContextAssertionAdaptor.class, kinectAdaptor, props);
		
		// STEP 3: Register the NoiseLevelAdaptor
		props = new Hashtable<String, Object>();
		props.put(ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS, NoiseLevelAdaptor.class.getName());
		props.put(ContextAssertionAdaptor.ADAPTOR_ASSERTION, SmartClassroom.hasNoiseLevel.getURI());
		props.put(ContextAssertionAdaptor.ADAPTOR_CMM_AGENT, NoiseLevelAdaptor.SENSOR_AGENT_NAME);
		ComponentInstance micAdaptorInstance = noiseLevelAdaptorFactory.createComponentInstance(props);
		micAdaptorInstance.start();
		//System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + micAdaptorInstance.getInstanceDescription().getDescription());
		//context.registerService(ContextAssertionAdaptor.class, noiseAdaptor, props);
		
		// STEP 4: Register the SenseBluetoothAdaptor
		props = new Hashtable<String, Object>();
		props.put(ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS, SenseBluetoothAdaptor.class.getName());
		props.put(ContextAssertionAdaptor.ADAPTOR_ASSERTION, SmartClassroom.sensesBluetoothAddress.getURI());
		props.put(ContextAssertionAdaptor.ADAPTOR_CMM_AGENT, SenseBluetoothAdaptor.SENSOR_AGENT_NAME);
		ComponentInstance presenceAdaptorInstance = presenceAdaptorFactory.createComponentInstance(props);
		presenceAdaptorInstance.start();
		//System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + presenceAdaptorInstance.getInstanceDescription().getDescription());
		//context.registerService(ContextAssertionAdaptor.class, presenceAdaptor, props);
    
		
		// STEP 5: Register the SenseLuminosityAdaptor
		props = new Hashtable<String, Object>();
		props.put(ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS, SenseLuminosityAdaptor.class.getName());
		props.put(ContextAssertionAdaptor.ADAPTOR_ASSERTION, SmartClassroom.sensesLuminosity.getURI());
		props.put(ContextAssertionAdaptor.ADAPTOR_CMM_AGENT, SenseLuminosityAdaptor.SENSOR_AGENT_NAME);
		ComponentInstance luminosityAdaptorInstance = luminosityAdaptorFactory.createComponentInstance(props);
		luminosityAdaptorInstance.start();
		//System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + luminosityAdaptorInstance.getInstanceDescription().getDescription());
		//context.registerService(ContextAssertionAdaptor.class, luminosityAdaptor, props);
	
		// STEP 6: Register the SenseTemperatureAdaptor
		props = new Hashtable<String, Object>();
		props.put(ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS, SenseTemperatureAdaptor.class.getName());
		props.put(ContextAssertionAdaptor.ADAPTOR_ASSERTION, SmartClassroom.sensesTemperature.getURI());
		props.put(ContextAssertionAdaptor.ADAPTOR_CMM_AGENT, SenseTemperatureAdaptor.SENSOR_AGENT_NAME);
		ComponentInstance temperatureAdaptorInstance = temperatureAdaptorFactory.createComponentInstance(props);
		temperatureAdaptorInstance.start();
		//System.out.println("[" + SensorAdaptorConfiguration.class.getName() + "] " + temperatureAdaptorInstance.getInstanceDescription().getDescription());
		//context.registerService(ContextAssertionAdaptor.class, temperatureAdaptor, props);
		
	}
}
