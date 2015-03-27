package org.aimas.ami.cmm.simulation.users;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.cmm.simulation.sensors.SmartClassroom;
import org.aimas.ami.contextrep.engine.api.ContextResultSet;
import org.aimas.ami.contextrep.engine.api.QueryResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.SimulationManager;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

@Component
public class DanUser implements PersonListener {
	public static final String ALICE_NAME = "Alice";
	public static final String DAN_NAME = "Dan";
	
	public static final String EF301_APP_ID_NAME = "DanUsage";
	public static final String EF301_DAN_ADAPTOR_NAME = "CtxUser" + "__" + EF301_APP_ID_NAME;
	
	private BundleContext bundleContext;
	
	public DanUser(BundleContext context) {
	    this.bundleContext = context;
    }
	
	@Validate
	private void start() {
		startAliceAvailabilitySubscription();
	}
	
	@Invalidate
	private void stop() {
		// cancel the subscription requesting Alice's availability status
		stopAliceAvailabilitySubscription();
		
	}
	
	
	@Requires
	private SimulationManager simulationManager;
	
	
	@Requires(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + EF301_APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + EF301_DAN_ADAPTOR_NAME + "))")
	private ApplicationUserAdaptor applicationAdaptor;
	
	
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
	}
	
	
	// AVAILABILITY SUBSCRIPTION AUXILIARIES
	////////////////////////////////////////////////////////////////////////////////////////////
	private String availabilitySubscriptionId;
	private AvailabilityInfoNotifier availabilityNotifier;
	
	private void startAliceAvailabilitySubscription() {
		if (availabilitySubscriptionId == null) {
			availabilityNotifier = new AvailabilityInfoNotifier();
			
			Query availabilitySubscription = QueryFactory.create(UserQueryCatalog.aliceAvailabilityQuery);
			try {
				availabilitySubscriptionId = applicationAdaptor.domainRangeSubscribe(
						availabilitySubscription, availabilityNotifier, 0, 
						SmartClassroom.MultiFunctionalRoom.getURI(), SmartClassroom.SchoolBuilding.getURI());
	        }
	        catch (DisconnectedQueryHandlerException e) {
	            e.printStackTrace();
	        }
		}
    }

	private void stopAliceAvailabilitySubscription() {
	    if (availabilitySubscriptionId != null) {
	    	applicationAdaptor.cancelSubscription(availabilitySubscriptionId);
	    	
	    	availabilitySubscriptionId = null;
	    	availabilityNotifier = null;
	    }
    }
	
	
	private class AvailabilityInfoNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult result) {
	        if (result.hasError()) {
	        	System.out.println("["+ getClass().getSimpleName() +"] Khamaka Brekker ... we have an error: " + result.getError());
	        }
	        else {
	        	ContextResultSet resultSet = result.getResultSet();
	        	while(resultSet.hasNext()) {
	        		QuerySolution solution = resultSet.nextSolution();
	        		Resource status = solution.getResource("status");
	        		Literal time = solution.getLiteral("time");
	        		
	        		System.out.println("["+ getClass().getSimpleName() +"] INFO: We received news that Alice is " 
	        				+ status.getLocalName() + " at " + time);
	        	}
	        }
        }

		@Override
        public void handleRefuse(Query query) {
	        System.out.println("[" + getClass().getSimpleName() + "] Oy. Khamaka Brekker: I got a refuse message for query:\n" + query);
        }
	}
}
