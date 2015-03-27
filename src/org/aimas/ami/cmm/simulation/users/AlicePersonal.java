package org.aimas.ami.cmm.simulation.users;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class AlicePersonal {
	
	/** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    public final static String BASE_URI = "http://pervasive.semanticweb.org/ont/2015/01/alicepersonal/core";
	public final static String NS = BASE_URI + "#";
	
	// Vocabulary properties
	////////////////////////
	public final static Property hasPersonCount = m_model.createProperty( NS + "hasPersonCount" );
}
