package example;

import java.util.List;

import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemplateDemo {
	public static void main(String[] args) {
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();

		// Return all vehicles within a window
		String qString = ""
				+ "PREFIX : <http://test#> "
				+ "REGISTER STREAM ?out AS "
				+ "CONSTRUCT ISTREAM { ?car a :Vehicle } "
				+ "FROM NAMED :static1 "
				+ "FROM NAMED WINDOW :w ON :stream1 [RANGE ?range STEP ?step] "
				+ "WHERE { WINDOW ?w { ?car a :Vehicle ; a ?vehicleType . } }";

		// Create a model to store templates and queries
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("sp", SP.NS);
		model.setNsPrefix("spl", SPL.NS);
		model.setNsPrefix("spin", SPIN.NS);
		model.setNsPrefix("rsp", SP.RSP);
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");

		// Add SPIN query
		Query query = QueryFactory.create(qString, Syntax.syntaxARQ);
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query q = arq2SPIN.createQuery(query, "http://query1");
		
		
		
		// Create template
		Template template = TemplateUtils.createTemplate("http://queryTemplate", q);

		Resource arg1 = TemplateUtils.getArgument(template);
		arg1.addProperty(SPL.predicate, model.getProperty(ARG.NS + "range"));
		arg1.addProperty(SPL.valueType, "");
		arg1.addProperty(RDFS.comment, "Range must be a time, e.g. PT10s");

		Resource arg2 = TemplateUtils.getArgument(template);
		arg2.addProperty(SPL.predicate, model.getProperty(ARG.NS + "step"));
		arg2.addProperty(SPL.valueType, XSD.positiveInteger);
		arg2.addProperty(RDFS.comment, "Range must be a time, e.g. PT1s");
		
		Resource arg3 = TemplateUtils.getArgument(template);
		arg3.addProperty(SPL.predicate, model.getProperty(ARG.NS + "out"));
		arg3.addProperty(SPL.valueType, RDFS.Resource);
		arg3.addProperty(RDFS.comment, "Output stream must be valid a resource");

		Resource arg4 = TemplateUtils.getArgument(template);
		arg4.addProperty(SPL.predicate, model.getProperty(ARG.NS + "vehicleType"));
		arg4.addProperty(SPL.valueType, RDFS.Resource);
		arg4.addProperty(SPL.optional, model.createTypedLiteral(true));
		arg4.addProperty(RDFS.comment, "(Optional) Vehicle must be a valid resource");

		model.write(System.out, "TTL");

		
		
		// Get the template from the model
		Template t = model.createResource("http://queryTemplate", SPIN.Template).as(Template.class);
		Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) template.getBody());
		
		// Set bindings with eager literal validation active
		com.hp.hpl.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;
		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("range", model.createLiteral("PT10s"));
		bindings.add("step", model.createLiteral("PT1s"));
		bindings.add("out", model.createResource("http://ns.valcri.org/output/stream1"));
		//bindings.add("vehicleType", model.createResource("http://ns.valcri.org/output/SuspectVechicle"));
		
		// Valdidate bindings against arguments
		List<Argument> arguments = t.getArguments(false);
		if(TemplateUtils.check(arguments, bindings)){
			System.err.println("All tests passed");
		} else {
			System.err.println("Failed one or more tests");
			return;
		}

		// Add prefixes from model
		PrefixMappingImpl pm = new PrefixMappingImpl();
		pm.setNsPrefixes(model.getNsPrefixMap());
		
		// Parameterized, constraint checking in the previous step
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString());
		pss.setNsPrefixes(pm); // This step is good for readability, bad for flexibility
		pss.setParams(bindings);
		System.out.println(pss);
		System.out.println(pss.asQuery());
	}
}
