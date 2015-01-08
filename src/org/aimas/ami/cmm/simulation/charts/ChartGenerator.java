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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.engine.api.PerformanceResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
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
				
				Map<String, PerformanceResult> collectedResults = getCollectedResults(resultFiles);
				//createGeneralCharts(collectedResults, testConfigFolder);
				createHistoryCharts(collectedResults, testConfigFolder);
			}
		}
	}
	

	private static Map<String, PerformanceResult> getCollectedResults(File[] resultFiles) {
		Map<String, PerformanceResult> collectedResults = new HashMap<>();
		for (int i = 0; i < resultFiles.length; i++) {
			File resultFile = resultFiles[i];
			String jsonContent = readFile(resultFile); 
			
			Gson gson = new Gson();
			PerformanceResult performanceResult = gson.fromJson(jsonContent, PerformanceResult.class);
			
			collectedResults.put(resultFile.getName(), performanceResult);
		}
		
	    return collectedResults;
    }
	
	
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
	
	
	private static void createHistoryCharts(Map<String, PerformanceResult> collectedResults, File configFolder) throws IOException {
	    int skipRate = 20;
		int phase = 0;
	    
		for (String resultName : collectedResults.keySet()) {
	    	PerformanceResult performanceResult = collectedResults.get(resultName);
	    	
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
	    	
	    	JFreeChart historyChart = setupHistoryChart(insertionDatasetCollection, inferenceBarDataset, deductionBarDataset);
	    	File historyGraph = new File(configFolder.getAbsolutePath() + File.separator + resultName + "_graph.png"); 
		    ChartUtilities.saveChartAsPNG(historyGraph, historyChart, 800, 600);
	    }
    }
	
	
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
	
	
	private static JFreeChart setupHistoryChart(
			XYDataset insertionDataset, XYDataset inferenceDataset, XYDataset deductionDataset) {
		
		NumberAxis insertionRangeAxis = new NumberAxis("ms");
		insertionRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYItemRenderer insertionRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES);
		XYPlot insertionPlot = new XYPlot(insertionDataset, null, insertionRangeAxis, insertionRenderer);
		insertionPlot.setDomainGridlinesVisible(true);
		insertionRenderer.setSeriesStroke(0, new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, 
				new float[] {4, 4}, 0));
		insertionRenderer.setSeriesStroke(1, new BasicStroke(1));
		insertionRenderer.setSeriesShape(0, ShapeUtilities.createRegularCross(2, 2));
		insertionRenderer.setSeriesShape(1, ShapeUtilities.createUpTriangle(4));
		
		NumberAxis inferenceRangeAxis = new NumberAxis("ms");
		inferenceRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		ClusteredXYBarRenderer inferenceRenderer = new ClusteredXYBarRenderer();
		inferenceRenderer.setShadowVisible(false);
		inferenceRenderer.setSeriesPaint(0, PaintUtilities.stringToColor("orange"));
		inferenceRenderer.setSeriesPaint(1, PaintUtilities.stringToColor("black"));
		
		//inferenceRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        XYPlot inferencePlot = new XYPlot(inferenceDataset, null, inferenceRangeAxis, inferenceRenderer);
        inferencePlot.setDomainGridlinesVisible(true);
        
        NumberAxis deductionRangeAxis = new NumberAxis("ms");
		deductionRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYBarRenderer deductionRenderer = new XYBarRenderer();
		deductionRenderer.setSeriesShape(0, ShapeUtilities.createDiamond(2));
		deductionRenderer.setShadowVisible(false);
		deductionRenderer.setSeriesPaint(0, PaintUtilities.stringToColor("magenta"));
		//deductionRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        XYPlot deductionPlot = new XYPlot(deductionDataset, null, deductionRangeAxis, deductionRenderer);
        deductionPlot.setDomainGridlinesVisible(true);
        
        NumberAxis domainAxis = new NumberAxis("Insert Event");
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
        plot.add(insertionPlot, 3);
        plot.add(inferencePlot, 2);
        plot.add(deductionPlot, 1);
        
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
