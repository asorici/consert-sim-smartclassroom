package org.aimas.ami.cmm.simulation.commands;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(name = "RemoveZoneVariableCommand")
@Provides
@Instantiate(name = "remove-zone-variable-command")
public class RemoveZoneVariableCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "remove-zone-variable";
	
	@Requires
	private SimulationManager simulationManager;
	
	public RemoveZoneVariableCommand() {
		addSignature(new Signature(new String[] {"zoneId", "variable"})); // Adding a signature, with two parameters: zoneId, variable
	}
	
	@Override
    public String getName() {
		return COMMAND_NAME;
    }

	@Override
    public Object execute(InputStream in, PrintStream out, JSONObject parameters, Signature signature) throws Exception {
		String zoneId = parameters.getString("zoneId");
		String variableName = parameters.getString("variable");
		
		Zone zone = simulationManager.getZone(zoneId);
		if (zone != null) {
			zone.removeVariable(variableName);
		}
		
	    return null;
    }
}
