package org.aimas.ami.cmm.simulation.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.cmm.simulation.SensorStatsCollector.SensingEvent;
import org.aimas.ami.cmm.simulation.SensorStatsCollector.SensingMessageEvent;
import org.aimas.ami.cmm.simulation.SensorStatsCollector.SensorStats;
import org.aimas.ami.cmm.simulation.commands.StopSimulationCommand.SimulationPerformanceStats;
import org.aimas.ami.contextrep.engine.api.PerformanceResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;
import org.jfree.util.PaintUtilities;
import org.jfree.util.ShapeUtilities;

import com.google.gson.Gson;

public class ChartGenerator {
	private static final String TEST_DIR = "test" + File.separator + "performance";
	
	
	private static void createCharts() throws IOException {
		// list all folders in the TEST_DIR - each one corresponds to a different configuration setting
		File testsFolder = new File(TEST_DIR);
		File[] testConfigurations = testsFolder.listFiles();
		
		for (int i = 0; i < testConfigurations.length; i++) {
			File testConfigFolder = testConfigurations[i];
			if (testConfigFolder.isDirectory()) {
				// list all collected json result files
				File[] resultFiles = testConfigFolder.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.getPath().endsWith(".json"); 
					}
				});
				
				System.out.println("## Generating charts for config: " + testConfigFolder.getName());
				
				Map<String, SimulationPerformanceStats> collectedResults = getCollectedResults(resultFiles);
				//createGeneralCharts(collectedResults, testConfigFolder);
				createHistoryCharts(collectedResults, testConfigFolder);
			}
		}
	}
	
	
	private static Map<String, SimulationPerformanceStats> getCollectedResults(File[] resultFiles) {
		Map<String, SimulationPerformanceStats> collectedResults = new HashMap<>();
		for (int i = 0; i < resultFiles.length; i++) {
			File resultFile = resultFiles[i];
			String jsonContent = readFile(resultFile); 
			
			Gson gson = new Gson();
			SimulationPerformanceStats simulationPerformanceStats = 
				gson.fromJson(jsonContent, SimulationPerformanceStats.class);
			
			collectedResults.put(resultFile.getName(), simulationPerformanceStats);
		}
		
	    return collectedResults;
    }
	
	
	@SuppressWarnings("unused")
    private static PerformanceResult getCumulatedMeasure(Map<String, PerformanceResult> collectedResults) {
		List<PerformanceResult> resultMeasures = new LinkedList<>(collectedResults.values());
	    
		PerformanceResult cumulatedMeasure = new PerformanceResult();
	    
	    for (PerformanceResult pm : resultMeasures) {
	    	cumulatedMeasure.minInsertionDuration = 0;
	    	cumulatedMeasure.averageInsertionDuration = 0;
	    	cumulatedMeasure.maxInsertionDuration = 0;
	    	
	    	cumulatedMeasure.minInsertionDelay = 0;
	    	cumulatedMeasure.averageInsertionDelay = 0;
	    	cumulatedMeasure.maxInsertionDelay = 0;
	    	
	    	cumulatedMeasure.minInferenceDelay = 0;
	    	cumulatedMeasure.averageInferenceDelay = 0;
	    	cumulatedMeasure.maxInferenceDelay = 0;
	    	
	    	cumulatedMeasure.minInferenceCheckDuration = 0;
	    	cumulatedMeasure.averageInferenceCheckDuration = 0;
	    	cumulatedMeasure.maxInferenceCheckDuration = 0;
	    	
	    	cumulatedMeasure.minContinuityCheckDuration = 0;
	    	cumulatedMeasure.averageContinuityCheckDuration = 0;
	    	cumulatedMeasure.maxContinuityCheckDuration = 0;
	    	
	    	cumulatedMeasure.minConstraintCheckDuration = 0;
	    	cumulatedMeasure.averageConstraintCheckDuration = 0;
	    	cumulatedMeasure.maxConstraintCheckDuration = 0;
	    	
	    	cumulatedMeasure.minDeductionCycleDuration = 0;
	    	cumulatedMeasure.averageDeductionCycleDuration = 0;
	    	cumulatedMeasure.maxDeductionCycleDuration = 0;
	    }
	    
	    for (PerformanceResult pm : resultMeasures) {
	    	cumulatedMeasure.minInsertionDuration += pm.minInsertionDuration;
	    	cumulatedMeasure.averageInsertionDuration += pm.averageInsertionDuration;
	    	cumulatedMeasure.maxInsertionDuration += pm.maxInsertionDuration;
	    	
	    	cumulatedMeasure.minInsertionDelay += pm.minInsertionDelay;
	    	cumulatedMeasure.averageInsertionDelay += pm.averageInsertionDelay;
	    	cumulatedMeasure.maxInsertionDelay += pm.maxInsertionDelay;
	    	
	    	cumulatedMeasure.minInferenceDelay += pm.minInferenceDelay;
	    	cumulatedMeasure.averageInferenceDelay += pm.averageInferenceDelay;
	    	cumulatedMeasure.maxInferenceDelay += pm.maxInferenceDelay;
	    	
	    	cumulatedMeasure.minInferenceCheckDuration += pm.minInferenceCheckDuration;
	    	cumulatedMeasure.averageInferenceCheckDuration += pm.averageInferenceCheckDuration;
	    	cumulatedMeasure.maxInferenceCheckDuration += pm.maxInferenceCheckDuration;
	    	
	    	cumulatedMeasure.minContinuityCheckDuration += pm.minContinuityCheckDuration;
	    	cumulatedMeasure.averageContinuityCheckDuration += pm.averageContinuityCheckDuration;
	    	cumulatedMeasure.maxContinuityCheckDuration += pm.maxContinuityCheckDuration;
	    	
	    	cumulatedMeasure.minConstraintCheckDuration += pm.minConstraintCheckDuration;
	    	cumulatedMeasure.averageConstraintCheckDuration += pm.averageConstraintCheckDuration;
	    	cumulatedMeasure.maxConstraintCheckDuration += pm.maxConstraintCheckDuration;
	    	
	    	cumulatedMeasure.minDeductionCycleDuration += pm.minDeductionCycleDuration;
	    	cumulatedMeasure.averageDeductionCycleDuration += pm.averageDeductionCycleDuration;
	    	cumulatedMeasure.maxDeductionCycleDuration += pm.maxDeductionCycleDuration;
	    }
	    
	    int numResults = resultMeasures.size();
	    
	    cumulatedMeasure.minInsertionDuration /= numResults;
    	cumulatedMeasure.averageInsertionDuration /= numResults;
    	cumulatedMeasure.maxInsertionDuration /= numResults;
    	
    	cumulatedMeasure.minInsertionDelay /= numResults;
    	cumulatedMeasure.averageInsertionDelay /= numResults;
    	cumulatedMeasure.maxInsertionDelay /= numResults;
    	
    	cumulatedMeasure.minInferenceDelay /= numResults;
    	cumulatedMeasure.averageInferenceDelay /= numResults;
    	cumulatedMeasure.maxInferenceDelay /= numResults;
    	
    	cumulatedMeasure.minInferenceCheckDuration /= numResults;
    	cumulatedMeasure.averageInferenceCheckDuration /= numResults;
    	cumulatedMeasure.maxInferenceCheckDuration /= numResults;
    	
    	cumulatedMeasure.minContinuityCheckDuration /= numResults;
    	cumulatedMeasure.averageContinuityCheckDuration /= numResults;
    	cumulatedMeasure.maxContinuityCheckDuration /= numResults;
    	
    	cumulatedMeasure.minConstraintCheckDuration /= numResults;
    	cumulatedMeasure.averageConstraintCheckDuration /= numResults;
    	cumulatedMeasure.maxConstraintCheckDuration /= numResults;
    	
    	cumulatedMeasure.minDeductionCycleDuration /= numResults;
    	cumulatedMeasure.averageDeductionCycleDuration /= numResults;
    	cumulatedMeasure.maxDeductionCycleDuration /= numResults;
    	
    	return cumulatedMeasure;
	}
	
	
	private static void createHistoryCharts(Map<String, SimulationPerformanceStats> collectedResults, File configFolder) throws IOException {
	    int skipRate = 12;
		int phase = 0;
	    
		for (String resultName : collectedResults.keySet()) {
			SimulationPerformanceStats simulationResults = collectedResults.get(resultName);
			
			PerformanceResult performanceResult = simulationResults.getEnginePerformanceResult();
	    	SensorStats sensingResult = simulationResults.getSeningPerformanceResult();
			
	    	XYSeriesCollection insertionDatasetCollection = new XYSeriesCollection();
	    	XYSeriesCollection inferenceDatasetCollection = new XYSeriesCollection();
	    	XYBarDataset inferenceBarDataset = new XYBarDataset(inferenceDatasetCollection, 16);
	    	//CategoryDataset infereceBarDataset = new DefaultCategoryDataset();
	    	
	    	XYSeriesCollection deductionCycleDatasetCollection = new XYSeriesCollection();
	    	XYBarDataset deductionBarDataset = new XYBarDataset(deductionCycleDatasetCollection, 8);
	    	
	    	// ============ create the insertion series ============
	    	XYSeries insertionDelaySeries = new XYSeries("Insert Delay", true);
	    	XYSeries insertionDurationSeries = new XYSeries("Insert Processing", true);
	    	
	    	List<Integer> sortedInsertionIDs = new ArrayList<Integer>(performanceResult.insertionDelayHistory.keySet());  
	    	Collections.sort(sortedInsertionIDs);
	    	
	    	for (int insertionID : sortedInsertionIDs) {
	    		if ((insertionID + phase) % skipRate == 0) {
	    			int insertionDelayValue = performanceResult.insertionDelayHistory.get(insertionID).intValue();
	    			int insertionDurationValue = performanceResult.insertionDurationHistory.get(insertionID).intValue();
    		
	    			insertionDelaySeries.add(insertionID, insertionDelayValue);
	    			insertionDurationSeries.add(insertionID, insertionDurationValue);
	    		}
    		}
	    	
	    	insertionDatasetCollection.addSeries(insertionDelaySeries);
	    	insertionDatasetCollection.addSeries(insertionDurationSeries);
	    	
	    	// ============ create the inference series ============
	    	XYSeries inferenceDelaySeries = new XYSeries("Inference Delay", true);
	    	XYSeries inferenceDurationSeries = new XYSeries("Inference Processing", true);
	    	
	    	
	    	for (Integer insertionID : performanceResult.insertionDelayHistory.keySet()) {
	    		Long inferenceDelayValue = performanceResult.inferenceDelayHistory.get(insertionID);
	    		Long inferenceDurationValue = performanceResult.inferenceDurationHistory.get(insertionID);
	    		
	    		if (inferenceDelayValue != null) {
	    			inferenceDelaySeries.add(insertionID, inferenceDelayValue);
	    		}
	    		
	    		if (inferenceDurationValue != null) {
	    			inferenceDurationSeries.add(insertionID, inferenceDurationValue);
	    		}
	    	}
	    	
	    	inferenceDatasetCollection.addSeries(inferenceDelaySeries);
	    	inferenceDatasetCollection.addSeries(inferenceDurationSeries);
	    	
	    	// ============ create the deduction series ============
	    	XYSeries deductionSeries = new XYSeries("Deduction Duration", true);
	    	for (Integer insertionID : performanceResult.insertionDelayHistory.keySet()) {
	    		Long deductionDurationValue = performanceResult.deductionCycleHistory.get(insertionID);
	    		if (deductionDurationValue != null) {
	    			deductionSeries.add(insertionID, deductionDurationValue);
	    		}
	    	}
	    	
	    	deductionCycleDatasetCollection.addSeries(deductionSeries);
	    	
	    	// ============ create the sensing event series ============
	    	List<SensingEvent> sensingEventHistory = sensingResult.getSensingEventHistory();
	    	List<SensingMessageEvent> sensingMessageHistory = sensingResult.getSensingMessageEventHistory();
	    	Collections.sort(sensingEventHistory);
	    	Collections.sort(sensingMessageHistory);
	    	
	    	long sensingEventStart = sensingEventHistory.get(0).getTimestamp();
	    	long sensingEventStop = sensingEventHistory.get(sensingEventHistory.size() - 1).getTimestamp();
	    	
	    	int firstTempIndex = -1;
	    	int lastTempIndex = -1;
	    	
	    	int firstNoiseLevelIndex = -1;
	    	int lastNoiseLevelIndex = -1;
	    	
	    	// create event count bins every 5 seconds
	    	XYSeries sensingEventSeries = new XYSeries("Sensing Events", true);
	    	XYSeries sensingMessageSeries = new XYSeries("Sensing Message Updates", true);
	    	
	    	int eventIndex = 0;
	    	int messageIndex = 0;
	    	
	    	int totalSensingEvents = 0;
	    	int totalSensingMessages = 0;
	    	
	    	int step = 10000;
	    	
	    	Set<String> sensingTypes = new HashSet<String>();
	    	
	    	for (long ts = sensingEventStart; ts < sensingEventStop; ts += step) {
	    		long tsNext = ts + step;
	    		int eventCt = 0;
		    	int messageCt = 0;
		    	
		    	sensingTypes.clear();
		    	
		    	while(eventIndex < sensingEventHistory.size() && 
		    			sensingEventHistory.get(eventIndex).getTimestamp() < tsNext) {
		    		
		    		sensingTypes.add(sensingEventHistory.get(eventIndex).getSensedAssertionType());
		    		
		    		eventCt += 1;
		    		eventIndex++;
		    	}
		    	
		    	while(messageIndex < sensingMessageHistory.size() && 
		    			sensingMessageHistory.get(messageIndex).getTimestamp() < tsNext) {
		    		messageCt += 1;
		    		messageIndex++;
		    	}
		    	
		    	sensingEventSeries.add((tsNext - sensingEventStart) / 1000, eventCt);
		    	sensingMessageSeries.add((tsNext - sensingEventStart) / 1000, messageCt);
		    	
		    	totalSensingEvents += eventCt;
		    	totalSensingMessages += messageCt;
		    	
		    	if (firstTempIndex < 0) {
		    		if (sensingTypes.contains("sensesTemperature")) {
		    			firstTempIndex = (int)((tsNext - sensingEventStart) / 1000);
		    		}
		    	}
		    	else if (lastTempIndex < 0) {
		    		if (!sensingTypes.contains("sensesTemperature")) {
		    			lastTempIndex = (int)((tsNext - sensingEventStart) / 1000);
		    		}
		    	}
		    	
		    	if (firstNoiseLevelIndex < 0) {
		    		if (sensingTypes.contains("hasNoiseLevel")) {
		    			firstNoiseLevelIndex = (int)((tsNext - sensingEventStart) / 1000);
		    		}
		    	}
		    	else if (lastNoiseLevelIndex < 0) {
		    		if (!sensingTypes.contains("hasNoiseLevel")) {
		    			lastNoiseLevelIndex = (int)((tsNext - sensingEventStart) / 1000);
		    		}
		    	}
	    	}
	    	
	    	sensingEventSeries.setKey("Sensing Events " + "(" + totalSensingEvents + ")");
	    	sensingMessageSeries.setKey("Sensing Message Updates " + "(" + totalSensingMessages + ")");
	    	
	    	XYSeriesCollection sensingEventCollection = new XYSeriesCollection();
	    	sensingEventCollection.addSeries(sensingEventSeries);
	    	sensingEventCollection.addSeries(sensingMessageSeries);
	    	
	    	// =================== Create charts ====================
	    	JFreeChart historyChart = setupEngineHistoryChart(performanceResult, insertionDatasetCollection, inferenceBarDataset, deductionBarDataset);
	    	JFreeChart sensingHistoryChart = setupSensingHistoryChart(sensingResult, sensingEventCollection,
	    			totalSensingEvents, totalSensingMessages, 
	    			firstTempIndex, lastTempIndex, firstNoiseLevelIndex, lastNoiseLevelIndex);
	    	
	    	File historyGraph = new File(configFolder.getAbsolutePath() + File.separator + resultName + "_engine_graph.png"); 
	    	File sensingHistoryGraph = new File(configFolder.getAbsolutePath() + File.separator + resultName + "_sensing_graph.png");
	    	ChartUtilities.saveChartAsPNG(historyGraph, historyChart, 800, 600);
	    	ChartUtilities.saveChartAsPNG(sensingHistoryGraph, sensingHistoryChart, 800, 600);
	    }
	}	


	@SuppressWarnings("unused")
    private static JFreeChart setupGeneralChart(String title, String domainAxisLabel, String rangeAxisLabel, 
			DefaultCategoryDataset dataset) {
		JFreeChart chart = ChartFactory.createBarChart(
		        title, domainAxisLabel, rangeAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		
		chart.setBackgroundPaint(Color.white);
		BarRenderer br = (BarRenderer) chart.getCategoryPlot().getRenderer();
		br.setItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		br.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.HALF_ASCENT_CENTER));
		br.setItemLabelsVisible(true);
		chart.getCategoryPlot().setRenderer(br);
		
		return chart;
	}
	
	
	private static JFreeChart setupSensingHistoryChart(SensorStats sensingResult, 
			XYSeriesCollection sensingEventCollection, int totalSensingEvents, int totalSensingMessages, 
			int firstTempIndex, int lastTempIndex, int firstNoiseLevelIndex, int lastNoiseLevelIndex) {
		
		JFreeChart chart = ChartFactory.createXYLineChart(
	            "Simulated Sensing Event Log", 
	            "Time (seconds)", 
	            "Nr. Events", 
	            sensingEventCollection
	        );
		
		XYPlot plot = chart.getXYPlot();
		ValueMarker tempStartMarker = new ValueMarker(firstTempIndex, Color.BLACK, new BasicStroke(2));
		ValueMarker tempStopMarker = new ValueMarker(lastTempIndex, Color.BLACK, new BasicStroke(2));
		
		ValueMarker noiseStartMarker = new ValueMarker(firstNoiseLevelIndex, Color.YELLOW, new BasicStroke(2));
		ValueMarker noiseStopMarker = new ValueMarker(lastNoiseLevelIndex, Color.YELLOW, new BasicStroke(2));
		
		plot.addDomainMarker(tempStartMarker, Layer.BACKGROUND);
		plot.addDomainMarker(tempStopMarker, Layer.BACKGROUND);
		plot.addDomainMarker(noiseStartMarker, Layer.BACKGROUND);
		plot.addDomainMarker(noiseStopMarker, Layer.BACKGROUND);
		
		//plot.addAnnotation(new XYTextAnnotation("Total sensing events: " + totalSensingEvents, 400, 300));
		//plot.addAnnotation(new XYTextAnnotation("Total sensing messages: " + totalSensingMessages, 400, 350));
		
		return chart;
    }
	
	private static JFreeChart setupEngineHistoryChart(PerformanceResult performanceResult,
			XYDataset insertionDataset, XYDataset inferenceDataset, XYDataset deductionDataset) {
		
		NumberAxis insertionRangeAxis = new NumberAxis("ms");
		insertionRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		insertionRangeAxis.setUpperBound(300);
		
		XYItemRenderer insertionRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES);
		XYPlot insertionPlot = new XYPlot(insertionDataset, null, insertionRangeAxis, insertionRenderer);
		insertionPlot.setDomainGridlinesVisible(true);
		insertionRenderer.setSeriesStroke(0, new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, 
				new float[] {4, 4}, 0));
		insertionRenderer.setSeriesStroke(1, new BasicStroke(1));
		insertionRenderer.setSeriesShape(0, ShapeUtilities.createRegularCross(2, 2));
		insertionRenderer.setSeriesShape(1, ShapeUtilities.createUpTriangle(4));
		
		Marker avgInsertDelayMark = new ValueMarker(performanceResult.averageInsertionDelay);
        avgInsertDelayMark.setPaint(Color.green);
        avgInsertDelayMark.setStroke(new BasicStroke(2.0f));
        avgInsertDelayMark.setLabel("Average Insertion Delay");
        avgInsertDelayMark.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        avgInsertDelayMark.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
        insertionPlot.addRangeMarker(avgInsertDelayMark);
		
		NumberAxis inferenceRangeAxis = new NumberAxis("ms");
		inferenceRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		ClusteredXYBarRenderer inferenceRenderer = new ClusteredXYBarRenderer();
		inferenceRenderer.setShadowVisible(false);
		inferenceRenderer.setSeriesPaint(0, PaintUtilities.stringToColor("orange"));
		inferenceRenderer.setSeriesPaint(1, PaintUtilities.stringToColor("black"));
		
		//inferenceRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        XYPlot inferencePlot = new XYPlot(inferenceDataset, null, inferenceRangeAxis, inferenceRenderer);
        inferencePlot.setDomainGridlinesVisible(true);
        
        Marker avgInferenceDelayMark = new ValueMarker(performanceResult.averageInferenceDelay);
        avgInferenceDelayMark.setPaint(Color.green);
        avgInferenceDelayMark.setStroke(new BasicStroke(2.0f));
        avgInferenceDelayMark.setLabel("Average Inference Delay");
        avgInferenceDelayMark.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        avgInferenceDelayMark.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
        inferencePlot.addRangeMarker(avgInferenceDelayMark);
		
        
        NumberAxis deductionRangeAxis = new NumberAxis("ms");
		deductionRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYBarRenderer deductionRenderer = new XYBarRenderer();
		deductionRenderer.setSeriesShape(0, ShapeUtilities.createDiamond(2));
		deductionRenderer.setShadowVisible(false);
		deductionRenderer.setSeriesPaint(0, PaintUtilities.stringToColor("magenta"));
		//deductionRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        XYPlot deductionPlot = new XYPlot(deductionDataset, null, deductionRangeAxis, deductionRenderer);
        deductionPlot.setDomainGridlinesVisible(true);
        
        Marker avgDeductionDurationMark = new ValueMarker(performanceResult.averageDeductionCycleDuration);
        avgDeductionDurationMark.setPaint(Color.green);
        avgDeductionDurationMark.setStroke(new BasicStroke(2.0f));
        avgDeductionDurationMark.setLabel("Average Deduction Duration");
        avgDeductionDurationMark.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        avgDeductionDurationMark.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
        deductionPlot.addRangeMarker(avgDeductionDurationMark);
        
        NumberAxis domainAxis = new NumberAxis("Insert Event");
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
        plot.add(insertionPlot, 1);
        //plot.add(inferencePlot, 2);
        //plot.add(deductionPlot, 1);
        
        JFreeChart result = new JFreeChart(
        		"ContextAssertion Runtime Statistics",
        		new Font("SansSerif", Font.BOLD, 12), plot, true);
        
        // result.getLegend().setAnchor(Legend.SOUTH);
        return result;
	}
	
	
	private static String readFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes,"UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
	}
	
	@SuppressWarnings("unused")
    private static void writeFile(File file, String content) {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
			        new FileOutputStream(file), "utf-8"));
			writer.write(content);
		}
		catch (IOException ex) {
		}
		finally {
			try {
				writer.close();
			}
			catch (Exception ex) {
			}
		}
	}
	
	
	@SuppressWarnings("unused")
    private static class MinMaxAverageDisplay implements Comparable<MinMaxAverageDisplay> {
		public static final String[] displayOrder = {"min", "average", "max"}; 
		
		int displayIndex = 0;
		
		private MinMaxAverageDisplay(int displayIndex) {
			this.displayIndex = displayIndex;
        }
		
		static MinMaxAverageDisplay min() {
			return new MinMaxAverageDisplay(0);
		}
		
		static MinMaxAverageDisplay average() {
			return new MinMaxAverageDisplay(1);
		}
		
		static MinMaxAverageDisplay max() {
			return new MinMaxAverageDisplay(2);
		}
		
		@Override
        public int compareTo(MinMaxAverageDisplay o) {
	        return displayIndex - o.displayIndex;
        }
		
		@Override
		public String toString() {
			return displayOrder[displayIndex];
		}
	}
	
	
	public static void main(String[] args) {
		try {
	        createCharts();
        }
        catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}
}
