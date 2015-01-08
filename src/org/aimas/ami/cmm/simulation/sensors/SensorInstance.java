package org.aimas.ami.cmm.simulation.sensors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.utils.ContextModelUtils;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;

public class SensorInstance {
	private static Map<Resource, Resource> sensorLocationMap;
	static {
		sensorLocationMap = new HashMap<Resource, Resource>();
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_PresenterArea, SmartClassroom.EF210_PresenterArea);
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_Section1_Left, SmartClassroom.EF210_Section1_Left);
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_Section1_Right, SmartClassroom.EF210_Section1_Right);
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_Section2_Left, SmartClassroom.EF210_Section2_Left);
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_Section2_Right, SmartClassroom.EF210_Section2_Right);
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_Section3_Left, SmartClassroom.EF210_Section3_Left);
		sensorLocationMap.put(SmartClassroom.Kinect_EF210_Section3_Right, SmartClassroom.EF210_Section3_Right);
		
		sensorLocationMap.put(SmartClassroom.Mic_EF210_PresenterArea, SmartClassroom.EF210_PresenterArea);
		sensorLocationMap.put(SmartClassroom.Mic_EF210_Section1_Left, SmartClassroom.EF210_Section1_Left);
		sensorLocationMap.put(SmartClassroom.Mic_EF210_Section1_Right, SmartClassroom.EF210_Section1_Right);
		sensorLocationMap.put(SmartClassroom.Mic_EF210_Section2_Left, SmartClassroom.EF210_Section2_Left);
		sensorLocationMap.put(SmartClassroom.Mic_EF210_Section2_Right, SmartClassroom.EF210_Section2_Right);
		sensorLocationMap.put(SmartClassroom.Mic_EF210_Section3_Left, SmartClassroom.EF210_Section3_Left);
		sensorLocationMap.put(SmartClassroom.Mic_EF210_Section3_Right, SmartClassroom.EF210_Section3_Right);
		
		sensorLocationMap.put(SmartClassroom.Lum_EF210_PresenterArea, SmartClassroom.EF210_PresenterArea);
		sensorLocationMap.put(SmartClassroom.Lum_EF210_Section1_Right, SmartClassroom.EF210_Section1_Right);
		sensorLocationMap.put(SmartClassroom.Lum_EF210_Section2_Right, SmartClassroom.EF210_Section2_Right);
		sensorLocationMap.put(SmartClassroom.Lum_EF210_Section3_Right, SmartClassroom.EF210_Section3_Right);
		
		sensorLocationMap.put(SmartClassroom.Temp_EF210_Section1_Left, SmartClassroom.EF210_Section1_Left);
		sensorLocationMap.put(SmartClassroom.Temp_EF210_Section1_Right, SmartClassroom.EF210_Section1_Right);
		sensorLocationMap.put(SmartClassroom.Temp_EF210_Section3_Left, SmartClassroom.EF210_Section3_Left);
		sensorLocationMap.put(SmartClassroom.Temp_EF210_Section3_Right, SmartClassroom.EF210_Section3_Right);
		
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_PresenterArea, SmartClassroom.EF210_PresenterArea);
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_Section1_Left, SmartClassroom.EF210_Section1_Left);
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_Section1_Right, SmartClassroom.EF210_Section1_Right);
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_Section2_Left, SmartClassroom.EF210_Section2_Left);
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_Section2_Right, SmartClassroom.EF210_Section2_Right);
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_Section3_Left, SmartClassroom.EF210_Section3_Left);
		sensorLocationMap.put(SmartClassroom.PresenceSensor_EF210_Section3_Right, SmartClassroom.EF210_Section3_Right);
	}
	
	private Resource idResource;
	private Model infoModel;
	
	public SensorInstance(String sensorIdURI, String sensorTypeURI) {
	    // create the model
		infoModel = ModelFactory.createDefaultModel();
		
		Resource sensorTypeResource = ResourceFactory.createResource(sensorTypeURI);
		idResource = infoModel.createResource(sensorIdURI, sensorTypeResource);
    }

	public Resource getIdResource() {
		return idResource;
	}
	
	public Model getInfoModel() {
		return infoModel;
	}
	
	public List<Update> getProfiledLocationUpdates() {
		List<Update> sensorProfiledUpdate = new LinkedList<Update>();
		
		// ASSERTION UUID CREATE
		Node assertionUUIDNode = Node.createURI(ContextModelUtils.createUUID(SmartClassroom.hasProfiledLocation));
		sensorProfiledUpdate.add(new UpdateCreate(assertionUUIDNode));
		
		// ASSERTION CONTENT
		QuadDataAcc assertionContent = new QuadDataAcc();
		assertionContent.addQuad(Quad.create(assertionUUIDNode, idResource.asNode(), 
			SmartClassroom.hasProfiledLocation.asNode(), sensorLocationMap.get(idResource).asNode()));
		sensorProfiledUpdate.add(new UpdateDataInsert(assertionContent));
		
		// ASSERTION ANNOTATIONS
		QuadDataAcc annotationContent = new QuadDataAcc();
		String assertionStoreURI = ContextModelUtils.getAssertionStoreURI(SmartClassroom.hasProfiledLocation.getURI());
		Node assertionStoreNode = Node.createURI(assertionStoreURI);
		annotationContent.addQuad(Quad.create(assertionStoreNode, assertionUUIDNode, 
				ConsertCore.CONTEXT_ASSERTION_RESOURCE.asNode(), SmartClassroom.hasProfiledLocation.asNode()));
		annotationContent.addQuad(Quad.create(assertionStoreNode, assertionUUIDNode, 
				ConsertCore.CONTEXT_ASSERTION_TYPE_PROPERTY.asNode(), ConsertCore.TYPE_PROFILED.asNode()));
		sensorProfiledUpdate.add(new UpdateDataInsert(annotationContent));
		
		return sensorProfiledUpdate;
	}
}
