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

@Component(name = "StartCMMPlatformCommand")
@Provides
@Instantiate(name = "start-cmm-platform-command")
public class StartCMMPlatformCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "start-cmm-platform";
		
	private BundleContext bundleContext;
	
	public StartCMMPlatformCommand(BundleContext context) {
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
		
		// Start the platform, which automatically searches for the default Provisioning Group
		platformMgmtService.startCMMPlatform();
		
		return null;
	}
	
	@Override
	public String getDescription() {
		return "Starts the CMM Platform for the simulation.\n\t" + super.getDescription();
	}
}
