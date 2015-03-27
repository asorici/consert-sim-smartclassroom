package org.aimas.ami.cmm.simulation.sensors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.cmm.sensing.ContextAssertionAdaptor;
import org.aimas.ami.cmm.simulation.SensorStatsCollector;
import org.aimas.ami.contextrep.datatype.CalendarInterval;
import org.aimas.ami.contextrep.datatype.CalendarIntervalList;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.resources.CMMConstants;
import org.aimas.ami.contextrep.resources.TimeService;
import org.aimas.ami.contextrep.utils.ContextModelUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.BindingPolicy;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

@Component
@Provides
public class SenseBluetoothAdaptor extends SensorAdaptorBase {
	public static final String SENSOR_AGENT_NAME = "CtxSensor_Presence" + "__" + "SmartClassroom";
	
	/* ContextAssertionAdaptor Service Properties used to identify the adaptor instance */
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS)
	protected String adaptorClassNameConfig;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_ASSERTION)
	protected String adaptorContextAssertion;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_CMM_AGENT)
	protected String adaptorSensorAgentName;
	
	private Set<String> sensedBluetoothAddresses;
	
	@Requires(id="presenceSensors")
	private BluetoothSensor[] presenceSensors;
	
	@Requires
	private SensorStatsCollector sensorStatsCollector;
	
	private ScheduledExecutorService presenceUpdateService;
	private ScheduledFuture<?> presenceUpdateTask;
	
	public SenseBluetoothAdaptor() {
	    super(SmartClassroom.sensesBluetoothAddress.getURI());
	    
	    adaptorClassNameConfig = getClass().getName();
	    adaptorContextAssertion = contextAssertionURI;
	    adaptorSensorAgentName = SENSOR_AGENT_NAME;
	    
	    this.sensedBluetoothAddresses = new HashSet<String>();
    }
	
	@Bind(policy=BindingPolicy.DYNAMIC_PRIORITY,
		  filter="(" + CMMConstants.CONSERT_APPLICATION_ID_PROP + "=" + "SmartClassroom" + ")")
	private void bindTimeService(TimeService timeService) {
		setTimeService(timeService);
	}
	
	@Bind(id="presenceSensors")
	public void bindPresenceSensor() {
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Unbind(id="presenceSensors")
	public void unbindPresenceSensor() {
		if (updatesEnabled.get()) {
			stopUpdates();
		}
	}
	
	@Invalidate
	private void stopAdaptor() {
		if (presenceUpdateTask != null) {
			presenceUpdateTask.cancel(true);
		}
		
		if (presenceUpdateService != null) {
			presenceUpdateService.shutdown();
			presenceUpdateService = null;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    public boolean supportsChangeBasedUpdateMode() {
	    return false;
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
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    protected Map<String, String> getSensorInstanceMap() {
		Map<String, String> instances = new HashMap<String, String>();
		
		instances.put(SmartClassroom.PresenceSensor_EF210_PresenterArea.getURI(), SmartClassroom.PresenceSensor.getURI());
		instances.put(SmartClassroom.PresenceSensor_EF210_Section1_Left.getURI(), SmartClassroom.PresenceSensor.getURI());
		instances.put(SmartClassroom.PresenceSensor_EF210_Section1_Right.getURI(), SmartClassroom.PresenceSensor.getURI());
		instances.put(SmartClassroom.PresenceSensor_EF210_Section2_Left.getURI(), SmartClassroom.PresenceSensor.getURI());
		instances.put(SmartClassroom.PresenceSensor_EF210_Section2_Right.getURI(), SmartClassroom.PresenceSensor.getURI());
		instances.put(SmartClassroom.PresenceSensor_EF210_Section3_Left.getURI(), SmartClassroom.PresenceSensor.getURI());
		instances.put(SmartClassroom.PresenceSensor_EF210_Section3_Right.getURI(), SmartClassroom.PresenceSensor.getURI());
		
		return instances;
    }
	
	
	@Override
    protected  List<Map<Integer, Update>> deliverAssertionUpdates(String sensorIdURI) {
		List<Map<Integer, Update>> updates = new LinkedList<Map<Integer,Update>>();
		SensorInstance sensorInstance = sensorInstances.get(sensorIdURI);
		
		for (String sensedAddress : sensedBluetoothAddresses) {
			//System.out.println("[" + getClass().getSimpleName() + "::" + sensorIdURI + "] "
			//	+ "Sensed bluetoothAddress: " + sensedAddress);
			
			Map<Integer, Update> assertionUpdate = new HashMap<Integer, Update>();
			
			// ======== STEP 1: ASSERTION UUID CREATE
			Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.sensesBluetoothAddress));
			assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ID_CREATE, new UpdateCreate(assertionUUIDNode));
			
			// ======== STEP 2: ASSERTION CONTENT
			QuadDataAcc assertionContent = new QuadDataAcc();
			assertionContent.addQuad(Quad.create(assertionUUIDNode, sensorInstance.getIdResource().asNode(), 
				SmartClassroom.sensesBluetoothAddress.asNode(), ResourceFactory.createTypedLiteral(sensedAddress, XSDDatatype.XSDstring).asNode()));
			assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_CONTENT_UPDATE, new UpdateDataInsert(assertionContent));
			
			// ======== STEP 3: ASSERTION ANNOTATIONS
			Calendar now = now();
			CalendarIntervalList validityIntervals = new CalendarIntervalList();
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			if (updateMode.equals(ContextAssertionAdaptor.TIME_BASED)) {
				Calendar validityLimit = (Calendar)now.clone();
				validityLimit.add(Calendar.SECOND, updateRate);
				
				validityIntervals.add(new CalendarInterval(now, true, validityLimit, true));
			}
			else {
				validityIntervals.add(new CalendarInterval(now, true, null, false));
			}
			
			//System.out.println("["+ getClass().getName() + "] submitted timestamp: " + formatter.format(now.getTime()));
			//System.out.println("["+ getClass().getName() + "] submitted validityInterval: " + validityIntervals);
			
			List<Statement> assertionAnnotations = ContextModelUtils.createAnnotationStatements(
					assertionUUIDNode.getURI(), SmartClassroom.sensesBluetoothAddress.getURI(), 
					ContextAssertionType.Sensed, now, validityIntervals, 1.0, sensorInstance.getIdResource().getURI());
			
			QuadDataAcc annotationContent = new QuadDataAcc();
			String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.sensesBluetoothAddress.getURI());
			Node assertionStoreNode = Node.createURI(assertionStoreURI);
			
			for (Statement s : assertionAnnotations) {
				annotationContent.addQuad(Quad.create(assertionStoreNode, s.asTriple()));
			}
			
			assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ANNOTATION_UPDATE, new UpdateDataInsert(annotationContent));
			
			// ========
			updates.add(assertionUpdate);
		}
		
		return updates;
    }
	
	private void startUpdates(boolean forceRestart) {
		System.out.println("[" + getClass().getSimpleName() + "] STARTING UPDATES.");
		
		if (presenceUpdateService == null || presenceUpdateService.isShutdown()) {
	    	presenceUpdateService = Executors.newSingleThreadScheduledExecutor();
	    }
	    
	    if (presenceUpdateTask == null) {
	    	presenceUpdateTask = presenceUpdateService.scheduleAtFixedRate(new SensePresenceUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);
	    }
	    else if (forceRestart) {
	    	presenceUpdateTask.cancel(false);			// cancel the old repeating task
	    	presenceUpdateTask = presenceUpdateService.scheduleAtFixedRate(new SensePresenceUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);		// do the new one (having most likely a different updateRate)
	    }
    }
	
	private void stopUpdates() {
		System.out.println("[" + getClass().getSimpleName() + "] STOPPING UPDATES.");
		
		if (presenceUpdateTask != null) {
			presenceUpdateTask.cancel(false);
			presenceUpdateTask = null;
		}
	}
	
	
	private class SensePresenceUpdateTask implements Runnable {
		@Override
        public void run() {
			// access the data from the presence sensor(s)
			for (BluetoothSensor sensor : presenceSensors) {
				sensedBluetoothAddresses.clear();
				
				Set<String> sensedAddresses = sensor.getSensedAddresses();
				
				sensorStatsCollector.markSensing(System.currentTimeMillis(), 
						SmartClassroom.sensesBluetoothAddress.getLocalName());
				
				if (sensedAddresses != null) {
					String sensorIdURI = SmartClassroom.BOOTSTRAP_NS + sensor.getSerialNumber();
					if (sensorInstances.containsKey(sensorIdURI)) {
						
						sensedBluetoothAddresses.addAll(sensedAddresses);
						List<UpdateRequest> updateRequests = deliverUpdates(sensorIdURI);
			        	
						if (updateRequests != null) {
							//System.out.println("[" + SenseBluetoothAdaptor.class.getSimpleName() + "] "
							//		+ "Generating update from sensor: " + sensorIdURI);
							
				        	for (UpdateRequest update : updateRequests) {
				        		sensingAdaptor.deliverUpdate(getProvidedAssertion(), update);
				        		sensorStatsCollector.markSensingUpdateMessage(System.currentTimeMillis(), 
				        				update.hashCode(), SmartClassroom.sensesBluetoothAddress.getLocalName());
				        	}
						}
					}
				}
			}
        }
	}
}
