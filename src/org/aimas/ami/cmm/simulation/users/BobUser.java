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
public class BobUser extends PersonUser {
	public static final String BOB_NAME = "Bob";
	public static final String BOB_SMARTPHONE_ADDRESS = "01:23:45:67:89:ac";
	public static final String BOB_ADAPTOR_NAME = "CtxUser_Bob";
	
	public BobUser() {
	    super(BOB_NAME, BOB_SMARTPHONE_ADDRESS);
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
			+   "(" + ApplicationUserAdaptor.ADAPTOR_NAME + "=" + BOB_ADAPTOR_NAME + "))")
	private void bindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		super.setApplicationAdaptor(applicationAdaptor);
	}
	
	@Unbind
	private void unbindApplicationAdaptor(ApplicationUserAdaptor applicationAdaptor) {
		super.setApplicationAdaptor(null);
	}
	
	@Bind
	private void bindBootstrapInsertionAdaptor(InsertionHandler insertionAdaptor) {
		super.setBootstrapInsertionAdaptor(insertionAdaptor);
	}
	
	@Unbind
	private void unbindBootstrapInsertionAdaptor(InsertionHandler insertionAdaptor) {
		super.setBootstrapInsertionAdaptor(null);
	}
}
