package org.aimas.ami.cmm.simulation.commands;

import java.io.InputStream;
import java.io.PrintStream;

import org.aimas.ami.cmm.simulation.SimulationUsers;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;
import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(name = "StopUserCommand")
@Provides
@Instantiate(name = "stop-user-command")
public class StopUserCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "stop-user";
	
	@Requires
	private SimulationManager simulationManager;
	
	public StopUserCommand() {
		addSignature(new Signature(new String[] {"userName"})); // Adding a signature, with one parameter: userName
	}
	
	@Override
    public String getName() {
		return COMMAND_NAME;
    }

	@Override
    public Object execute(InputStream in, PrintStream out, JSONObject parameters, Signature signature) throws Exception {
		String userName = parameters.getString("userName");
		ComponentInstance instance = SimulationUsers.userComponentInstanceMap.get(userName);
		
		if (instance != null) {
			if ("AliceUser BobUser CecilleUser".contains(userName)) {
				simulationManager.removePerson(userName);
			}
			
			instance.stop();
			instance.dispose();
		}
		else {
			System.out.println("["+getClass().getSimpleName()+"] Could not find component instance for user: " + userName);
		}
		
	    return null;
    }
}
