package org.aimas.ami.cmm.simulation;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;
import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(name="StopSimulationCommand")
@Provides
@Instantiate(name = "stop-simulation-command")
public class StopSimulationCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "stop-simulation";
	
	@Requires
	private SimulationManager simulationManager;
	
	public StopSimulationCommand() {
		addSignature(new Signature(new String[0])); // Adding an empty signature, without parameters
	}
	
	@Override
    public String getName() {
		return COMMAND_NAME;
    }

	@Override
    public Object execute(InputStream in, PrintStream out, JSONObject parameters, Signature signature) throws Exception {
		out.println("###################### SIMULATION END ######################");
		return null;
    }
	
}
