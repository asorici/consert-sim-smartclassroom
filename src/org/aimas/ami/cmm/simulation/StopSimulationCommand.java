package org.aimas.ami.cmm.simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.aimas.ami.contextrep.engine.api.PerformanceResult;
import org.aimas.ami.contextrep.engine.api.StatsHandler;
import org.aimas.ami.contextrep.resources.CMMConstants;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.liglab.adele.icasa.commands.AbstractCommand;
import fr.liglab.adele.icasa.commands.Signature;
import fr.liglab.adele.icasa.simulator.SimulationManager;

@Component(name="StopSimulationCommand")
@Provides
@Instantiate(name = "stop-simulation-command")
public class StopSimulationCommand extends AbstractCommand {
	private static final String COMMAND_NAME = "stop-simulation";
	private static final String SMART_CLASSROOM_APP = "SmartClassroom";
	
	private static final String TEST_DIR = "test" + File.separator + "performance";
	private static final String WITH_PROVISIONING_CONTROL = "with_prov_ctrl";
	private static final String WITHOUT_PROVISIONING_CONTROL = "without_prov_ctrl";
	private static final int UPDATE_RATE = 2; 
	
	@Requires
	private SimulationManager simulationManager;
	
	@Requires(
		filter="(" + CMMConstants.CONSERT_APPLICATION_ID_PROP + "=" + SMART_CLASSROOM_APP + ")"
	)
	private StatsHandler engineStatsAdaptor;
	
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
		
		out.println("#### Collecting performance results");
		PerformanceResult performanceResult = engineStatsAdaptor.measurePerformance();
		storePerformanceStatistics(performanceResult);
		
		return null;
    }
	
	private void storePerformanceStatistics(PerformanceResult performanceResult) {
		// get JSON output of run statistics
		Gson gsonOut = new GsonBuilder().setPrettyPrinting().create();
		String jsonOutput = gsonOut.toJson(performanceResult, PerformanceResult.class);
		
		// create test output directory if it does not exist 
		File globalTestDir = new File(TEST_DIR);
		if (!globalTestDir.exists()) {
			globalTestDir.mkdirs();
		}
		
		String outputFolderName = TEST_DIR + File.separator + WITH_PROVISIONING_CONTROL + "_" + UPDATE_RATE;
		//String outputFolderName = TEST_DIR + File.separator + WITHOUT_PROVISIONING_CONTROL + "_" + UPDATE_RATE;
		File outputFolder = new File(outputFolderName);
		if (!outputFolder.exists()) {
			outputFolder.mkdirs();
		}
		
		// json output filename is timestamp pretty printed
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		String jsonFileName = outputFolderName + File.separator + "test_" + sdf.format(now.getTime()) + ".json";
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonFileName), "utf-8"));
			writer.write(jsonOutput);
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		finally {
			try {
				if (writer != null) {
					writer.close();
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
