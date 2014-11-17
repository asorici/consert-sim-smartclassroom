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
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.sparql.modify.request.UpdateDeleteInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

@Component
@Provides
public class KinectSkeletonAdaptor extends SensorAdaptorBase {
	private List<SkeletonInfo> skeletonTracker;
	
	private ScheduledExecutorService kinectUpdateService;
	private ScheduledFuture<?> kinectUpdateTask;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS)
	private String adaptorClassNameConfig;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_HANDLED_SENSORS)
	private String[] sensorInstanceConfig;
	
	@Requires(id="kinectSensors")
	private KinectCamera[] kinectSensors;
	
	protected KinectSkeletonAdaptor() {
	    super(SmartClassroom.sensesSkeletonInPosition.getURI());
	    
	    adaptorClassNameConfig = getClass().getName();
	    sensorInstanceConfig = prepareSensorInstanceConfig();
	    
	    this.skeletonTracker = new LinkedList<SkeletonInfo>();
    }
	
	@Bind(policy="dynamic-priority")
	private void bindTimeService(TimeService timeService) {
		setTimeService(timeService);
	}
	
	@Bind(id="kinectSensors")
	public void bindKinectSensor(KinectCamera cam) {
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Unbind(id="kinectSensors")
	public void unbindKinectSensor(KinectCamera cam) {
		System.out.println("[" + KinectSkeletonAdaptor.class.getSimpleName() + "] "
				+ "removing camera: " + cam.getSerialNumber());
		
		if (updatesEnabled.get()) {
			startUpdates(false);
		}
	}
	
	@Invalidate
	private void stopAdaptor() {
		if (kinectUpdateTask != null) {
			kinectUpdateTask.cancel(true);
			kinectUpdateTask = null;
		}
		
		if (kinectUpdateService != null) {
			kinectUpdateService.shutdown();
			kinectUpdateService = null;
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
	
	/////////////////////////////////////////////////////////////////////////
	@Override
    protected Map<String, String> getSensorInstanceMap() {
		Map<String, String> instances = new HashMap<String, String>();
		
		instances.put(SmartClassroom.Kinect_EF210_PresenterArea.getURI(), SmartClassroom.KinectCamera.getURI());
		instances.put(SmartClassroom.Kinect_EF210_Section1_Left.getURI(), SmartClassroom.KinectCamera.getURI());
		instances.put(SmartClassroom.Kinect_EF210_Section1_Right.getURI(), SmartClassroom.KinectCamera.getURI());
		instances.put(SmartClassroom.Kinect_EF210_Section2_Left.getURI(), SmartClassroom.KinectCamera.getURI());
		instances.put(SmartClassroom.Kinect_EF210_Section2_Right.getURI(), SmartClassroom.KinectCamera.getURI());
		instances.put(SmartClassroom.Kinect_EF210_Section3_Left.getURI(), SmartClassroom.KinectCamera.getURI());
		instances.put(SmartClassroom.Kinect_EF210_Section3_Right.getURI(), SmartClassroom.KinectCamera.getURI());
		
		return instances;
    }
	
	@Override
    protected List<Map<Integer, Update>> deliverAssertionUpdates(String sensorIdURI) {
		List<Map<Integer, Update>> updates = new LinkedList<Map<Integer,Update>>();
		SensorInstance sensorInstance = sensorInstances.get(sensorIdURI);
		
		for (SkeletonInfo skelInfo : skeletonTracker) {
			Map<Integer, Update> assertionUpdate = new HashMap<Integer, Update>();
			
			Model skeletonModel = ModelFactory.createDefaultModel();
			Resource skeletonRes = skeletonModel.createResource(skelInfo.getSkeletonIdURI(), SmartClassroom.KinectSkeleton);
			Resource skelPositionRes = ResourceFactory.createResource(SmartClassroom.NS + skelInfo.getSkeletonPosition());
		
			// ======== STEP 1
			UpdateDeleteInsert entityStoreUpdate = new UpdateDeleteInsert();
			Node entityStoreNode = Node.createURI(ConsertCore.ENTITY_STORE_URI);
	    	
	    	StmtIterator it = skeletonModel.listStatements();
	    	for (;it.hasNext();) {
	    		Statement s = it.next();
	    		entityStoreUpdate.getDeleteAcc().addQuad(Quad.create(entityStoreNode, s.asTriple()));
	    		entityStoreUpdate.getInsertAcc().addQuad(Quad.create(entityStoreNode, s.asTriple()));
	    	}
	    	
	    	assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ENTITY_UPDATE, entityStoreUpdate);
	    	
	    	// ======== STEP 2: ASSERTION UUID CREATE
	    	Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.sensesSkeletonInPosition));
	    	assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ID_CREATE, new UpdateCreate(assertionUUIDNode));
	    	
	    	// ======== STEP 3: ASSERTION CONTENT
	    	QuadDataAcc assertionContent = new QuadDataAcc();
	    	Resource bnode = ResourceFactory.createResource();
	    	assertionContent.addQuad(Quad.create(assertionUUIDNode, bnode.asNode(), 
		    	RDF.type.asNode(), SmartClassroom.sensesSkeletonInPosition.asNode()));
	    	assertionContent.addQuad(Quad.create(assertionUUIDNode, bnode.asNode(), 
			    SmartClassroom.hasCameraRole.asNode(), sensorInstance.getIdResource().asNode()));
	    	assertionContent.addQuad(Quad.create(assertionUUIDNode, bnode.asNode(), 
			    SmartClassroom.hasSkeletonRole.asNode(), skeletonRes.asNode()));
	    	assertionContent.addQuad(Quad.create(assertionUUIDNode, bnode.asNode(), 
				SmartClassroom.hasSkelPositionRole.asNode(), skelPositionRes.asNode()));
		    
	    	assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_CONTENT_UPDATE, new UpdateDataInsert(assertionContent));
	    	
	    	// ======== STEP 4: ASSERTION ANNOTATIONS
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
					assertionUUIDNode.getURI(), SmartClassroom.sensesSkeletonInPosition.getURI(), 
					ContextAssertionType.Sensed, now, validityIntervals, 1.0, sensorInstance.getIdResource().getURI());
			
			QuadDataAcc annotationContent = new QuadDataAcc();
			String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.sensesSkeletonInPosition.getURI());
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
		
		if (kinectUpdateService == null || kinectUpdateService.isShutdown()) {
	    	kinectUpdateService = Executors.newSingleThreadScheduledExecutor();
	    }
	    
	    if (kinectUpdateTask == null) {
	    	kinectUpdateTask = kinectUpdateService.scheduleAtFixedRate(new KinectUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);
	    }
	    else if (forceRestart) {
	    	kinectUpdateTask.cancel(false);			// cancel the old repeating task
	    	kinectUpdateTask = kinectUpdateService.scheduleAtFixedRate(new KinectUpdateTask(), 0, 
	    			updateRate, TimeUnit.SECONDS);		// do the new one (having most likely a different updateRate)
	    }
    }
	
	private void stopUpdates() {
		System.out.println("[" + getClass().getSimpleName() + "] STOPPING UPDATES.");
		
		if (kinectUpdateTask != null) {
			kinectUpdateTask.cancel(false);
			kinectUpdateTask = null;
		}
	}
	
	
	private class KinectUpdateTask implements Runnable {
		@Override
        public void run() {
			// access the data from the kinect sensor(s)
			for (KinectCamera kinect : kinectSensors) {
				skeletonTracker.clear();
				
				String sensorIdURI = SmartClassroom.BOOTSTRAP_NS + kinect.getSerialNumber();
				Map<String, String> sensedSkeletons = kinect.getSensedSkeletons();
				
				if (sensedSkeletons != null) {
					if (sensorInstances.containsKey(sensorIdURI)) {
						for (String skeletonId : sensedSkeletons.keySet()) {
							String skeletonIdURI = SmartClassroom.BOOTSTRAP_NS + skeletonId;
							SkeletonInfo skelInfo = new SkeletonInfo(skeletonIdURI, sensedSkeletons.get(skeletonId));
							
							skeletonTracker.add(skelInfo);
						}
						
						
						List<UpdateRequest> updateRequests = deliverUpdates(sensorIdURI);
						
						if (updateRequests != null) {
							//System.out.println("[" + KinectSkeletonAdaptor.class.getSimpleName() + "] "
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
	
	/////////////////////////////////////////////////////////////////////////
	private static class SkeletonInfo {
		String skeletonIdURI;
		String skeletonPosition;
		
		public SkeletonInfo(String skeletonIdURI, String skeletonPosition) {
	        this.skeletonIdURI = skeletonIdURI;
	        this.skeletonPosition = skeletonPosition;
        }
		
		public String getSkeletonIdURI() {
			return skeletonIdURI;
		}
		
		public String getSkeletonPosition() {
			return skeletonPosition;
		}
	}
}
