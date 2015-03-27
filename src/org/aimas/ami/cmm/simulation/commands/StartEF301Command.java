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

@Component(name = "StartEF301Command")
@Provides
@Instantiate(name = "start-ef301-command")
public class StartEF301Command extends AbstractCommand {
	private static final String COMMAND_NAME = "start-ef301";
	
	private static final String EF301_APP = "EF301";
	private static final String DAN_APP = "DanUsage";
	
	private static final String CONTEXT_DIMENSION_URI = "http://pervasive.semanticweb.org/ont/2004/06/person#locatedIn";
	private static final String CONTEXT_DOMAIN_VALUE_URI = "http://pervasive.semanticweb.org/ont/2014/07/smartclassroom/bootstrap#EF301";
	
	private BundleContext bundleContext;
	
	public StartEF301Command(BundleContext context) {
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
		
		platformMgmtService.installProvisioningGroup(EF301_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		platformMgmtService.installProvisioningGroup(DAN_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		
		return null;
	}
	
	@Override
	public String getDescription() {
		return "Starts the EF301 room simulation.\n\t" + super.getDescription();
	}
}
