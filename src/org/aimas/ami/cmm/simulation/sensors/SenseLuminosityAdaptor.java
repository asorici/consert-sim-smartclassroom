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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

import fr.liglab.adele.icasa.device.light.Photometer;

@Component
@Provides
public class SenseLuminosityAdaptor extends SensorAdaptorBase {
	public static final String SENSOR_AGENT_NAME = "CtxSensor_Luminosity" + "__" + "SmartClassroom";
	
	/* ContextAssertionAdaptor Service Properties used to identify the adaptor instance */
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS)
	protected String adaptorClassNameConfig;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_ASSERTION)
	protected String adaptorContextAssertion;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_CMM_AGENT)
	protected String adaptorSensorAgentName;
	
	private Map<String, Integer> luminosityMap;
	
	private ScheduledExecutorService luminosityUpdateService;
	private ScheduledFuture<?> luminosityUpdateTask;
	
	@Requires(id="lightSensors")
	private Photometer[] luminositySensors;
	
	protected SenseLuminosityAdaptor() {
	    super(SmartClassroom.sensesLuminosity.getURI());
	    
	    adaptorClassNameConfig = getClass().getName();
	    adaptorContextAssertion = contextAssertionURI;
	    adaptorSensorAgentName = SENSOR_AGENT_NAME;
	    
	    luminosityMap = new HashMap<String, Integer>();
	}
	
	@Bind(policy=BindingPolicy.DYNAMIC_PRIORITY,
		  filter="(" + CMMConstants.CONSERT_APPLICATION_ID_PROP + "=" + "SmartClassroom" + ")")
	private void bindTimeService(TimeService timeService) {
		setTimeService(timeService);
	}
	
	@Bind(id="lightSensors")
	public void bindLuminositySensor() {
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Unbind(id="lightSensors")
	public void unbindLuminositySensor() {
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Invalidate
	private void stopAdaptor() {
		if (luminosityUpdateTask != null) {
			luminosityUpdateTask.cancel(true);
			luminosityUpdateTask = null;
		}
		
		if (luminosityUpdateService != null) {
			luminosityUpdateService.shutdown();
			luminosityUpdateService = null;
		}
	}
	
	////////////////////////////////////////////////////////////////
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
	
	////////////////////////////////////////////////////////////////
	@Override
    protected Map<String, String> getSensorInstanceMap() {
		Map<String, String> instances = new HashMap<String, String>();
		
		instances.put(SmartClassroom.Lum_EF210_PresenterArea.getURI(), SmartClassroom.LuminositySensor.getURI());
		instances.put(SmartClassroom.Lum_EF210_Section1_Right.getURI(), SmartClassroom.LuminositySensor.getURI());
		instances.put(SmartClassroom.Lum_EF210_Section2_Right.getURI(), SmartClassroom.LuminositySensor.getURI());
		instances.put(SmartClassroom.Lum_EF210_Section3_Right.getURI(), SmartClassroom.LuminositySensor.getURI());
		
		return instances;
    }
	
	@Override
    protected List<Map<Integer, Update>> deliverAssertionUpdates(String sensorIdURI) {
		List<Map<Integer, Update>> updates = new LinkedList<Map<Integer,Update>>();
		SensorInstance sensorInstance = sensorInstances.get(sensorIdURI);
		
		Map<Integer, Update> assertionUpdate = new HashMap<Integer, Update>();
		updates.add(assertionUpdate);
		
		// ======== STEP 1: ASSERTION UUID CREATE
		Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.sensesLuminosity));
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ID_CREATE, new UpdateCreate(assertionUUIDNode));
		
		// ======== STEP 2: ASSERTION CONTENT
		int luminosityLevel = luminosityMap.get(sensorIdURI);
		
		QuadDataAcc assertionContent = new QuadDataAcc();
		assertionContent.addQuad(Quad.create(assertionUUIDNode, sensorInstance.getIdResource().asNode(), 
			SmartClassroom.sensesLuminosity.asNode(), ResourceFactory.createTypedLiteral(new Integer(luminosityLevel)).asNode()));
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
				assertionUUIDNode.getURI(), SmartClassroom.sensesLuminosity.getURI(), 
				ContextAssertionType.Sensed, now, validityIntervals, 1.0, sensorInstance.getIdResource().getURI());
		
		QuadDataAcc annotationContent = new QuadDataAcc();
		String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.sensesLuminosity.getURI());
		Node assertionStoreNode = Node.createURI(assertionStoreURI);
		
		for (Statement s : assertionAnnotations) {
			annotationContent.addQuad(Quad.create(assertionStoreNode, s.asTriple()));
		}
		
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ANNOTATION_UPDATE, new UpdateDataInsert(annotationContent));
		
		return updates;
    }
	
	private void startUpdates(boolean forceRestart) {
		System.out.println("[" + getClass().getSimpleName() + "] STARTING UPDATES.");
		if (luminosityUpdateService == null || luminosityUpdateService.isShutdown()) {
	    	luminosityUpdateService = Executors.newSingleThreadScheduledExecutor();
	    }
	    
	    if (luminosityUpdateTask == null) {
	    	luminosityUpdateTask = luminosityUpdateService.scheduleAtFixedRate(new LuminosityUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);
	    }
	    else if (forceRestart) {
	    	luminosityUpdateTask.cancel(false);			// cancel the old repeating task
	    	luminosityUpdateTask = luminosityUpdateService.scheduleAtFixedRate(new LuminosityUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);		// do the new one (having most likely a different updateRate)
	    }
    }
	
	private void stopUpdates() {
		System.out.println("[" + getClass().getSimpleName() + "] STOPPING UPDATES.");
		
		if (luminosityUpdateTask != null) {
			luminosityUpdateTask.cancel(false);
			luminosityUpdateTask = null;
		}
	}
	
	private class LuminosityUpdateTask implements Runnable {
		@Override
        public void run() {
			// access the data from the luminosity sensor(s)
			luminosityMap.clear();
			for (Photometer luminositySensor : luminositySensors) {
				int ligthLevel = (int)Math.round(luminositySensor.getIlluminance());
				String sensorIdURI = SmartClassroom.BOOTSTRAP_NS + luminositySensor.getSerialNumber();
				
				if (sensorInstances.containsKey(sensorIdURI)) {
					luminosityMap.put(sensorIdURI, ligthLevel);
					List<UpdateRequest> updateRequests = deliverUpdates(sensorIdURI);
					
					if (updateRequests != null) {
						//System.out.println("[" + SenseLuminosityAdaptor.class.getSimpleName() + "] "
						//		+ "Generating update from sensor: " + sensorIdURI);
						
						for (UpdateRequest update : updateRequests) {
			        		sensingAdaptor.deliverUpdate(getProvidedAssertion(), update);
			        	}
					}
				}
			}
        }
	}
}
