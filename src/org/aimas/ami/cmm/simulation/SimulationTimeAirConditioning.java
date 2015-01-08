package org.aimas.ami.cmm.simulation;

import java.util.Calendar;
import java.util.TimeZone;

import org.aimas.ami.contextrep.resources.CMMConstants;
import org.aimas.ami.contextrep.resources.TimeService;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import fr.liglab.adele.icasa.clock.Clock;

@Component(name="SimulationTime-AirConditioning", immediate=false, publicFactory = false)
@Provides(properties = {
	@StaticServiceProperty(name="service.ranking", type="java.lang.Integer", value="10"),
	@StaticServiceProperty(name=CMMConstants.CONSERT_APPLICATION_ID_PROP, type="java.lang.String", value="AirConditioning"),
})
@Instantiate
public class SimulationTimeAirConditioning implements TimeService {
	
	@Requires
	private Clock simulationClock;
	
	@Override
    public Calendar getCalendarInstance() {
	    long simulationMillis = simulationClock.currentTimeMillis();
	    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    cal.setTimeInMillis(simulationMillis);
	    
	    //SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		//formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	    //System.out.println("["+ getClass().getName() + "] Invocation yields datetime: " + formatter.format(cal.getTime()));
	    
	    return cal;
    }

	@Override
    public long getCurrentTimeMillis() {
	    return simulationClock.currentTimeMillis();
    }
	
}
