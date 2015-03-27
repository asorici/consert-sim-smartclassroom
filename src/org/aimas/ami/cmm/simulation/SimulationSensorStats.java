package org.aimas.ami.cmm.simulation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component(name="SimulationSensorStats")
@Provides
@Instantiate(name = "collect-sensing-stats")
public class SimulationSensorStats implements SensorStatsCollector {
	
	List<SensingEvent> sensingEventHistory = new LinkedList<SensingEvent>();
	List<SensingMessageEvent> sensingMessageEventHistory = new LinkedList<SensingMessageEvent>();
	
	
	@Override
    public void markSensing(long timestamp, String sensedAssertionType) {
	    sensingEventHistory.add(new SensingEvent(timestamp, sensedAssertionType));
    }

	@Override
    public void markSensingUpdateMessage(long timestamp, int messageId, String sensedAssertionType) {
	    sensingMessageEventHistory.add(new SensingMessageEvent(timestamp, sensedAssertionType, messageId));
    }

	@Override
    public SensorStats collectSensingStats() {
	    Collections.sort(sensingEventHistory);
	    Collections.sort(sensingMessageEventHistory);
	    
	    return new SensorStats(sensingEventHistory, sensingMessageEventHistory);
    }
}
