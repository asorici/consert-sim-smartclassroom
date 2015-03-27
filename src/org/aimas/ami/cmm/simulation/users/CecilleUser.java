package org.aimas.ami.cmm.simulation.users;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component
//@Instantiate
public class CecilleUser extends PersonUser {
	public static final String APP_ID_NAME = "CecilleUsage";
	public static final String CECILLE_NAME = "Cecille";
	public static final String CECILLE_SMARTPHONE_ADDRESS = "01:23:45:67:89:ad";
	public static final String CECILLE_ADAPTOR_NAME = "CtxUser" + "__" + APP_ID_NAME;
	
	public CecilleUser() {
	    super(CECILLE_NAME, CECILLE_SMARTPHONE_ADDRESS);
    }
	
	@Validate
	private void start() {
		// describe myself
		describeSelf();
		
		// start presence task
		startPresenceTask(null);
	}
	
	@Invalidate
	private void stop() {
		// cancel the ad-hoc subscription
		cancelAdHocSubscription();
		
		// cancel user presence task and shutdown the presence request executor
		if (userPresenceRequestTask != null) {
			userPresenceRequestTask.cancel(false);
			userPresenceRequestExecutor.shutdown();
		}
	}
	
	@Bind
	private void bindSimulationManager(SimulationManager simulationManager) {
		super.setSimulationManager(simulationManager);
	}
	
	@Unbind
	private void unbindSimulationManager(SimulationManager simulationManager) {
		super.setSimulationManager(null);
	}
	
	@Bind(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + CECILLE_ADAPTOR_NAME + "))")
	private void bindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		super.setApplicationAdaptor(applicationAdaptor);
	}
	
	@Unbind
	private void unbindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		super.setApplicationAdaptor(null);
	}
}
