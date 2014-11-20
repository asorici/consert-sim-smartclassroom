package org.aimas.ami.cmm.simulation.users;

import java.util.HashMap;
import java.util.Map;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.cmm.simulation.sensors.SmartClassroom;
import org.aimas.ami.contextrep.engine.api.InsertionHandler;
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
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.SimulationManager;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

public class PersonUser implements PersonListener {
	public static final String APP_ID_NAME = "SmartClassroomSpec";
	
	protected boolean firstMove = true;
	protected Object guard = new Object();
	
	protected String name;
	protected String smartphoneAddress;
	
	protected ContextManager simulationManager;
	protected ApplicationUserAdaptor applicationAdaptor;  
	protected InsertionHandler bootstrapInsertionAdaptor;
	
	protected Model personModel;
	protected boolean describedSelf = false;
	
	protected Map<String, Query> numUsersSubscription;
	protected Map<String, Query> adhocMeetingSubscription;
	
	protected PersonUser(String name, String smartphoneAddress) {
		this.name = name;
		this.smartphoneAddress = smartphoneAddress;
	}
	
	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
		simulationManager.addListener(this);
	}
	
	public void setApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		this.applicationAdaptor = applicationAdaptor;
	}
	
	public void setBootstrapInsertionAdaptor(InsertionHandler bootstrapInsertionAdaptor) {
		this.bootstrapInsertionAdaptor = bootstrapInsertionAdaptor;
	}
	
	// SELF DESCRIPTION
	////////////////////////////////////////////////////////////////////////////////////////////
	private void describeSelf() {
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
		
		//System.out.println("[" + PersonUser.class.getName() + " " + name + "] Describing myself");
		
		// == STEP 4: insert the new statement
		bootstrapInsertionAdaptor.insert(personProfiledUpdate, InsertionHandler.CHANGE_BASED_UPDATE_MODE);
		
		describedSelf = true;
	}
	
	private void subscribeForNumUsers() {
		synchronized(guard) {
			if (numUsersSubscription == null) {
				Query subscribeQuery = QueryFactory.create(UserQueryCatalog.numUsersQuery);
				try {
		            String subscriptionId = applicationAdaptor.localSubscribe(subscribeQuery, new NumUsersNotifier(), 0);
		            numUsersSubscription = new HashMap<String, Query>();
		            numUsersSubscription.put(subscriptionId, subscribeQuery);
	            }
	            catch (DisconnectedQueryHandlerException e) {
		            e.printStackTrace();
	            }
			}
		}
	}
	
	private void subscribeForAdHocDiscussion() {
		synchronized(guard) {
			if (adhocMeetingSubscription == null) {
				Query subscribeQuery = QueryFactory.create(UserQueryCatalog.adHocMeetingQuery);
				try {
		            String subscriptionId = applicationAdaptor.localSubscribe(subscribeQuery, new AdhocDiscussionNotifier(), 0);
		            adhocMeetingSubscription = new HashMap<String, Query>();
		            adhocMeetingSubscription.put(subscriptionId, subscribeQuery);
	            }
	            catch (DisconnectedQueryHandlerException e) {
		            e.printStackTrace();
	            }
			}
		}
	}
	
	private void cancelSubscriptions() {
		if (adhocMeetingSubscription != null) {
			String subscriptionId = adhocMeetingSubscription.keySet().iterator().next();
			applicationAdaptor.cancelSubscription(subscriptionId);
			adhocMeetingSubscription = null;
		}
		
		if (numUsersSubscription != null) {
			String subscriptionId = numUsersSubscription.keySet().iterator().next();
			applicationAdaptor.cancelSubscription(subscriptionId);
			numUsersSubscription = null;
		}
	}
	
	// PERSON LISTENER
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    public void personAdded(Person person) {}
	
	@Override
    public void personDeviceAttached(Person person, LocatedDevice device) {}
	
	@Override
    public void personDeviceDetached(Person person, LocatedDevice device) {}
	
	@Override
    public void personMoved(Person person, Position position) {
		if (person.getName().equals(name)) {
			//System.out.println("[" + PersonUser.class.getName() + " " + name + "] Received personMoved notification to (" + position.x + ", " + position.y + ")");
			
			if (firstMove) {
				// on first ever move of the person, announce specific ContextEntities and EntityDescriptions
				if (!describedSelf) {
					describeSelf();
				}
				
				// register to be notified of how many users there are in the room
				subscribeForNumUsers();
			}
			else {
				cancelSubscriptions();
				//System.out.println("[INFO "+ PersonUser.class.getName() +"]: User " + name + " has left the room.");
			}
		}
	}
	
	@Override
    public void personRemoved(Person person) {}
	
	// SUBSCRIPTION RESULT NOTIFIERS
	////////////////////////////////////////////////////////////////////////////////////////////
	private class NumUsersNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult queryResult) {
			if (!queryResult.hasError()) {
				ResultSet resultSet = queryResult.getResultSet();
				if (resultSet.hasNext()) {
					QuerySolution solution = resultSet.nextSolution();
					int numUsers = solution.getLiteral("numUsers").getInt();
					
					//System.out.println("["+getClass().getSimpleName() + " " + name + " ] Received notification for numUsers : " + numUsers);
					if (numUsers > 1) {
						subscribeForAdHocDiscussion();
					}
					else {
						synchronized(guard) {
							if (adhocMeetingSubscription != null) {
								String subscriptionId = adhocMeetingSubscription.keySet().iterator().next();
								applicationAdaptor.cancelSubscription(subscriptionId);
								adhocMeetingSubscription = null;
							}
						}
					}
				}
				else {
					System.out.println("[INFO "+ PersonUser.class.getName() +"] NO RESULTS FOR numUsers QUERY");
				}
			}
			else {
				System.out.println("[INFO "+ PersonUser.class.getName() +"] Error executing numUsers query!!!");
				queryResult.getError().printStackTrace();
			}
        }

		@Override
        public void handleRefuse(Query query) {
	        System.out.println("[INFO "+ PersonUser.class.getName() +"] The num users query got refused!!!");
        }
	}
	
	
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
}
