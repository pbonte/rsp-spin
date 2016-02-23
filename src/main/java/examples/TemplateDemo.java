package examples;

import java.time.Duration;

import org.rspql.lang.rspql.ParserRSPQL;
import org.rsqpql.spin.utils.TemplateUtils;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemplateDemo {
	public static void main(String[] args) {
		// Initialize
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		// Eager literal validation active, required
		com.hp.hpl.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;
				
		TemplateDemo d = new TemplateDemo();
		// Create model
		Model model = ModelFactory.createDefaultModel();
		// Add SPIN template to model
		d.createExampleTemplate(model);
		
		String queryString = d.getTemplate(model);
		Query query = QueryFactory.create(queryString, ParserRSPQL.rspqlSyntax);
		query.setPrefix("rdf", RDF.getURI());
		query.setPrefix("rdfs", RDFS.getURI());
		query.setPrefix("", "http://test#");
		System.out.println(query.toString());
		
	}
	
	public void createExampleTemplate(Model model){
		// Return all vehicles within a window
		String qString = "" + "PREFIX : <http://test#> " + "REGISTER STREAM ?out AS "
				+ "CONSTRUCT ISTREAM { ?car a :VehicleOfInterest } " + "FROM NAMED :static1 "
				+ "FROM NAMED WINDOW :w1 ON :anprStream [RANGE ?range STEP ?step] "
				+ "FROM NAMED WINDOW :w2 ON :suspectStream [RANGE ?range] "
				+ "FROM NAMED WINDOW :w3 ON :suspectStream [FROM NOW-?range TO NOW-?to] "
				+ "WHERE { "
				+ "WINDOW :w1 { ?car a :Vehicle ; a ?vehicleType . }"
				+ "WINDOW :w2 { ?supect :driving ?car . } "
				+ "}";
		
		// First add SPIN query
		Query query = QueryFactory.create(qString, ParserRSPQL.rspqlSyntax);
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query q = arq2SPIN.createQuery(query, "http://org.example/query");
		model.setNsPrefixes(query.getPrefixMapping());

		// Create template based on query
		Template template = TemplateUtils.createTemplate("http:///org.example/template", q);
		
		// Add some argument constraints
		TemplateUtils.createArgument(template, "range", XSD.duration, false, "Range must be a duration (e.g. PT10s)");
		TemplateUtils.createArgument(template, "step", XSD.duration, false, "Step must be a duration (e.g. PT10s)");
		TemplateUtils.createArgument(template, "out", RDFS.Resource, false, "Out must be a resource");
		TemplateUtils.createArgument(template, "vehicleType", RDFS.Resource, true, "Vehicle type must be a valid resource");
	}
	
	/**
	 * Get an instantiated model from template.
	 * 
	 * @param model
	 */
	public String getTemplate(Model model){
		// Get the template from the model using a template resource handle
		Template template = model.createResource("http:///org.example/template", SPIN.Template).as(Template.class);
		Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) template.getBody());

		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("step", template.getModel().createTypedLiteral(Duration.parse("PT1m"), XSDDatatype.XSDduration));
		bindings.add("range", template.getModel().createTypedLiteral(Duration.parse("PT1s"), XSDDatatype.XSDduration));
		bindings.add("out", template.getModel().createResource("http://test#outputStream"));
		bindings.add("vehicleType", template.getModel().createResource("http://test#SuspectVechicle"));

		// Validate template with bindings
		boolean success = TemplateUtils.validate(template, bindings);
		if (!success) {
			System.err.println("Error: Not a valid set of bindings");
			return null;
		}

		// Add prefixes from model
		//PrefixMappingImpl pm = new PrefixMappingImpl();
		//pm.setNsPrefixes(model.getNsPrefixMap());
		//arq.setPrefixMapping(pm);

		// Parameterized, constraint checking in the previous step
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString());
		pss.setParams(bindings);
		String query = TemplateUtils.clean(pss.toString());
		return query;
	}
}
