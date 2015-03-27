package org.aimas.ami.cmm.simulation.users;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.contextrep.engine.api.QueryResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import fr.liglab.adele.icasa.ContextManager;
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

@Component
//@Instantiate(name="projector")
public class ProjectorUser implements PersonListener {
	private static final String APP_ID_NAME = "Projector";
	private static final String ADAPTOR_NAME = "CtxUser" + "__" + APP_ID_NAME;
	private static final int PRESENCE_REQUEST_SECONDS_INTERVAL = 5;
	
	@Requires(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + ADAPTOR_NAME + "))")
	private ApplicationUserAdaptor applicationAdaptor;
	
	@Requires
	private ContextManager simulationManager;
	
	private Map<String, Query> luminositySubscription;
	
	private ScheduledExecutorService userPresenceRequestExecutor;
	private ScheduledFuture<?> userPresenceRequestTask;
	
	@Validate
	private void start() {
		simulationManager.addListener(this);
		startPresenceTask();
	}
	
	@Invalidate
	private void stop() {
		cancelLuminosityUpdates();
		stopPresenceTask();
		
		simulationManager.removeListener(this);
	}
	
	private void startPresenceTask() {
		userPresenceRequestExecutor = Executors.newSingleThreadScheduledExecutor();
		userPresenceRequestTask = userPresenceRequestExecutor.scheduleAtFixedRate(
					new UserPresenceTask(), 0, PRESENCE_REQUEST_SECONDS_INTERVAL, TimeUnit.SECONDS);
    }
	
	private void stopPresenceTask() {
		if (userPresenceRequestTask != null) {
			userPresenceRequestTask.cancel(false);
			userPresenceRequestExecutor.shutdown();
		}
	}
	
	private void subscribeLuminosityUpdates() {
		if (luminositySubscription == null) {
			Query subscribeQuery = QueryFactory.create(UserQueryCatalog.getLuminosityQuery);
			try {
	            String subscriptionId = applicationAdaptor.localSubscribe(subscribeQuery, new LuminosityNotifier(), 0);
	            
	            //System.out.println("[INFO "+ getClass().getSimpleName() +"] SUBSCRIBING FOR LUMINOSITY QUERY WITH ID: " + subscriptionId);
	            luminositySubscription = new HashMap<String, Query>();
	            luminositySubscription.put(subscriptionId, subscribeQuery);
            }
            catch (DisconnectedQueryHandlerException e) {
	            e.printStackTrace();
            }
		}
	}
	
	private void cancelLuminosityUpdates() {
		if (luminositySubscription != null) {
			String subscriptionId = luminositySubscription.keySet().iterator().next();
			System.out.println("[INFO "+ getClass().getName() +"] CANCELING SUBSCRIPTION FOR LUMINOSITY: " + subscriptionId);
			
			applicationAdaptor.cancelSubscription(subscriptionId);
			luminositySubscription = null;
		}
	}
	
	// SUBSCRIPTION RESULT NOTIFIERS
	////////////////////////////////////////////////////////////////////////////////////////////
	private class LuminosityNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult queryResult) {
			if (!queryResult.hasError()) {
				/*
				ResultSet resultSet = queryResult.getResultSet();
				System.out.println("[INFO "+ getClass().getName() +"] Luminosity map:");
				
				while(resultSet.hasNext()) {
					QuerySolution sol = resultSet.next();
					String roomSection = sol.getResource("roomSection").getLocalName();
					int luminosity = sol.getLiteral("luminosity").getInt();
					System.out.println("	" + roomSection + ": " + luminosity);
				}
				*/
			}
			else {
				System.out.println("[INFO "+ getClass().getName() +"] Error executing get luminostity query!!!");
				queryResult.getError().printStackTrace();
			}
        }

		@Override
        public void handleRefuse(Query query) {
			System.out.println("[INFO "+ getClass().getName() +"] The get temperature query got refused!!!");
        }
	}
	
	private class UserPresenceTask implements Runnable, QueryNotificationHandler {

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
					
					//System.out.println("["+ProjectorUser.class.getSimpleName() +"] Received notification for numUsers : " + numUsers);
					if (numUsers >= 1) {
						subscribeLuminosityUpdates();
					}
					else {
						cancelLuminosityUpdates();
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
	
	// PERSON LISTENER
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
    public void personAdded(Person paramPerson) {}

	@Override
    public void personRemoved(Person paramPerson) {}

	@Override
    public void personMoved(Person paramPerson, Position paramPosition) {
		//startPresenceTask();
	}

	@Override
    public void personDeviceAttached(Person paramPerson, LocatedDevice paramLocatedDevice) {}

	@Override
    public void personDeviceDetached(Person paramPerson, LocatedDevice paramLocatedDevice) {}
}
