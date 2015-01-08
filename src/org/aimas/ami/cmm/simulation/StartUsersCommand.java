package org.aimas.ami.cmm.simulation;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;

@Component(name="StartUsersCommand")
@Provides
@Instantiate(name = "start-users-command")
public class StartUsersCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "start-users";
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.users.AliceUser)")
	private Factory aliceUserFactory;
	
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.users.BobUser)")
	private Factory bobUserFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.users.CecilleUser)")
	private Factory cecilleUserFactory;
	
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.users.AirConditioningUser)")
	private Factory airConditioningUserFactory;
	
	@Requires(filter="(factory.name=org.aimas.ami.cmm.simulation.users.ProjectorUser)")
	private Factory projectorUserFactory;
	
	
	public StartUsersCommand() {
		addSignature(new Signature(new String[0])); // Adding an empty signature, without parameters
	}
	
	@Override
    public String getName() {
		return COMMAND_NAME;
    }

	@Override
    public Object execute(InputStream arg0, PrintStream arg1, JSONObject arg2, Signature arg3) throws Exception {
		ComponentInstance aliceUserInstance = aliceUserFactory.createComponentInstance(null);
		aliceUserInstance.start();
		SimulationUsers.userComponentInstanceMap.put(SimulationUsers.ALICE_NAME, aliceUserInstance);
		
		
		ComponentInstance bobUserInstance = bobUserFactory.createComponentInstance(null);
		bobUserInstance.start();
		SimulationUsers.userComponentInstanceMap.put(SimulationUsers.BOB_NAME, bobUserInstance);
		
		ComponentInstance cecilleUserInstance = cecilleUserFactory.createComponentInstance(null);
		cecilleUserInstance.start();
		SimulationUsers.userComponentInstanceMap.put(SimulationUsers.CECILLE_NAME, cecilleUserInstance);
		
		
		
		ComponentInstance airConditioningUserInstance = airConditioningUserFactory.createComponentInstance(null);
		airConditioningUserInstance.start();
		SimulationUsers.userComponentInstanceMap.put(SimulationUsers.AIRCONDITIONING_NAME, airConditioningUserInstance);
		
		ComponentInstance projectorUserInstance = projectorUserFactory.createComponentInstance(null);
		projectorUserInstance.start();
		SimulationUsers.userComponentInstanceMap.put(SimulationUsers.PROJECTOR_NAME, projectorUserInstance);
		
		
	    return null;
    }
	
}
