package org.aimas.ami.cmm.simulation.users;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedCoordinatorException;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.cmm.sensing.ContextAssertionAdaptor;
import org.aimas.ami.cmm.sensing.ContextAssertionDescription;
import org.aimas.ami.cmm.simulation.sensors.SmartClassroom;
import org.aimas.ami.cmm.simulation.users.PersonUser.NumUserListener;
import org.aimas.ami.contextrep.engine.api.ContextResultSet;
import org.aimas.ami.contextrep.engine.api.QueryResult;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.resources.CMMConstants;
import org.aimas.ami.contextrep.resources.TimeService;
import org.aimas.ami.contextrep.utils.ContextModelUtils;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.BindingPolicy;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.sparql.modify.request.UpdateDeleteInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.SimulationManager;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

@Component
public class AliceUser extends PersonUser implements PersonListener, NumUserListener {
	public static final String ALICE_NAME = "Alice";
	public static final String ALICE_SMARTPHONE_ADDRESS = "01:23:45:67:89:ab";
	
	public static final String LAB_APP_ID_NAME = "AliceUsage";
	public static final String LAB_ALICE_ADAPTOR_NAME = "CtxUser" + "__" + LAB_APP_ID_NAME;
	
	public static final String BUILDING_APP_ID_NAME = "AliceBuildingUsage";
	public static final String BUILDING_ALICE_ADAPTOR_NAME = "CtxUser" + "__" + BUILDING_APP_ID_NAME;
	
	public static final String PERSONAL_APP_ID_NAME = "AlicePersonal";
	public static final String PERSONAL_ADAPTOR_NAME = "CtxUser" + "__" + PERSONAL_APP_ID_NAME;
	
	private BundleContext bundleContext;
	private ApplicationUserAdaptor csbuildingApplicationAdapter;
	private ApplicationUserAdaptor personalApplicationAdapter;
	
	private Zone currentZone;
	
	public AliceUser(BundleContext context) {
	    super(ALICE_NAME, ALICE_SMARTPHONE_ADDRESS);
	    this.bundleContext = context;
    }
	
	@Validate
	private void start() {
		// describe myself
		describeSelf();
		
		// start presence task
		startPresenceTask(this);
		//startPresenceTask(null);
	}
	
	@Invalidate
	private void stop() {
		// cancel the ad-hoc subscription
		cancelAdHocSubscription();
		
		// cancel user presence task and shutdown the presence request executor
		if (userPresenceRequestTask != null) {
			userPresenceRequestTask.cancel(false);
			userPresenceRequestExecutor.shutdown();
			
			userPresenceRequestTask = null;
			userPresenceRequestExecutor = null;
		}
	}
	
	@Bind
	private void bindSimulationManager(SimulationManager simulationManager) {
		System.out.println("[" + AliceUser.class.getSimpleName() + "] Simulation Manager dependency resolved.");
		super.setSimulationManager(simulationManager);
		
		// add ourselves as a PersonListener
		simulationManager.addListener(this);
	}
	
	@Unbind
	private void unbindSimulationManager(SimulationManager simulationManager) {
		super.setSimulationManager(null);
	}
	
	
	@Bind(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + LAB_APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + LAB_ALICE_ADAPTOR_NAME + "))")
	private void bindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		System.out.println("[" + AliceUser.class.getName() + "] Application Adaptor dependency resolved.");
		super.setApplicationAdaptor(applicationAdaptor);
	}
	
	@Unbind
	private void unbindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		super.setApplicationAdaptor(null);
	}
	
	
	@Bind(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + BUILDING_APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + BUILDING_ALICE_ADAPTOR_NAME + "))")
	private void bindCSBuildingApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		System.out.println("[" + AliceUser.class.getName() + "] CSBuilding Application Adaptor dependency resolved.");
		this.csbuildingApplicationAdapter = applicationAdaptor;
	}
	
	@Unbind
	private void unbindCSBuildingApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		this.csbuildingApplicationAdapter = null;
	}
	
	
	@Bind(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + PERSONAL_APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + PERSONAL_ADAPTOR_NAME + "))")
	private void bindPersonalApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		System.out.println("[" + AliceUser.class.getName() + "] Personal Application Adaptor dependency resolved.");
		this.personalApplicationAdapter = applicationAdaptor;
	}
	
	@Unbind
	private void unbindPersonalApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		this.personalApplicationAdapter = null;
	}
	
	
	@Requires(policy=BindingPolicy.DYNAMIC_PRIORITY,
			  filter="(" + CMMConstants.CONSERT_APPLICATION_ID_PROP + "=" + "AliceUsage" + ")")
	private TimeService timeService;
	
	// PERSON LISTENER
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    public void personAdded(Person person) {}
	
	@Override
    public void personDeviceAttached(Person person, LocatedDevice device) {}
	
	@Override
    public void personDeviceDetached(Person person, LocatedDevice device) {}
	
	@Override
    public void personRemoved(Person person) {}
	
	@Override
    public void personMoved(Person person, Position oldPosition) {
		Position newPosition = person.getCenterAbsolutePosition();
		
		if (person.getName().equals(name)) {
			currentZone = simulationManager.getZoneFromPosition(newPosition);
			
			if (currentZone != null && currentZone.getId().contains("EF210")) {
				subscribeLocationInfo();
			}
			else {
				if (currentZone != null && currentZone.getId().contains("CSBuilding")) {
					cancelLocationSubscription();
					announceAvailabilityInBuilding();
				}
			}
		}		
	}
	
	
	// PROFILED LOCATION AUXILIARIES
	////////////////////////////////////////////////////////////////////////////////////////////
	private String locationInfoSubscriptionId;
	private LocationInfoNotifier locationInfoNotifier;
	
	private void subscribeLocationInfo() {
		
		if (locationInfoSubscriptionId == null) {
			locationInfoNotifier = new LocationInfoNotifier();
			Query locationInfoSubscription = QueryFactory.create(UserQueryCatalog.whereAliceQuery);
			try {
				System.out.println("[" + getClass().getSimpleName() + "] INFO: Subscribing for location information");
				locationInfoSubscriptionId = applicationAdaptor.localSubscribe(locationInfoSubscription, locationInfoNotifier, 0);
	        }
	        catch (DisconnectedQueryHandlerException e) {
	            e.printStackTrace();
	        }
		}
    }

	private void cancelLocationSubscription() {
	    if (locationInfoSubscriptionId != null) {
	    	applicationAdaptor.cancelSubscription(locationInfoSubscriptionId);
	    	locationInfoSubscriptionId = null;
	    	locationInfoNotifier = null;
	    }
    }
	
	
	private class LocationInfoNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult result) {
	        if (result.hasError()) {
	        	System.out.println("["+ getClass().getSimpleName() +"] Hamaka Brekker ... we have an error: " + result.getError());
	        }
	        else {
	        	ContextResultSet resultSet = result.getResultSet();
	        	while(resultSet.hasNext()) {
	        		QuerySolution solution = resultSet.nextSolution();
	        		Resource location = solution.getResource("loc");
	        		
	        		System.out.println("[" + getClass().getSimpleName() + "] INFO: Received Notification for Alice Location Info: " + location);
	        		
	        		Resource aliceRes = ResourceFactory.createResource(SmartClassroom.BOOTSTRAP_NS + ALICE_NAME);
	        		Statement profiledLocationStatement = ResourceFactory.createStatement(aliceRes, SmartClassroom.locatedIn, location);
	        		Statement locationEntity = ResourceFactory.createStatement(location, RDF.type, SmartClassroom.MultiFunctionalRoom); 
	        		
	        		List<Statement> staticStatements = new LinkedList<Statement>();
	        		staticStatements.add(locationEntity);
	        		
	        		Map<Integer, Update> assertionUpdate = createBinaryProfiledAssertionUpdate(profiledLocationStatement, staticStatements);
        			UpdateRequest updateReq = new UpdateRequest();
        			
        			Update entityStoreUpdate = assertionUpdate.get(ASSERTION_ENTITY_UPDATE);
        			Update idCreate = assertionUpdate.get(ASSERTION_ID_CREATE);
        			Update contentUpdate = assertionUpdate.get(ASSERTION_CONTENT_UPDATE);
        			Update annotationUpdate = assertionUpdate.get(ASSERTION_ANNOTATION_UPDATE);
        			
        			if (entityStoreUpdate != null) {
        				updateReq.add(entityStoreUpdate);
        			}
        			updateReq.add(idCreate);
        			updateReq.add(contentUpdate);
        			updateReq.add(annotationUpdate);
        			
        			// send the profiled location update through the personal application adapter
        			ContextAssertionDescription profiledLocationDesc = new ContextAssertionDescription(SmartClassroom.locatedIn.getURI());
        			profiledLocationDesc.addSupportedAnnotationURI(ConsertAnnotation.DATETIME_TIMESTAMP.getURI());
        			profiledLocationDesc.addSupportedAnnotationURI(ConsertAnnotation.NUMERIC_VALUE_CERTAINTY.getURI());
        			profiledLocationDesc.addSupportedAnnotationURI(ConsertAnnotation.SOURCE_ANNOTATION.getURI());
        			try {
        				//System.out.println("[" + getClass().getSimpleName() + "] Sending profiled Alice locatedIn Assertion:\n" + updateReq);
        				//System.out.println();
        				//System.out.println();
        				personalApplicationAdapter.sendProfiledAssertion(profiledLocationDesc, updateReq);
                    }
                    catch (DisconnectedCoordinatorException e) {
	                    e.printStackTrace();
                    }
	        	}
	        }
        }

		@Override
        public void handleRefuse(Query query) {
	        System.out.println("[" + getClass().getSimpleName() + "] Oy. Khamaka Brekker: I got a refuse message for query:\n" + query);
        }
	}
	
	// AVAILABILITY STATUS AUXILIARIES
	////////////////////////////////////////////////////////////////////////////////////////////
	private void announceAvailabilityInBuilding() {
		Resource aliceRes = ResourceFactory.createResource(SmartClassroom.BOOTSTRAP_NS + ALICE_NAME);
		
		Statement profiledAvailabilityStatement = ResourceFactory.createStatement(aliceRes, 
				SmartClassroom.hasAvailabilityStatus, SmartClassroom.Free);
		Statement personEntity = ResourceFactory.createStatement(aliceRes, RDF.type, SmartClassroom.Person); 
		Statement statusEntity = ResourceFactory.createStatement(SmartClassroom.Free, RDF.type, SmartClassroom.AvailabilityStatus);
		
		List<Statement> staticStatements = new LinkedList<Statement>();
		staticStatements.add(personEntity);
		staticStatements.add(statusEntity);
		
		
		Map<Integer, Update> assertionUpdate = 
				createBinaryProfiledAssertionUpdate(profiledAvailabilityStatement, staticStatements);
		UpdateRequest updateReq = new UpdateRequest();
		
		Update entityStoreUpdate = assertionUpdate.get(ASSERTION_ENTITY_UPDATE);
		Update idCreate = assertionUpdate.get(ASSERTION_ID_CREATE);
		Update contentUpdate = assertionUpdate.get(ASSERTION_CONTENT_UPDATE);
		Update annotationUpdate = assertionUpdate.get(ASSERTION_ANNOTATION_UPDATE);
		
		if (entityStoreUpdate != null) {
			updateReq.add(entityStoreUpdate);
		}
		updateReq.add(idCreate);
		updateReq.add(contentUpdate);
		updateReq.add(annotationUpdate);
		
		// send the profiled location update through the personal application adapter
		ContextAssertionDescription profiledAvailabilityDesc = new ContextAssertionDescription(SmartClassroom.hasAvailabilityStatus.getURI());
		profiledAvailabilityDesc.addSupportedAnnotationURI(ConsertAnnotation.DATETIME_TIMESTAMP.getURI());
		profiledAvailabilityDesc.addSupportedAnnotationURI(ConsertAnnotation.NUMERIC_VALUE_CERTAINTY.getURI());
		profiledAvailabilityDesc.addSupportedAnnotationURI(ConsertAnnotation.SOURCE_ANNOTATION.getURI());
		
		try {
            csbuildingApplicationAdapter.sendProfiledAssertion(profiledAvailabilityDesc, updateReq);
        }
        catch (DisconnectedCoordinatorException e) {
            e.printStackTrace();
        }
    }
	
	// USER COUNT STATUS AUXILIARIES
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    public void userCountUpdated(String locationURI, int userNumber) {
		Resource location = ResourceFactory.createResource(locationURI);
		Statement profiledUserCountStatement = ResourceFactory.createStatement(location, AlicePersonal.hasPersonCount, 
				ResourceFactory.createTypedLiteral(new Integer(userNumber)));
		
		Map<Integer, Update> assertionUpdate = createBinaryProfiledAssertionUpdate(profiledUserCountStatement, null);
		UpdateRequest updateReq = new UpdateRequest();
		
		Update entityStoreUpdate = assertionUpdate.get(ASSERTION_ENTITY_UPDATE);
		Update idCreate = assertionUpdate.get(ASSERTION_ID_CREATE);
		Update contentUpdate = assertionUpdate.get(ASSERTION_CONTENT_UPDATE);
		Update annotationUpdate = assertionUpdate.get(ASSERTION_ANNOTATION_UPDATE);
		
		if (entityStoreUpdate != null) {
			updateReq.add(entityStoreUpdate);
		}
		updateReq.add(idCreate);
		updateReq.add(contentUpdate);
		updateReq.add(annotationUpdate);
		
		// send the profiled location update through the personal application adapter
		ContextAssertionDescription profiledUserCountDesc = new ContextAssertionDescription(AlicePersonal.hasPersonCount.getURI());
		profiledUserCountDesc.addSupportedAnnotationURI(ConsertAnnotation.DATETIME_TIMESTAMP.getURI());
		profiledUserCountDesc.addSupportedAnnotationURI(ConsertAnnotation.NUMERIC_VALUE_CERTAINTY.getURI());
		profiledUserCountDesc.addSupportedAnnotationURI(ConsertAnnotation.SOURCE_ANNOTATION.getURI());
		
		try {
			//System.out.println("[" + getClass().getSimpleName() + "] Sending profiled UserCount Assertion:\n" + updateReq);
			//System.out.println();
			//System.out.println();
			personalApplicationAdapter.sendProfiledAssertion(profiledUserCountDesc, updateReq);
        }
        catch (DisconnectedCoordinatorException e) {
            e.printStackTrace();
        }
    }
	
	
	
	// AUXILIARY FOR CREATING PROFILED UPDATES
	////////////////////////////////////////////////////////////////////////////////////////////
	private Map<Integer, Update> createBinaryProfiledAssertionUpdate(Statement binaryAssertion, List<Statement> staticUpdates) {
		Map<Integer, Update> assertionUpdate = new HashMap<Integer, Update>();
		Resource aliceRes = ResourceFactory.createResource(SmartClassroom.BOOTSTRAP_NS + ALICE_NAME);
		
		// STEP 1 - insert the static updates - perform a delete/insert to avoid duplicates
		if (staticUpdates != null && !staticUpdates.isEmpty()) {
			UpdateDeleteInsert entityStoreUpdate = new UpdateDeleteInsert();
			Node entityStoreNode = Node.createURI(ConsertCore.ENTITY_STORE_URI);
	    	
			for (Statement st : staticUpdates) {
				entityStoreUpdate.getDeleteAcc().addQuad(Quad.create(entityStoreNode, st.asTriple())); 
				entityStoreUpdate.getInsertAcc().addQuad(Quad.create(entityStoreNode, st.asTriple()));
			}
			
	    	assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ENTITY_UPDATE, entityStoreUpdate);
		}
    	
		// ======== STEP 2: ASSERTION UUID CREATE
		Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(binaryAssertion.getPredicate()));
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ID_CREATE, new UpdateCreate(assertionUUIDNode));
		
		// ======== STEP 3: ASSERTION CONTENT
		QuadDataAcc assertionContent = new QuadDataAcc();
		assertionContent.addQuad(Quad.create(assertionUUIDNode, binaryAssertion.asTriple()));
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_CONTENT_UPDATE, new UpdateDataInsert(assertionContent));
		
		// ======== STEP 4: ASSERTION ANNOTATIONS
		Calendar now = timeService.getCalendarInstance();
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		//System.out.println("["+ getClass().getName() + "] submitted timestamp: " + formatter.format(now.getTime()));
		//System.out.println("["+ getClass().getName() + "] submitted validityInterval: " + validityIntervals);
		
		List<Statement> assertionAnnotations = ContextModelUtils.createAnnotationStatements(
				assertionUUIDNode.getURI(), binaryAssertion.getPredicate().getURI(), 
				ContextAssertionType.Profiled, now, null, 1.0, aliceRes.getURI());
		
		QuadDataAcc annotationContent = new QuadDataAcc();
		String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(binaryAssertion.getPredicate().getURI());
		Node assertionStoreNode = Node.createURI(assertionStoreURI);
		
		for (Statement s : assertionAnnotations) {
			annotationContent.addQuad(Quad.create(assertionStoreNode, s.asTriple()));
		}
		
		assertionUpdate.put(ContextAssertionAdaptor.ASSERTION_ANNOTATION_UPDATE, new UpdateDataInsert(annotationContent));
		
		return assertionUpdate;
    }
}
