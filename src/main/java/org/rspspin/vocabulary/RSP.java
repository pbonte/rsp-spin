package org.rspspin.vocabulary;

import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.topbraid.spin.util.SimpleImplementation;
import org.rspspin.model.NamedWindow;
import org.rspspin.model.NamedWindowImpl;

public class RSP {
	public final static String NS = "http://w3id.org/rsp/spin#";
	
	// Classes
	public final static Resource OutputStreamOperator = ResourceFactory.createResource(NS + "OutputStreamOperator");
	public final static Resource Dstream = ResourceFactory.createResource(NS + "Dstream");
	public final static Resource Istream = ResourceFactory.createResource(NS + "Istream");
	public final static Resource Rstream = ResourceFactory.createResource(NS + "Rstream");
	public final static Resource NamedWindow = ResourceFactory.createResource(NS + "NamedWindow");
	public final static Resource LogicalWindow = ResourceFactory.createResource(NS + "LogicalWindow");
	public final static Resource LogicalPastWindow = ResourceFactory.createResource(NS + "LogicalPastWindow");
	public final static Resource PhysicalWindow = ResourceFactory.createResource(NS + "PhysicalWindow");
	
	// Properties
	public final static Property hasOutputStream = ResourceFactory.createProperty(NS + "hasOutputStream");
	public final static Property hasOutputStreamOperator = ResourceFactory.createProperty(NS + "hasOutputStreamOperator");
	public final static Property fromNamedWindow = ResourceFactory.createProperty(NS + "fromNamedWindow");
	public final static Property windowNameNode = ResourceFactory.createProperty(NS + "windowNameNode");
	public final static Property streamUri = ResourceFactory.createProperty(NS + "streamUri");
	public final static Property windowUri = ResourceFactory.createProperty(NS + "windowUri");
	public final static Property logicalRange = ResourceFactory.createProperty(NS + "logicalRange");
	public final static Property logicalStep = ResourceFactory.createProperty(NS + "logicalStep");
	public final static Property from = ResourceFactory.createProperty(NS + "from");
	public final static Property to = ResourceFactory.createProperty(NS + "to");
	public final static Property physicalRange = ResourceFactory.createProperty(NS + "physicalRange");
	public final static Property physicalStep = ResourceFactory.createProperty(NS + "physicalStep");
	
    static {
		RSP.init(BuiltinPersonalities.model);
    }
	
	private static void init(Personality<RDFNode> p){
		p.add(NamedWindow.class, new SimpleImplementation(NamedWindow.asNode(), NamedWindowImpl.class));
	}
}
