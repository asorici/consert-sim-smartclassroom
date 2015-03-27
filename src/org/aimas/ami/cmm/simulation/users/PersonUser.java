package org.aimas.ami.cmm.simulation.users;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedCoordinatorException;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.cmm.sensing.ContextAssertionDescription;
import org.aimas.ami.cmm.simulation.sensors.SmartClassroom;
import org.aimas.ami.contextrep.engine.api.QueryResult;
import org.aimas.ami.contextrep.utils.ContextModelUtils;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.UpdateRequest;

import fr.liglab.adele.icasa.ContextManager;
import fr.liglab.adele.icasa.simulator.SimulationManager;

public class PersonUser {
	static final int PRESENCE_REQUEST_SECONDS_INTERVAL = 5;
	
	public static final int ASSERTION_ENTITY_UPDATE 	=	1;
	public static final int ASSERTION_ID_CREATE 		=	2;
	public static final int ASSERTION_CONTENT_UPDATE 	= 	3;
	public static final int ASSERTION_ANNOTATION_UPDATE = 	4;
	
	public static interface NumUserListener {
		public void userCountUpdated(String location, int userNumber);
	}
	
	protected boolean firstMove = true;
	protected Object guard = new Object();
	
	protected String name;
	protected String smartphoneAddress;
	
	protected ContextManager simulationManager;
	protected ApplicationUserAdaptor applicationAdaptor;  
	//protected InsertionHandler bootstrapInsertionAdaptor;
	
	protected Model personModel;
	protected boolean describedSelf = false;
	
	protected ScheduledExecutorService userPresenceRequestExecutor;
	protected ScheduledFuture<?> userPresenceRequestTask;
	
	protected Map<String, Query> adhocMeetingSubscription;
	
	protected PersonUser(String name, String smartphoneAddress) {
		this.name = name;
		this.smartphoneAddress = smartphoneAddress;
	}
	
	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}
	
	public void setApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		this.applicationAdaptor = applicationAdaptor;
	}
	
	/*
	public void setBootstrapInsertionAdaptor(InsertionHandler bootstrapInsertionAdaptor) {
		this.bootstrapInsertionAdaptor = bootstrapInsertionAdaptor;
	}
	*/
	
	protected void startPresenceTask(NumUserListener presenceListener) {
		userPresenceRequestExecutor = Executors.newSingleThreadScheduledExecutor();
		userPresenceRequestTask = userPresenceRequestExecutor.scheduleAtFixedRate(
					new UserPresenceTask(presenceListener), 0, PRESENCE_REQUEST_SECONDS_INTERVAL, TimeUnit.SECONDS);
	}
	
	protected void stopPresenceTask() {
		if (userPresenceRequestTask != null) {
			userPresenceRequestTask.cancel(false);
			userPresenceRequestExecutor.shutdown();
			
			userPresenceRequestExecutor = null;
			userPresenceRequestTask = null;
		}
	}
	
	
	// SELF DESCRIPTION
	////////////////////////////////////////////////////////////////////////////////////////////
	protected void describeSelf() {
		// Compose the EntityStore + profiled device ownership update request for the Person and Smartphone ContextEntities
		UpdateRequest personProfiledUpdate = new UpdateRequest();
		Node entityStoreNode = Node.createURI(ConsertCore.ENTITY_STORE_URI);
		
		// == STEP 1: person and device entities
		personModel = ModelFactory.createDefaultModel();
		Resource personResource = personModel.createResource(SmartClassroom.BOOTSTRAP_NS + name, SmartClassroom.Person);
		
		Resource smartphoneResource = personModel.createResource(SmartClassroom.BOOTSTRAP_NS + "Smartphone_" + name, SmartClassroom.Smartphone);
		smartphoneResource.addProperty(SmartClassroom.bluetoothMAC, smartphoneAddress, XSDDatatype.XSDstring);
		
		// == STEP 2: create the EntityStore update
		QuadDataAcc entityStoreData = new QuadDataAcc();
		StmtIterator it = personModel.listStatements();
    	for (;it.hasNext();) {
    		Statement s = it.next();
    		entityStoreData.addQuad(Quad.create(entityStoreNode, s.asTriple()));
    	}
    	
    	personProfiledUpdate.add(new UpdateDataInsert(entityStoreData));
		
    	// == STEP 3: create the profiled device ownership data
    	// ASSERTION UUID CREATE
		Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.hasUser));
		personProfiledUpdate.add(new UpdateCreate(assertionUUIDNode));
		
		// ASSERTION CONTENT
		QuadDataAcc assertionContent = new QuadDataAcc();
		assertionContent.addQuad(Quad.create(assertionUUIDNode, smartphoneResource.asNode(), 
			SmartClassroom.hasUser.asNode(), personResource.asNode()));
		personProfiledUpdate.add(new UpdateDataInsert(assertionContent));
		
		// ASSERTION ANNOTATIONS
		QuadDataAcc annotationContent = new QuadDataAcc();
		String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.hasUser.getURI());
		Node assertionStoreNode = Node.createURI(assertionStoreURI);
		annotationContent.addQuad(Quad.create(assertionStoreNode, assertionUUIDNode, 
				ConsertCore.CONTEXT_ASSERTION_RESOURCE.asNode(), SmartClassroom.hasUser.asNode()));
		annotationContent.addQuad(Quad.create(assertionStoreNode, assertionUUIDNode, 
				ConsertCore.CONTEXT_ASSERTION_TYPE_PROPERTY.asNode(), ConsertCore.TYPE_PROFILED.asNode()));
		personProfiledUpdate.add(new UpdateDataInsert(annotationContent));
		
		System.out.println("[" + PersonUser.class.getName() + " " + name + "] Describing myself");
		
		// == STEP 4: insert the new statement
		//bootstrapInsertionAdaptor.insertAssertion(personProfiledUpdate, InsertionHandler.TIME_BASED_UPDATE_MODE);
		ContextAssertionDescription profiledAssertionDesc = new ContextAssertionDescription(SmartClassroom.hasUser.getURI());
		
		try {
	        applicationAdaptor.sendProfiledAssertion(profiledAssertionDesc, personProfiledUpdate);
	        describedSelf = true;
		}
        catch (DisconnectedCoordinatorException e) {
	        e.printStackTrace();
        }
	}
	
	protected void subscribeForAdHocDiscussion() {
		synchronized(guard) {
			if (adhocMeetingSubscription == null) {
				Query subscribeQuery = QueryFactory.create(UserQueryCatalog.adHocMeetingQuery);
				try {
		            String subscriptionId = applicationAdaptor.localSubscribe(subscribeQuery, new AdhocDiscussionNotifier(), 0);
		            System.out.println("[INFO "+ getClass().getSimpleName() +"] SUBSCRIBING FOR AD-HOC MEETING QUERY WITH ID: " + subscriptionId);
		            
		            adhocMeetingSubscription = new HashMap<String, Query>();
		            adhocMeetingSubscription.put(subscriptionId, subscribeQuery);
	            }
	            catch (DisconnectedQueryHandlerException e) {
		            e.printStackTrace();
	            }
			}
		}
	}
	
	protected void cancelAdHocSubscription() {
		if (adhocMeetingSubscription != null) {
			String subscriptionId = adhocMeetingSubscription.keySet().iterator().next();
			System.out.println("[INFO "+ getClass().getSimpleName() +"] CANCELING SUBSCRIPTION FOR AD-HOC MEETING: " + subscriptionId);
			
			applicationAdaptor.cancelSubscription(subscriptionId);
			adhocMeetingSubscription = null;
		}
	}
	
	
	// SUBSCRIPTION RESULT NOTIFIERS
	////////////////////////////////////////////////////////////////////////////////////////////
	private class AdhocDiscussionNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult queryResult) {
			if (!queryResult.hasError()) {
				boolean isDiscussion = queryResult.getAskResult();
				if (isDiscussion) {
					System.out.println("[INFO "+ PersonUser.class.getName() +"] USER " + name + " IN AD-HOC DISCUSSION!!!!!!!!");
				}
			}
			else {
				System.out.println("[INFO "+ PersonUser.class.getName() +"] Error executing ad-hoc discussion query!!!");
				queryResult.getError().printStackTrace();
			}
        }

		@Override
        public void handleRefuse(Query query) {
	        System.out.println("[INFO "+ PersonUser.class.getName() +"] The ad-hoc discussion query got refused!!!");
        }
	}
	
	private class UserPresenceTask implements Runnable, QueryNotificationHandler {
		private NumUserListener presenceListener;
		
		public UserPresenceTask(NumUserListener listener) {
			this.presenceListener = listener;
		}
		
		@Override
        public void run() {
			Query presenceQuery = QueryFactory.create(UserQueryCatalog.numUsersQuery);
			try {
	            applicationAdaptor.submitLocalQuery(presenceQuery, this);
            }
            catch (DisconnectedQueryHandlerException e) {
	            e.printStackTrace();
            }
        }

		@Override
        public void handleResultNotification(Query query, QueryResult queryResult) {
			if (!queryResult.hasError()) {
				ResultSet resultSet = queryResult.getResultSet();
				
				if (resultSet.hasNext()) {
					QuerySolution solution = resultSet.nextSolution();
					int numUsers = solution.getLiteral("numUsers").getInt();
					
					//System.out.println("["+PersonUser.this.getClass().getSimpleName() +"] Received notification for numUsers in EF210 AmI Lab: " + numUsers);
					if (numUsers > 1) {
						subscribeForAdHocDiscussion();
					}
					else {
						synchronized(guard) {
							cancelAdHocSubscription();
						}
					}
					
					// if we have a presence listener, update result
					if (presenceListener != null) {
						presenceListener.userCountUpdated(SmartClassroom.EF210.getURI(), numUsers);
					}
				}
				else {
					System.out.println("[INFO "+ AirConditioningUser.class.getName() +"] NO RESULTS FOR numUsers QUERY");
				}
			}
			else {
				System.out.println("[INFO "+ AirConditioningUser.class.getName() +"] Error executing numUsers query!!!");
				queryResult.getError().printStackTrace();
			}
        }

		@Override
        public void handleRefuse(Query query) {
			System.out.println("[INFO "+ PersonUser.class.getName() +"] The num users query got refused!!!");
        }
	}
}
