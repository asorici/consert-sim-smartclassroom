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
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

@Component
@Provides
public class InformTeachingAdaptor extends SensorAdaptorBase {
	
	private ScheduledExecutorService teachingUpdateService;
	private ScheduledFuture<?> teachingUpdateTask;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_IMPL_CLASS)
	private String adaptorClassNameConfig;
	
	@ServiceProperty(name=ContextAssertionAdaptor.ADAPTOR_HANDLED_SENSORS)
	private String[] sensorInstanceConfig;
	
	
	public InformTeachingAdaptor() {
	    super(SmartClassroom.takesPlaceIn.getURI());
	    
	    adaptorClassNameConfig = getClass().getName();
	    sensorInstanceConfig = prepareSensorInstanceConfig();
	}
	
	@Bind(policy="dynamic-priority")
	private void bindTimeService(TimeService timeService) {
		setTimeService(timeService);
	}
	
	
	private List<CalendarInterval> getTeachingDates() {
		List<CalendarInterval> teachingDates = new LinkedList<CalendarInterval>();
		
		Calendar mark = timeService.getCalendarInstance();
		
		// Interval 1
		Calendar start1 = (Calendar)mark.clone();
		start1.set(Calendar.HOUR_OF_DAY, 10);
		start1.set(Calendar.MINUTE, 0);
		start1.set(Calendar.SECOND, 0);
		start1.set(Calendar.MILLISECOND, 0);
		
		Calendar end1 = (Calendar)mark.clone();
		end1.set(Calendar.HOUR_OF_DAY, 12);
		end1.set(Calendar.MINUTE, 0);
		end1.set(Calendar.SECOND, 0);
		end1.set(Calendar.MILLISECOND, 0);
		
		CalendarInterval interval1 = new CalendarInterval(start1, true, end1, true);
		teachingDates.add(interval1);
		
		// Interval 2
		Calendar start2 = (Calendar)mark.clone();
		start2.set(Calendar.HOUR_OF_DAY, 16);
		start2.set(Calendar.MINUTE, 0);
		start2.set(Calendar.SECOND, 0);
		start2.set(Calendar.MILLISECOND, 0);
		
		Calendar end2 = (Calendar)mark.clone();
		end2.set(Calendar.HOUR_OF_DAY, 18);
		end2.set(Calendar.MINUTE, 0);
		end2.set(Calendar.SECOND, 0);
		end2.set(Calendar.MILLISECOND, 0);
		
		CalendarInterval interval2 = new CalendarInterval(start2, true, end2, true);
		teachingDates.add(interval2);
		
		return teachingDates;
	}
	
	/* Content Management */
	private Resource teachingActivityRes;
	
	private CalendarInterval getCurrentTeachingInterval() {
	    Calendar now = timeService.getCalendarInstance();
	    List<CalendarInterval> teachingDates = getTeachingDates();
	    
	    for (CalendarInterval interval : teachingDates) {
	    	if (interval.includes(now)) {
	    		return interval;
	    	}
	    }
	    
	    return null;
    }
	
	private Model createTeachingActivityInstance(CalendarInterval teachingInterval) {
	    // create the activity teaching model, which will hold all the statements that must be given as an EntityStore update
	    Model teachingActivityModel = ModelFactory.createDefaultModel();
	    
		String instanceURI = SensingUtil.generateUniqueURI(SmartClassroom.TeachingActivity.getURI());
	    teachingActivityRes = teachingActivityModel.createResource(instanceURI, SmartClassroom.TeachingActivity);
		
	    // create the start and end InstantThing instances for the activity interval
	    Resource intervalStartRes = teachingActivityModel.createResource(
	    		SensingUtil.generateUniqueURI(SmartClassroom.InstantThing.getURI()), SmartClassroom.InstantThing);
	    Resource intervalEndRes = teachingActivityModel.createResource(
	    		SensingUtil.generateUniqueURI(SmartClassroom.InstantThing.getURI()), SmartClassroom.InstantThing);
	    
	    intervalStartRes.addProperty(SmartClassroom.at, teachingActivityModel.createTypedLiteral(teachingInterval.lowerLimit()));
	    intervalEndRes.addProperty(SmartClassroom.at, teachingActivityModel.createTypedLiteral(teachingInterval.upperLimit()));
	    
	    teachingActivityRes.addProperty(SmartClassroom.from, intervalStartRes);
	    teachingActivityRes.addProperty(SmartClassroom.to, intervalEndRes);
	    
	    return teachingActivityModel;
    }
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    public boolean supportsChangeBasedUpdateMode() {
	    return true;
    }

	@Override
    public int getMaxUpdateRate() {
	    return 0;
    }
	
	@Override
    public boolean setUpdateEnabled(boolean enabled) {
		updatesEnabled.set(enabled);
		
		if (updatesEnabled.get()) {
			startUpdates();
		}
		else {
			stopUpdates();
		}
		
		return true;
    }

	@Override
    public boolean setUpdateMode(String updateMode, int updateRate) {
		this.updateMode = updateMode;
		this.updateRate = updateRate;
		
		return true;
    }
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected Map<String, String> getSensorInstanceMap() {
		Map<String, String> instances = new HashMap<String, String>();
		
		instances.put(SmartClassroom.TeachingActivitySensor.getURI(), ConsertCore.CONTEXT_AGENT.getURI());
		
		return instances;
	}
	
	@Override
    protected SensorInstance deliverSensorEntityInformation(String sensorIdURI, String sensorTypeURI) {
		SensorInstance sensorInstance = super.deliverSensorEntityInformation(sensorIdURI, sensorTypeURI);
		
		Resource instanceRes = sensorInstance.getIdResource();
		instanceRes.addProperty(ConsertCore.CONTEXT_AGENT_TYPE_PROPERTY, ConsertCore.CTX_SENSOR);
		
		return sensorInstance;
    }
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void startUpdates() {
		System.out.println("[" + getClass().getSimpleName() + "] STARTING UPDATES.");
		
		if (teachingUpdateService == null || teachingUpdateService.isShutdown()) {
	    	teachingUpdateService = Executors.newSingleThreadScheduledExecutor();
	    }
	    
	    /* For the teaching update we don't schedule anything at fixed rate. It depends on the teaching intervals.
	     * In this method we compute the delay from the current moment to the closest interval that follows.
	     * We compute the delay for that and schedule an update at that moment. The actual update task will
	     * then schedule the next update. 
	     */
	    int delay = getDelayToClosestTeachingInterval();
	    teachingUpdateTask = teachingUpdateService.schedule(new InformTeachingUpdateTask(), delay, TimeUnit.MILLISECONDS);
    }
	
	private void stopUpdates() {
		System.out.println("[" + getClass().getSimpleName() + "] STOPPING UPDATES.");
		
		if (teachingUpdateTask != null) {
			teachingUpdateTask.cancel(false);
			teachingUpdateTask = null;
		}
	}
	
	private int getDelayToClosestTeachingInterval() {
		List<CalendarInterval> teachingIntervals = getTeachingDates();
		Calendar now = timeService.getCalendarInstance();
		
		CalendarInterval closestInterval = null;
		
		for (int index = 0; index < teachingIntervals.size(); index++) {
			CalendarInterval interval = teachingIntervals.get(index);
			
			if (interval.includes(now) || interval.lowerLimit().after(now)) {
				closestInterval = interval;
				break;
			}
		}
		
		if (closestInterval != null) {
			int delay = (int)(closestInterval.lowerLimit().getTimeInMillis() - now.getTimeInMillis());
			if (delay < 0) { 
				delay = 0;		// this means we are right in the middle of the interval
			}
			else {
				delay += 2000; 	// Add 2 seconds just to make sure we land in the middle of it
			}
			
			return delay;
		}
		else {
			// If we have surpassed all available intervals, then it means that the next available one is
			// the first one next morning
			Calendar nextStart = teachingIntervals.get(0).lowerLimit();
			nextStart.add(Calendar.DATE, 1);
			
			return (int)(nextStart.getTimeInMillis() - now.getTimeInMillis());
		}
	}
	
	@Override
    protected List<Map<Integer, Update>> deliverAssertionUpdates(String sensorIdURI) {
		List<Map<Integer, Update>> updates = new LinkedList<Map<Integer,Update>>();
		CalendarInterval teachingInterval = getCurrentTeachingInterval();
		
		Map<Integer, Update> assertionUpdate = new HashMap<Integer, Update>();
		updates.add(assertionUpdate);
		
	    if (teachingInterval != null) {
			// ======== STEP 1 (optional): ENTITY STORE UPDATE
			if (teachingActivityRes == null) {
				QuadDataAcc entityStoreData = new QuadDataAcc();
				Node entityStoreNode = Node.createURI(ConsertCore.ENTITY_STORE_URI);
				
		    	// We must introduce the created activity in the EntityStore, so we must create an update request
		    	// which includes updating the EntityStore
		    	Model teachingActivityModel = createTeachingActivityInstance(teachingInterval);
		    	
		    	StmtIterator it = teachingActivityModel.listStatements();
		    	for (;it.hasNext();) {
		    		Statement s = it.next();
		    		entityStoreData.addQuad(Quad.create(entityStoreNode, s.asTriple()));
		    	}
		    	
		    	assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ENTITY_UPDATE, new UpdateDataInsert(entityStoreData));
			}
			
			// ======== STEP 2: ASSERTION UUID CREATE
			Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.takesPlaceIn));
			assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ID_CREATE, new UpdateCreate(assertionUUIDNode));
			
			// ======== STEP 3: ASSERTION CONTENT
			QuadDataAcc assertionContent = new QuadDataAcc();
			assertionContent.addQuad(Quad.create(assertionUUIDNode, teachingActivityRes.asNode(), 
					SmartClassroom.takesPlaceIn.asNode(), SmartClassroom.EF210.asNode()));
			assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_CONTENT_UPDATE, new UpdateDataInsert(assertionContent));
			
			// ======== STEP 4: ASSERTION ANNOTATIONS
			Calendar now = timeService.getCalendarInstance();
			CalendarIntervalList validityIntervals = new CalendarIntervalList();
			validityIntervals.add(teachingInterval);
			
			SensorInstance sensorInstance = sensorInstances.get(sensorIdURI);
			List<Statement> assertionAnnotations = ContextModelUtils.createAnnotationStatements(
					assertionUUIDNode.getURI(), SmartClassroom.takesPlaceIn.getURI(), 
					ContextAssertionType.Profiled, now, validityIntervals, 1.0, sensorInstance.getIdResource().getURI());
			
			QuadDataAcc annotationContent = new QuadDataAcc();
			String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.takesPlaceIn.getURI());
			Node assertionStoreNode = Node.createURI(assertionStoreURI);
			
			for (Statement s : assertionAnnotations) {
				annotationContent.addQuad(Quad.create(assertionStoreNode, s.asTriple()));
			}
			
			assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ANNOTATION_UPDATE, new UpdateDataInsert(annotationContent));
		}
	    
	    return updates;
    }
	
	private class InformTeachingUpdateTask implements Runnable {
		@Override
        public void run() {
			String sensorIdURI = SmartClassroom.TeachingActivitySensor.getURI();
			System.out.println("[" + InformTeachingAdaptor.class.getSimpleName() + "] "
					+ "Generating update from sensor: " + sensorIdURI);
			
			List<UpdateRequest> updateRequests = deliverUpdates(sensorIdURI);
			if (updateRequests != null) {
	        	for (UpdateRequest update : updateRequests) {
	        		sensingAdaptor.deliverUpdate(getProvidedAssertion(), update);
	        	}
			}
			
        	if (updatesEnabled.get()) {
        		scheduleNextTask();
        	}
        }
		
		private void scheduleNextTask() {
			CalendarInterval teachingInterval = getCurrentTeachingInterval();
			Calendar now = timeService.getCalendarInstance();
			
			if (teachingInterval != null) {
				// if we are within a teaching interval (as is normally the case), we want to schedule
				// the new update for the next interval in the ordered teaching dates list 
				int intervalIndex = 0;
				
				// get the index of the current teachingInterval
				List<CalendarInterval> teachingDates = getTeachingDates();
				for (int index = 0; index < teachingDates.size(); index++) {
					CalendarInterval interval = teachingDates.get(index);
					if (interval.compareTo(teachingInterval) == 0) {
						intervalIndex = index;
						break;
					}
				}
				
				// proceed to next one
				intervalIndex++;
				
				// see where we are
				Calendar nextStart = null;
				if (intervalIndex == teachingDates.size()) {
					// we got to wait for next day
					nextStart = teachingDates.get(0).lowerLimit();
					nextStart.add(Calendar.DATE, 1);
				}
				else {
					// else we are still in this day
					nextStart = teachingDates.get(intervalIndex).lowerLimit();
				}
				
				int delay = (int)(nextStart.getTimeInMillis() - now.getTimeInMillis());
				teachingUpdateTask = teachingUpdateService.schedule(new InformTeachingUpdateTask(), delay, TimeUnit.MILLISECONDS);
			}
			else {
				int delay = getDelayToClosestTeachingInterval();
				teachingUpdateTask = teachingUpdateService.schedule(new InformTeachingUpdateTask(), delay, TimeUnit.MILLISECONDS);
			}
		}
	}
}
