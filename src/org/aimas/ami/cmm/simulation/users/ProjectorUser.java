package org.aimas.ami.cmm.simulation.users;

import java.util.HashMap;
import java.util.Map;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.cmm.api.DisconnectedQueryHandlerException;
import org.aimas.ami.cmm.api.QueryNotificationHandler;
import org.aimas.ami.contextrep.engine.api.QueryResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import fr.liglab.adele.icasa.ContextManager;
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

@Component(publicFactory=false)
@Instantiate
public class ProjectorUser implements PersonListener {
	private static final String APP_ID_NAME = "SmartClassroomSpec";
	private static final String ADAPTOR_NAME = "CtxUser_Projector";
	
	@Requires(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + ADAPTOR_NAME + "))")
	private ApplicationUserAdaptor applicationAdaptor;
	
	@Requires
	private ContextManager simulationManager;
	
	private Map<String, Query> usersPresentSubscription;
	private Map<String, Query> luminositySubscription;
	
	public ProjectorUser() {
		simulationManager.addListener(this);
	}
	
	private void subscribeUsersPresent() {
		if (usersPresentSubscription == null) {
			Query subscribeQuery = QueryFactory.create(UserQueryCatalog.usersPresentQuery);
			try {
	            String subscriptionId = applicationAdaptor.localSubscribe(subscribeQuery, new UsersPresentNotifier(), 0);
	            usersPresentSubscription = new HashMap<String, Query>();
	            usersPresentSubscription.put(subscriptionId, subscribeQuery);
            }
            catch (DisconnectedQueryHandlerException e) {
	            e.printStackTrace();
            }
		}
    }
	
	private void subscribeLuminosityUpdates() {
		if (luminositySubscription == null) {
			Query subscribeQuery = QueryFactory.create(UserQueryCatalog.getLuminosityQuery);
			try {
	            String subscriptionId = applicationAdaptor.localSubscribe(subscribeQuery, new LuminosityNotifier(), 0);
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
	private class UsersPresentNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult queryResult) {
			if (!queryResult.hasError()) {
				boolean userPresent = queryResult.getAskResult();
				if (userPresent) {
					subscribeLuminosityUpdates();
				}
				else {
					cancelLuminosityUpdates();
				}
			}
			else {
				System.out.println("[INFO "+ getClass().getName() +"] Error executing users exist query!!!");
				queryResult.getError().printStackTrace();
			}
        }

		@Override
        public void handleRefuse(Query query) {
			System.out.println("[INFO "+ getClass().getName() +"] The users exist query got refused!!!");
        }
	}
	
	private class LuminosityNotifier implements QueryNotificationHandler {
		@Override
        public void handleResultNotification(Query query, QueryResult queryResult) {
			if (!queryResult.hasError()) {
				ResultSet resultSet = queryResult.getResultSet();
				System.out.println("[INFO "+ getClass().getName() +"] Luminosity map:");
				/*
				while(resultSet.hasNext()) {
					QuerySolution sol = resultSet.next();
					String roomSection = sol.getResource("roomSection").getLocalName();
					int luminosity = sol.getLiteral("luminosity").getInt();
					System.out.println("	" + roomSection + ": " + luminosity);
				}
				*/
			}
			else {
				System.out.println("[INFO "+ getClass().getName() +"] Error executing get temperature query!!!");
				queryResult.getError().printStackTrace();
			}
        }

		@Override
        public void handleRefuse(Query query) {
			System.out.println("[INFO "+ getClass().getName() +"] The get temperature query got refused!!!");
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
		subscribeUsersPresent();
	}

	@Override
    public void personDeviceAttached(Person paramPerson, LocatedDevice paramLocatedDevice) {}

	@Override
    public void personDeviceDetached(Person paramPerson, LocatedDevice paramLocatedDevice) {}
}
