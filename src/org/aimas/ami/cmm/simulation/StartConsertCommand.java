package org.aimas.ami.cmm.simulation;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import fr.liglab.adele.icasa.ContextManager;
import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;

@Component(name = "StartConsertCommand")
@Provides
@Instantiate(name = "start-consert-cmm-command")
public class StartConsertCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "start-consert-cmm";
	
	private static final String CONSERT_ENGINE_BUNDLE = "consert-engine.engine.jar";
	private static final String CONSERT_CMM_AGENT_BUNDLE = "consert-middleware.agent-bundle.jar";
	private static final String JADE_OSGI_BUNDLE = "jade.jadeOsgi-1.0.0.jar";
	
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
		/* Here we have to perform the series of install and start operations for the  start the bundles 
		 * that implement the CONSERT CMM behaviour. In order these are:
		 *  - install everything
		 *  - start CONSERT Engine
		 *  - wait for CONSERT Engine initialization
		 *  - start CONSERT Middleware sensing resources
		 *  - start CONSERT Middleware Agents
		 */
		
		String bundleLocationBase = "file:./bundle";
		Bundle consertEngine = bundleContext.installBundle(bundleLocationBase + "/" + CONSERT_ENGINE_BUNDLE);
		
		bundleContext.installBundle(bundleLocationBase + "/" + JADE_OSGI_BUNDLE);
		Bundle consertCMMAgents = bundleContext.installBundle(bundleLocationBase + "/" + CONSERT_CMM_AGENT_BUNDLE);
		
		consertEngine.start();
		Thread.sleep(5000);
		
		consertCMMAgents.start();
		
		return null;
	}
	
	@Override
	public String getDescription() {
		return "Starts the CONSERT Context Management Middleware\n\t" + super.getDescription();
	}
}
