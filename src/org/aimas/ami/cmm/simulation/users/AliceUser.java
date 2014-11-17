package org.aimas.ami.cmm.simulation.users;

import org.aimas.ami.cmm.api.ApplicationUserAdaptor;
import org.aimas.ami.contextrep.engine.api.InsertionHandler;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Unbind;

import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(publicFactory=false)
@Instantiate
public class AliceUser extends PersonUser {
	public static final String ALICE_NAME = "Alice";
	public static final String ALICE_SMARTPHONE_ADDRESS = "01:23:45:67:89:ab";
	public static final String ALICE_ADAPTOR_NAME = "CtxUser_Alice";
	
	public AliceUser() {
	    super(ALICE_NAME, ALICE_SMARTPHONE_ADDRESS);
    }
	
	@Bind
	private void bindSimulationManager(SimulationManager simulationManager) {
		System.out.println("[" + AliceUser.class.getName() + "] Simulation Manager dependency resolved.");
		super.setSimulationManager(simulationManager);
	}
	
	@Unbind
	private void unbindSimulationManager(SimulationManager simulationManager) {
		super.setSimulationManager(null);
	}
	
	@Bind(filter=""
			+ "(&(" + ApplicationUserAdaptor.APP_IDENTIFIER_PROPERTY + "=" + APP_ID_NAME + ")"
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + ALICE_ADAPTOR_NAME + "))")
	private void bindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		System.out.println("[" + AliceUser.class.getName() + "] Application Adaptor dependency resolved.");
		super.setApplicationAdaptor(applicationAdaptor);
	}
	
	@Unbind
	private void unbindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		super.setApplicationAdaptor(null);
	}
	
	@Bind
	private void bindBootstrapInsertionAdaptor(InsertionHandler insertionAdaptor) {
		System.out.println("[" + AliceUser.class.getName() + "] Bootstrap dependency resolved.");
		super.setBootstrapInsertionAdaptor(insertionAdaptor);
	}
	
	@Unbind
	private void unbindBootstrapInsertionAdaptor(InsertionHandler insertionAdaptor) {
		super.setBootstrapInsertionAdaptor(null);
	}
}
