package org.aimas.ami.cmm.simulation;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;

import org.aimas.ami.cmm.api.CMMOperationFuture;
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

@Component(name = "StartConsertCommand")
@Provides
@Instantiate(name = "start-consert-cmm-command")
public class StartConsertCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "start-consert-cmm";
	
	private static final String CONTEXT_DIMENSION_URI = "http://pervasive.semanticweb.org/ont/2004/06/person#locatedIn";
	private static final String CONTEXT_DOMAIN_VALUE_URI = "http://pervasive.semanticweb.org/ont/2014/07/smartclassroom/bootstrap#EF210";
	
	private static final String AIR_CONDITIONING_APP = "AirConditioning";
	private static final String PROJECTOR_APP = "Projector";
	private static final String ALICE_APP = "AliceUsage";
	private static final String BOB_APP = "BobUsage";
	private static final String CECILLE_APP = "CecilleUsage";
	
	//private static final String CONSERT_ENGINE_BUNDLE = "consert-engine.engine.jar";
	//private static final String JADE_OSGI_BUNDLE = "jade.jadeOsgi-1.0.0.jar";
	//private static final String CONSERT_CMM_AGENT_BUNDLE = "consert-middleware.agent-bundle.jar";
	
	private BundleContext bundleContext;
	
	public StartConsertCommand(BundleContext context) {
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
		/* Here we have to start the CONSERT CMM platform on which our simulated application will run.
		 * To do so, we look for the CMMPlatformManagementService that our default instance bundle for this 
		 * application will have installed.
		 */
		//String bundleLocationBase = "file:./bundle";
		//Bundle consertCMMAgents = bundleContext.installBundle(bundleLocationBase + "/" + CONSERT_CMM_AGENT_BUNDLE);
		//consertCMMAgents.start();
		
		ServiceReference<CMMPlatformManagementService> platformMgmtServiceRef = 
				bundleContext.getServiceReference(CMMPlatformManagementService.class);
		CMMPlatformManagementService platformMgmtService = bundleContext.getService(platformMgmtServiceRef);
		
		// First we start the platform, which automatically searches for the default Provisioning Group
		platformMgmtService.startCMMPlatform();
		
		// Then, as this is a simulation, we launch the other "users" of the SmartClassroom: the projector, the
		// air conditioning unit and the 3 persons
		CMMOperationFuture<?> op = platformMgmtService.installProvisioningGroup(AIR_CONDITIONING_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		try {
			op.awaitOperation();
		}
		catch (ExecutionException ex) {
			ex.printStackTrace();
		}
		
		platformMgmtService.installProvisioningGroup(PROJECTOR_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		platformMgmtService.installProvisioningGroup(ALICE_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		platformMgmtService.installProvisioningGroup(BOB_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		platformMgmtService.installProvisioningGroup(CECILLE_APP, CONTEXT_DIMENSION_URI, CONTEXT_DOMAIN_VALUE_URI);
		
		return null;
	}
	
	@Override
	public String getDescription() {
		return "Starts the CONSERT Context Management Middleware Platform for the EF210 room of the SmartClassroom simulation.\n\t" + super.getDescription();
	}
}
