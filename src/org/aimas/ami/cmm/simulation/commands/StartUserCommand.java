package org.aimas.ami.cmm.simulation.commands;

import java.io.InputStream;
import java.io.PrintStream;

import org.aimas.ami.cmm.simulation.SimulationUsers;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.json.JSONObject;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;
import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(name = "StartUserCommand")
@Provides
@Instantiate(name = "start-user-command")
public class StartUserCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "start-user";
	
	@Requires
	private SimulationManager simulationManager;
	
	
	@Bind(id="aliceUserFactory", filter="(factory.name=org.aimas.ami.cmm.simulation.users.AliceUser)")
	private void bindAliceLabUserFactory(Factory aliceUserFactory) {
		SimulationUsers.userComponentFactoryMap.put(SimulationUsers.ALICE_NAME, aliceUserFactory);
	}
	
	@Unbind(id="aliceUserFactory")
	private void unbindAliceLabUserFactory(Factory aliceUserFactory) {
		SimulationUsers.userComponentFactoryMap.remove(SimulationUsers.ALICE_NAME);
	}
	
	@Bind(id="bobUserFactory", filter="(factory.name=org.aimas.ami.cmm.simulation.users.BobUser)")
	private void bindBobUserFactory(Factory bobUserFactory) {
		SimulationUsers.userComponentFactoryMap.put(SimulationUsers.BOB_NAME, bobUserFactory);
	}
	
	@Unbind(id="bobUserFactory")
	private void unbindBobUserFactory(Factory bobUserFactory) {
		SimulationUsers.userComponentFactoryMap.remove(SimulationUsers.BOB_NAME);
	}
	
	
	
	@Bind(id="cecilleUserFactory", filter="(factory.name=org.aimas.ami.cmm.simulation.users.CecilleUser)")
	private void bindCecilleUserFactory(Factory cecilleUserFactory) {
		SimulationUsers.userComponentFactoryMap.put(SimulationUsers.CECILLE_NAME, cecilleUserFactory);
	}
	
	@Unbind(id="cecilleUserFactory")
	private void unbindCecilleUserFactory(Factory cecilleUserFactory) {
		SimulationUsers.userComponentFactoryMap.remove(SimulationUsers.CECILLE_NAME);
	}
	
	
	
	@Bind(id="danUserFactory", filter="(factory.name=org.aimas.ami.cmm.simulation.users.DanUser)")
	private void bindDanUserFactory(Factory danUserFactory) {
		SimulationUsers.userComponentFactoryMap.put(SimulationUsers.DAN_NAME, danUserFactory);
	}
	
	@Unbind(id="danUserFactory")
	private void unbindDanUserFactory(Factory danUserFactory) {
		SimulationUsers.userComponentFactoryMap.remove(SimulationUsers.DAN_NAME);
	}
	
	
	
	@Bind(id="airConditioningUserFactory", filter="(factory.name=org.aimas.ami.cmm.simulation.users.AirConditioningUser)")
	private void bindAirConditioningUserFactory(Factory airConditioningUserFactory) {
		SimulationUsers.userComponentFactoryMap.put(SimulationUsers.AIRCONDITIONING_NAME, airConditioningUserFactory);
	}
	
	@Unbind(id="airConditioningUserFactory")
	private void unbindAirConditioningUserFactory(Factory airConditioningUserFactory) {
		SimulationUsers.userComponentFactoryMap.remove(SimulationUsers.AIRCONDITIONING_NAME);
	}
	
	
	
	@Bind(id="projectorUserFactory", filter="(factory.name=org.aimas.ami.cmm.simulation.users.ProjectorUser)")
	private void bindProjectorUserFactory(Factory projectorUserFactory) {
		SimulationUsers.userComponentFactoryMap.put(SimulationUsers.PROJECTOR_NAME, projectorUserFactory);
	}
	
	@Unbind(id="projectorUserFactory")
	private void unbindProjectorUserFactory(Factory projectorUserFactory) {
		SimulationUsers.userComponentFactoryMap.remove(SimulationUsers.PROJECTOR_NAME);
	}
	
	
	
	public StartUserCommand() {
		addSignature(new Signature(new String[] {"userName"})); // Adding a signature, with one parameter: userName
	}
	
	@Override
    public String getName() {
		return COMMAND_NAME;
    }

	@Override
    public Object execute(InputStream in, PrintStream out, JSONObject parameters, Signature signature) throws Exception {
		String userName = parameters.getString("userName");
		Factory factory = SimulationUsers.userComponentFactoryMap.get(userName);
		
		if (factory != null) {
			ComponentInstance userInstance = factory.createComponentInstance(null);
			userInstance.start();
			SimulationUsers.userComponentInstanceMap.put(userName, userInstance);
		}
		else {
			System.out.println("["+getClass().getSimpleName()+"] Could not find component FACTORY for user: " + userName + "!!!!");
		}
		
	    return null;
    }
}
