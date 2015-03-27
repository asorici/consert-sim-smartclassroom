package org.aimas.ami.cmm.simulation.commands;

import java.io.InputStream;
import java.io.PrintStream;

import org.aimas.ami.cmm.api.CMMPlatformManagementService;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import fr.liglab.adele.icasa.ContextManager;
import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;

@Component(name = "StartAlicePersonalCommand")
@Provides
@Instantiate(name = "start-alice-personal-command")
public class StartAlicePersonalCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "start-alice-personal";
	
	private static final String ALICE_PERSONAL_APP = "AlicePersonal";
	
	private BundleContext bundleContext;
	
	public StartAlicePersonalCommand(BundleContext context) {
		bundleContext = context;
		
		addSignature(new Signature(new String[0])); // Adding an empty signature, without parameters
	}
	
	@Requires
	private ContextManager simulationManager;
	
	@Override
	public String getName() {
		return COMMAND_NAME;
	}
	
	@Override
	public Object execute(InputStream input, PrintStream output, JSONObject parameters, Signature signature) throws Exception {
		
		ServiceReference<CMMPlatformManagementService> platformMgmtServiceRef = 
				bundleContext.getServiceReference(CMMPlatformManagementService.class);
		CMMPlatformManagementService platformMgmtService = bundleContext.getService(platformMgmtServiceRef);
		
		System.out.println("["+getClass().getSimpleName()+"] STARTING ALICE PERSONAL APP.");
		platformMgmtService.installProvisioningGroup(ALICE_PERSONAL_APP, null, null);
		
		return null;
	}
	
	@Override
	public String getDescription() {
		return "Starts the AlicePersonal CMU.\n\t" + super.getDescription();
	}
}
