package org.aimas.ami.cmm.simulation.sensors;

import java.util.LinkedList;
import java.util.List;

import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;

public class SensingUtil {
	private static long counter = 1;
	
	public static final String CHANGE_BASED_UPDATE = "change-based";
	public static final String TIME_BASED_UPDATE = "time-based";
	
	private static List<String> standardAnnotations = new LinkedList<String>();
	static {
		standardAnnotations.add(ConsertAnnotation.SOURCE_ANNOTATION.getURI());
		standardAnnotations.add(ConsertAnnotation.NUMERIC_VALUE_CERTAINTY.getURI());
		standardAnnotations.add(ConsertAnnotation.TEMPORAL_VALIDITY.getURI());
		standardAnnotations.add(ConsertAnnotation.DATETIME_TIMESTAMP.getURI());
	}
	
	public static String generateUniqueURI(String resourceBaseURI) {
		return resourceBaseURI + "-" + (counter++);
	}
	
	public static List<String> getStandardAnnotations() {
		return standardAnnotations;
	}
}
