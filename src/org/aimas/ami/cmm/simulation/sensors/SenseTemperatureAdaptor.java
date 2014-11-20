package org.aimas.ami.cmm.simulation.sensors;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.cmm.sensing.ContextAssertionAdaptor;
import org.aimas.ami.contextrep.datatype.CalendarInterval;
import org.aimas.ami.contextrep.datatype.CalendarIntervalList;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.resources.TimeService;
import org.aimas.ami.contextrep.utils.ContextModelUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

import fr.liglab.adele.icasa.device.temperature.Thermometer;

@Component
@Provides(specifications={ContextAssertionAdaptor.class})
public class SenseTemperatureAdaptor extends SensorAdaptorBase {
	public static final String SENSOR_AGENT_NAME = "CtxSensor_Temperature" + "__" + "SmartClassroom";
	
	/* ContextAssertionAdaptor Service Properties used to identify the adaptor instance */
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS)
	protected String adaptorClassNameConfig;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_ASSERTION)
	protected String adaptorContextAssertion;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_CMM_AGENT)
	protected String adaptorSensorAgentName;
	
	private Map<String, Integer> temperatureMap;
	
	private ScheduledExecutorService temperatureUpdateService;
	private ScheduledFuture<?> temperatureUpdateTask;
	
	@Requires(id="temperatureSensors")
	private Thermometer[] temperatureSensors;
	
	protected SenseTemperatureAdaptor() {
	    super(SmartClassroom.sensesTemperature.getURI());
	    
	    adaptorClassNameConfig = getClass().getName();
	    adaptorContextAssertion = contextAssertionURI;
	    adaptorSensorAgentName = SENSOR_AGENT_NAME;
	    
	    temperatureMap = new HashMap<String, Integer>();
    }
	
	@Bind(policy="dynamic-priority")
	private void bindTimeService(TimeService timeService) {
		setTimeService(timeService);
	}
	
	@Bind(id="temperatureSensors")
	public void bindTemperatureSensor() {
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Unbind(id="temperatureSensors")
	public void unbindTemperatureSensor() {
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Invalidate
	private void stopAdaptor() {
		if (temperatureUpdateTask != null) {
			temperatureUpdateTask.cancel(true);
			temperatureUpdateTask = null;
		}
		
		if (temperatureUpdateService != null) {
			temperatureUpdateService.shutdown();
			temperatureUpdateService = null;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////
	@Override
    public boolean supportsChangeBasedUpdateMode() {
	    return true;
    }

	@Override
    public int getMaxUpdateRate() {
	    return 1;
    }

	@Override
    public boolean setUpdateEnabled(boolean enabled) {
		updatesEnabled.set(enabled);
		
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
		else {
			stopUpdates();
		}
		
		return true;
    }

	@Override
    public boolean setUpdateMode(String updateMode, int updateRate) {
		if (updateMode.equals(ContextAssertionAdaptor.TIME_BASED)) {
			this.updateMode = updateMode;
			this.updateRate = updateRate;
			
			if (updatesEnabled.get()) {
				startUpdates(true);
			}
			
			return true;
		}
		
		return false;
    }
	
	//////////////////////////////////////////////////////////////////////////////////
	@Override
    protected Map<String, String> getSensorInstanceMap() {
		Map<String, String> instances = new HashMap<String, String>();
		
		instances.put(SmartClassroom.Temp_EF210_Section1_Left.getURI(), SmartClassroom.TemperatureSensor.getURI());
		instances.put(SmartClassroom.Temp_EF210_Section1_Right.getURI(), SmartClassroom.TemperatureSensor.getURI());
		instances.put(SmartClassroom.Temp_EF210_Section3_Left.getURI(), SmartClassroom.TemperatureSensor.getURI());
		instances.put(SmartClassroom.Temp_EF210_Section3_Right.getURI(), SmartClassroom.TemperatureSensor.getURI());
		
		return instances;
    }
	
	@Override
    protected List<Map<Integer, Update>> deliverAssertionUpdates(String sensorIdURI) {
		List<Map<Integer, Update>> updates = new LinkedList<Map<Integer,Update>>();
		SensorInstance sensorInstance = sensorInstances.get(sensorIdURI);
		
		Map<Integer, Update> assertionUpdate = new HashMap<Integer, Update>();
		updates.add(assertionUpdate);
		
		// ======== STEP 1: ASSERTION UUID CREATE
		Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.sensesTemperature));
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ID_CREATE, new UpdateCreate(assertionUUIDNode));
		
		// ======== STEP 2: ASSERTION CONTENT
		int temperatureLevel = temperatureMap.get(sensorIdURI);
		
		QuadDataAcc assertionContent = new QuadDataAcc();
		assertionContent.addQuad(Quad.create(assertionUUIDNode, sensorInstance.getIdResource().asNode(), 
			SmartClassroom.sensesTemperature.asNode(), ResourceFactory.createTypedLiteral(new Integer(temperatureLevel)).asNode()));
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_CONTENT_UPDATE, new UpdateDataInsert(assertionContent));
		
		// ======== STEP 3: ASSERTION ANNOTATIONS
		Calendar now = now();
		CalendarIntervalList validityIntervals = new CalendarIntervalList();
		
		if (updateMode.equals(ContextAssertionAdaptor.TIME_BASED)) {
			Calendar validityLimit = (Calendar)now.clone();
			validityLimit.add(Calendar.SECOND, updateRate);
			
			validityIntervals.add(new CalendarInterval(now, true, validityLimit, true));
		}
		else {
			validityIntervals.add(new CalendarInterval(now, true, null, false));
		}
		
		List<Statement> assertionAnnotations = ContextModelUtils.createAnnotationStatements(
				assertionUUIDNode.getURI(), SmartClassroom.sensesTemperature.getURI(), 
				ContextAssertionType.Sensed, now, validityIntervals, 1.0, sensorInstance.getIdResource().getURI());
		
		QuadDataAcc annotationContent = new QuadDataAcc();
		String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.sensesTemperature.getURI());
		Node assertionStoreNode = Node.createURI(assertionStoreURI);
		
		for (Statement s : assertionAnnotations) {
			annotationContent.addQuad(Quad.create(assertionStoreNode, s.asTriple()));
		}
		
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ANNOTATION_UPDATE, new UpdateDataInsert(annotationContent));
		
		return updates;
    }
	
	private void startUpdates(boolean forceRestart) {
		System.out.println("[" + getClass().getSimpleName() + "] STARTING UPDATES.");
		
	    if (temperatureUpdateService == null || temperatureUpdateService.isShutdown()) {
	    	temperatureUpdateService = Executors.newSingleThreadScheduledExecutor();
	    }
	    
	    if (temperatureUpdateTask == null) {
	    	temperatureUpdateTask = temperatureUpdateService.scheduleAtFixedRate(new TemperatureUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);
	    }
	    else if (forceRestart) {
	    	temperatureUpdateTask.cancel(false);			// cancel the old repeating task
	    	temperatureUpdateTask = temperatureUpdateService.scheduleAtFixedRate(new TemperatureUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);		// do the new one (having most likely a different updateRate)
	    }
    }
	
	private void stopUpdates() {
		System.out.println("[" + getClass().getSimpleName() + "] STOPPING UPDATES.");
		
		if (temperatureUpdateTask != null) {
			temperatureUpdateTask.cancel(false);
			temperatureUpdateTask = null;
		}
	}
	
	private class TemperatureUpdateTask implements Runnable {
		@Override
        public void run() {
			// access the data from the temperature sensor(s)
			temperatureMap.clear();
			for (Thermometer temperatureSensor : temperatureSensors) {
				int temperatureLevel = (int)Math.round(temperatureSensor.getTemperature() - 273.15);
				String sensorIdURI = SmartClassroom.BOOTSTRAP_NS + temperatureSensor.getSerialNumber();
				
				if (sensorInstances.containsKey(sensorIdURI)) {
					temperatureMap.put(sensorIdURI, temperatureLevel);
					List<UpdateRequest> updateRequests = deliverUpdates(sensorIdURI);
		        	
					if (updateRequests != null) {
						System.out.println("[" + SenseTemperatureAdaptor.class.getSimpleName() + "] "
								+ "Generating update from sensor: " + sensorIdURI);
						
						for (UpdateRequest update : updateRequests) {
			        		sensingAdaptor.deliverUpdate(getProvidedAssertion(), update);
			        	}
					}
				}
			}
        }
	}
}
