package org.aimas.ami.cmm.simulation.commands;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.cmm.simulation.users.UserQueryCatalog;
import org.aimas.ami.contextrep.engine.api.ContextResultSet;
import org.aimas.ami.contextrep.engine.api.QueryResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;
import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(name = "SimulateUserCallCommand")
@Provides
@Instantiate(name = "simulate-user-call-command")
public class SimulateUserCallCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "simulate-call";
	
	private static final String PERSONAL_APP_ID_NAME = "AlicePersonal";
	private static final String PERSONAL_ADAPTOR_NAME = "CtxUser" + "__" + PERSONAL_APP_ID_NAME;
	
	
	@Requires
	private SimulationManager simulationManager;
	
	@Requires(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + PERSONAL_APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + PERSONAL_ADAPTOR_NAME + "))")
	private ApplicationUserAdaptor applicationAdaptor;
	
	
	public SimulateUserCallCommand() {
		addSignature(new Signature(new String[] {"from", "to"})); // Adding a signature, with two parameters: from, to
	}
	
	@Override
    public String getName() {
		return COMMAND_NAME;
    }

	@Override
    public Object execute(InputStream in, PrintStream out, JSONObject parameters, Signature signature) throws Exception {
		String fromUser = parameters.getString("from");
		String toUser = parameters.getString("to");
		
		System.out.println("["+getClass().getSimpleName()+"] Simulating a CALL from user: " + fromUser + " to user: " + toUser);
		System.out.println("["+getClass().getSimpleName()+"] The simulation implies just a short "
				+ "lived subscription to the current availability status of the callee.");
		
		// create subscription
		makeSubscription();
		
		// cancel it after 15 seconds
		subscriptionCancelExecutor = Executors.newSingleThreadScheduledExecutor();
		subscriptionCancelExecutor.schedule(new CancelSubscriptionTask(), 15, TimeUnit.SECONDS);
		
	    return null;
    }
	
	private void makeSubscription() {
		if (availabilitySubscriptionId == null) {
			availabilityNotifier = new AvailabilityInfoNotifier();
			
			Query availabilitySubscription = QueryFactory.create(UserQueryCatalog.aliceAvailabilityQuery);
			try {
				availabilitySubscriptionId = applicationAdaptor.localSubscribe(availabilitySubscription, availabilityNotifier, 0);
	        }
	        catch (DisconnectedQueryHandlerException e) {
	            e.printStackTrace();
	        }
		}
	}
	
	private ScheduledExecutorService subscriptionCancelExecutor;
	private String availabilitySubscriptionId;
	private AvailabilityInfoNotifier availabilityNotifier;
	
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
	
	
	private class CancelSubscriptionTask implements Runnable {
		
		@Override
        public void run() {
	        if (availabilitySubscriptionId != null) {
	        	applicationAdaptor.cancelSubscription(availabilitySubscriptionId);
	        	availabilityNotifier = null;
	        	
	        	subscriptionCancelExecutor.shutdown();
	        }
        }
		
	}
}
